import hashlib
import re
import logging
import requests
from typing import Optional, Tuple, Any, Dict
from urllib.parse import urlparse, urlsplit, urlunsplit, urljoin
from bs4 import BeautifulSoup

try:
    import cloudscraper
    HAS_CLOUDSCRAPER = True
except ImportError:
    HAS_CLOUDSCRAPER = False

logger = logging.getLogger(__name__)

class ProfileDownloadError(Exception):
    """Raised when profile download target resolution fails."""
    pass

class DownloadResult:
    def __init__(
        self,
        source_id: str,
        target_type: str,
        target_url: str,
        http_status: Optional[int] = None,
        content_type: Optional[str] = None,
        bytes_downloaded: int = 0,
        sha256: Optional[str] = None,
        content: Optional[str] = None,
        error: Optional[str] = None,
    ):
        self.source_id = source_id
        self.target_type = target_type
        self.target_url = target_url
        self.http_status = http_status
        self.content_type = content_type
        self.bytes_downloaded = bytes_downloaded
        self.sha256 = sha256
        self.content = content
        self.error = error

    @property
    def is_success(self) -> bool:
        return bool(self.content and self.sha256 and not self.error)

def safe_url(url: str) -> str:
    """Sanitizes URL for logging by removing query parameters and sensitive tokens."""
    if not url or not isinstance(url, str):
        return ""
    try:
        parts = urlsplit(url.strip())
        return urlunsplit((parts.scheme, parts.netloc, parts.path, "", ""))
    except Exception:
        return url[:60]

def minify_ovpn(content: str) -> str:
    """Removes comments and extra blank lines from OVPN configuration"""
    lines = []
    for line in content.splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("#", ";")):
            lines.append(stripped)
    return "\n".join(lines)

def compute_sha256(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()

def extract_numeric_profile_id(val: str) -> str:
    if not val or not isinstance(val, str):
        return ""
    val_str = val.strip()
    if val_str.isdigit():
        return val_str
    m = re.search(r'/download/(\d+)/?', val_str) or re.search(r'/server/(\d+)/?', val_str) or re.search(r'id=(\d+)', val_str)
    if m:
        return m.group(1)
    return ""

def is_valid_ovpn_content(text: str) -> bool:
    if not text or len(text) < 50:
        return False
    lower = text.lower()
    if "<html" in lower and "</html" in lower:
        return False
    if "application/json" in lower and "error" in lower and "client" not in lower:
        return False
    has_directive = "client" in lower or "dev tun" in lower or "dev tap" in lower or "remote " in lower
    has_crypto = "<ca>" in lower or "ca " in lower or "<secret>" in lower or "<key>" in lower or "secret " in lower or "crypto" in lower or "<cert>" in lower
    return has_directive and has_crypto

import threading

class OvpnDownloader:
    _token_lock = threading.Lock()
    _last_pvl_request_time = 0.0

    def __init__(
        self,
        base_url: str = "https://publicvpnlist.com",
        access_key: Optional[str] = None,
        session: Optional[requests.Session] = None
    ):
        self.base_url = base_url.rstrip("/")
        self.access_key = access_key
        if session:
            self.session = session
        else:
            self.session = requests.Session()

    def _get_auth_headers(self) -> Dict[str, str]:
        headers = {
            "Accept": "application/x-openvpn-profile, text/plain, application/json, text/html, */*",
            "User-Agent": "PublicVPNList-Cloud-Sync/1.0",
            "Referer": f"{self.base_url}/"
        }
        if self.access_key:
            headers["Authorization"] = f"Bearer {self.access_key}"
            headers["X-Access-Key"] = self.access_key
        return headers

    def resolve_download_target(self, target: Any) -> Tuple[str, str]:
        source_id = getattr(target, "source_id", getattr(target, "server_id", str(target)))
        profile_url = getattr(target, "profile_source_url", "")
        page_url = getattr(target, "server_page_url", "")

        if profile_url and profile_url.startswith("http") and "publicvpnlist.com/download/" not in profile_url:
            return "profile_source_url", profile_url

        if page_url and page_url.startswith("http") and "/api/v1/" not in page_url and "publicvpnlist.com/download/" not in page_url:
            return "server_page_url", page_url

        raise ProfileDownloadError(f"No direct profile download URL for source_id={source_id}")

    def download_profile(self, target: Any) -> DownloadResult:
        source_id = getattr(target, "source_id", getattr(target, "server_id", str(target)))

        # Priority 0: Inline Decoded OVPN Profile (from VPNGate CSV / Auto-OVPN / Seed Database)
        ovpn_inline = getattr(target, "ovpn_content", "")
        if ovpn_inline and len(ovpn_inline) >= 50 and is_valid_ovpn_content(ovpn_inline):
            minified = minify_ovpn(ovpn_inline)
            sha256_val = compute_sha256(minified)
            return DownloadResult(
                source_id=source_id,
                target_type="inline_decoded_profile",
                target_url="inline",
                http_status=200,
                content_type="application/x-openvpn-profile",
                bytes_downloaded=len(minified.encode("utf-8")),
                sha256=sha256_val,
                content=minified
            )

        # Priority 0.5: Native PublicVPNList Token Flow / Direct Download Scraping
        profile_id = getattr(target, "profile_id", "") or extract_numeric_profile_id(getattr(target, "profile_source_url", "")) or extract_numeric_profile_id(source_id)
        source_name = str(getattr(target, "source_name", ""))
        target_url_check = str(getattr(target, "profile_source_url", ""))

        if profile_id and profile_id.isdigit() and ("publicvpnlist" in source_name or "publicvpnlist.com" in target_url_check):
            token_url = f"{self.base_url}/get_token.php"
            page_url = f"{self.base_url}/download/{profile_id}/"
            token_headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
                "X-Requested-With": "XMLHttpRequest",
                "Accept": "application/json, text/html, */*",
                "Referer": page_url
            }

            for attempt in range(1, 8):
                try:
                    import time
                    with OvpnDownloader._token_lock:
                        now = time.time()
                        diff = now - OvpnDownloader._last_pvl_request_time
                        if diff < 1.5:
                            time.sleep(1.5 - diff)
                        OvpnDownloader._last_pvl_request_time = time.time()

                    dl_url = None
                    res_tok = None
                    try:
                        res_tok = self.session.post(token_url, data={"id": profile_id}, headers=token_headers, timeout=(10, 30))
                        if res_tok.status_code == 200:
                            tok_json = res_tok.json()
                            raw_url = tok_json.get("url") or f"/download.php?token={tok_json.get('token')}"
                            dl_url = urljoin(self.base_url, raw_url)
                    except Exception:
                        pass

                    if not dl_url:
                        res_page = self.session.get(page_url, headers=token_headers, timeout=(10, 30))
                        if res_page.status_code == 200:
                            soup = BeautifulSoup(res_page.text, "html.parser")
                            link_node = soup.find('a', href=re.compile(r'\.ovpn|/get/|/file/|/download/', re.I))
                            if link_node and link_node.get('href'):
                                dl_url = urljoin(self.base_url, link_node['href'])

                    if dl_url:
                        res_ovpn = self.session.get(dl_url, headers=token_headers, timeout=(10, 30))
                        if res_ovpn.status_code == 200 and is_valid_ovpn_content(res_ovpn.text):
                            minified = minify_ovpn(res_ovpn.text)
                            sha256_val = compute_sha256(minified)
                            return DownloadResult(
                                source_id=source_id,
                                target_type="pvl_token_download",
                                target_url=dl_url,
                                http_status=200,
                                content_type="application/x-openvpn-profile",
                                bytes_downloaded=len(minified.encode("utf-8")),
                                sha256=sha256_val,
                                content=minified
                            )

                    if res_tok and res_tok.status_code == 429:
                        logger.warning(f"PublicVPNList 429 Rate Limit for {source_id}. Pausing 15s for Cloudflare reset...")
                        import time
                        time.sleep(15.0)
                    else:
                        status_code = res_tok.status_code if res_tok else "scrape"
                        logger.warning(f"PublicVPNList token/download status {status_code} for {source_id}")
                except Exception as e:
                    logger.warning(f"PublicVPNList token download attempt {attempt} error for {source_id}: {e}")
                    import time
                    time.sleep(1.5 * attempt)

            return DownloadResult(
                source_id=source_id,
                target_type="pvl_token_download",
                target_url=f"{self.base_url}/download/{profile_id}/",
                error="PublicVPNList token download failed after retries"
            )

        try:
            target_type, target_url = self.resolve_download_target(target)
        except ProfileDownloadError as e:
            logger.debug(f"Server {source_id}: {e}")
            return DownloadResult(
                source_id=source_id,
                target_type="unresolved",
                target_url="",
                error=str(e)
            )

        headers = self._get_auth_headers()
        target_host = urlsplit(target_url).netloc if target_url else "unknown"

        logger.info(
            f"Downloading profile: source_id={source_id} target_type={target_type} "
            f"safe_url={safe_url(target_url)} target_host={target_host} "
            f"has_auth_header={bool(self.access_key)} timeout=(15, 90)"
        )

        # Priority 1: Direct profile_source_url
        if target_type == "profile_source_url":
            try:
                res = self.session.get(target_url, headers=headers, timeout=(15, 90), allow_redirects=True)
                status = res.status_code if res is not None else 0
                content_type = res.headers.get("Content-Type", "") if res is not None else ""
                bytes_count = len(res.content) if res is not None and res.content else 0

                logger.info(
                    f"source_id={source_id} target_type={target_type} status_code={status} "
                    f"content_type={content_type} content_length={bytes_count} "
                    f"final_safe_url={safe_url(res.url if res else '')} redirect_count={len(res.history) if res else 0}"
                )

                if res is not None and res.status_code == 200:
                    text = res.text
                    if is_valid_ovpn_content(text):
                        minified = minify_ovpn(text)
                        sha256_val = compute_sha256(minified)
                        return DownloadResult(
                            source_id=source_id,
                            target_type=target_type,
                            target_url=safe_url(target_url),
                            http_status=200,
                            content_type=content_type,
                            bytes_downloaded=len(minified.encode("utf-8")),
                            sha256=sha256_val,
                            content=minified
                        )

                    # Handle JSON response
                    if "application/json" in content_type.lower():
                        try:
                            json_data = res.json()
                            if isinstance(json_data, dict):
                                ovpn_text = (
                                    json_data.get("config") or
                                    json_data.get("profile") or
                                    json_data.get("ovpn") or
                                    (json_data.get("data", {}).get("config") if isinstance(json_data.get("data"), dict) else None)
                                )
                                if ovpn_text and is_valid_ovpn_content(str(ovpn_text)):
                                    minified = minify_ovpn(str(ovpn_text))
                                    sha256_val = compute_sha256(minified)
                                    return DownloadResult(
                                        source_id=source_id,
                                        target_type=target_type,
                                        target_url=safe_url(target_url),
                                        http_status=200,
                                        content_type=content_type,
                                        bytes_downloaded=len(minified.encode("utf-8")),
                                        sha256=sha256_val,
                                        content=minified
                                    )

                                nested_dl_url = (
                                    json_data.get("config_download_url") or
                                    json_data.get("download_url") or
                                    json_data.get("url") or
                                    (json_data.get("data", {}).get("config_download_url") if isinstance(json_data.get("data"), dict) else None)
                                )
                                if nested_dl_url and str(nested_dl_url).startswith("http") and str(nested_dl_url) != target_url:
                                    res_nested = self.session.get(str(nested_dl_url), headers=headers, timeout=(15, 90), allow_redirects=True)
                                    if res_nested and res_nested.status_code == 200 and is_valid_ovpn_content(res_nested.text):
                                        minified = minify_ovpn(res_nested.text)
                                        sha256_val = compute_sha256(minified)
                                        return DownloadResult(
                                            source_id=source_id,
                                            target_type=target_type,
                                            target_url=safe_url(str(nested_dl_url)),
                                            http_status=200,
                                            content_type=res_nested.headers.get("Content-Type", ""),
                                            bytes_downloaded=len(minified.encode("utf-8")),
                                            sha256=sha256_val,
                                            content=minified
                                        )
                        except Exception as json_err:
                            logger.warning(f"Server {source_id}: JSON parse error: {json_err}")

                    return DownloadResult(
                        source_id=source_id,
                        target_type=target_type,
                        target_url=safe_url(target_url),
                        http_status=200,
                        content_type=content_type,
                        bytes_downloaded=bytes_count,
                        error="Invalid OVPN content in response"
                    )
                else:
                    return DownloadResult(
                        source_id=source_id,
                        target_type=target_type,
                        target_url=safe_url(target_url),
                        http_status=status,
                        content_type=content_type,
                        bytes_downloaded=bytes_count,
                        error=f"HTTP {status}"
                    )

            except requests.exceptions.Timeout as exc:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} safe_url={safe_url(target_url)} "
                    f"error_type=Timeout error_message={type(exc).__name__}: {exc}"
                )
                return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"Timeout: {exc}")

            except requests.exceptions.SSLError as exc:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} safe_url={safe_url(target_url)} "
                    f"error_type=SSLError error_message={type(exc).__name__}: {exc}"
                )
                return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"SSLError: {exc}")

            except requests.exceptions.ConnectionError as exc:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} safe_url={safe_url(target_url)} "
                    f"error_type=ConnectionError error_message={type(exc).__name__}: {exc}"
                )
                return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"ConnectionError: {exc}")

            except requests.exceptions.TooManyRedirects as exc:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} safe_url={safe_url(target_url)} "
                    f"error_type=TooManyRedirects error_message={type(exc).__name__}: {exc}"
                )
                return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"TooManyRedirects: {exc}")

            except requests.exceptions.RequestException as exc:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} safe_url={safe_url(target_url)} "
                    f"error_type=RequestException error_message={type(exc).__name__}: {exc}"
                )
                return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"RequestException: {exc}")

        # Priority 2: HTML Page or Direct Download Target URL
        try:
            page_headers = dict(headers)
            page_headers["Referer"] = self.base_url
            res1 = self.session.get(target_url, headers=page_headers, timeout=(15, 90), allow_redirects=True)
            status1 = res1.status_code if res1 is not None else 0
            ct1 = res1.headers.get("Content-Type", "") if res1 is not None else ""

            if not res1 or res1.status_code != 200:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} status_code={status1} "
                    f"content_type={ct1} error=HTTP {status1}"
                )
                return DownloadResult(
                    source_id=source_id,
                    target_type=target_type,
                    target_url=safe_url(target_url),
                    http_status=status1,
                    content_type=ct1,
                    error=f"Download page HTTP {status1}"
                )

            # Check if res1 is direct OVPN file
            if is_valid_ovpn_content(res1.text):
                minified = minify_ovpn(res1.text)
                sha256_val = compute_sha256(minified)
                return DownloadResult(
                    source_id=source_id,
                    target_type=target_type,
                    target_url=safe_url(target_url),
                    http_status=200,
                    content_type=ct1,
                    bytes_downloaded=len(minified.encode("utf-8")),
                    sha256=sha256_val,
                    content=minified
                )

            # Step A: Extract href with BeautifulSoup if res1 is HTML
            dl_url = None
            soup = BeautifulSoup(res1.text, "html.parser")
            link_node = soup.find('a', href=re.compile(r'\.ovpn|/get/|/file/|download\.php', re.I))
            if link_node and link_node.get('href'):
                dl_url = urljoin(self.base_url, link_node['href'])

            if not dl_url:
                logger.warning(
                    f"source_id={source_id} target_type={target_type} status_code={status1} "
                    f"content_type={ct1} error=Could not obtain download URL"
                )
                return DownloadResult(
                    source_id=source_id,
                    target_type=target_type,
                    target_url=safe_url(target_url),
                    http_status=status1,
                    content_type=ct1,
                    error="Could not obtain download URL"
                )

            # Download actual OVPN file
            dl_res = self.session.get(dl_url, headers={"Referer": target_url}, timeout=(15, 90), allow_redirects=True)
            status2 = dl_res.status_code if dl_res is not None else 0
            ct2 = dl_res.headers.get("Content-Type", "") if dl_res is not None else ""
            bytes2 = len(dl_res.content) if dl_res is not None and dl_res.content else 0

            logger.info(
                f"source_id={source_id} target_type={target_type} status_code={status2} "
                f"content_type={ct2} content_length={bytes2} final_safe_url={safe_url(dl_res.url if dl_res else '')} "
                f"redirect_count={len(dl_res.history) if dl_res else 0}"
            )

            if not dl_res or dl_res.status_code != 200:
                return DownloadResult(
                    source_id=source_id,
                    target_type=target_type,
                    target_url=safe_url(dl_url),
                    http_status=status2,
                    content_type=ct2,
                    bytes_downloaded=bytes2,
                    error=f"Profile file HTTP {status2}"
                )

            text = dl_res.text
            if not is_valid_ovpn_content(text):
                return DownloadResult(
                    source_id=source_id,
                    target_type=target_type,
                    target_url=safe_url(dl_url),
                    http_status=status2,
                    content_type=ct2,
                    bytes_downloaded=bytes2,
                    error="Invalid OVPN content"
                )

            minified = minify_ovpn(text)
            sha256_val = compute_sha256(minified)
            return DownloadResult(
                source_id=source_id,
                target_type=target_type,
                target_url=safe_url(dl_url),
                http_status=200,
                content_type=ct2,
                bytes_downloaded=len(minified.encode("utf-8")),
                sha256=sha256_val,
                content=minified
            )

        except requests.exceptions.Timeout as exc:
            logger.warning(f"source_id={source_id} target_type={target_type} error_type=Timeout detail={exc}")
            return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"Timeout: {exc}")

        except requests.exceptions.SSLError as exc:
            logger.warning(f"source_id={source_id} target_type={target_type} error_type=SSLError detail={exc}")
            return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"SSLError: {exc}")

        except requests.exceptions.ConnectionError as exc:
            logger.warning(f"source_id={source_id} target_type={target_type} error_type=ConnectionError detail={exc}")
            return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"ConnectionError: {exc}")

        except requests.exceptions.RequestException as exc:
            logger.warning(f"source_id={source_id} target_type={target_type} error_type=RequestException detail={exc}")
            return DownloadResult(source_id=source_id, target_type=target_type, target_url=safe_url(target_url), error=f"RequestException: {exc}")
