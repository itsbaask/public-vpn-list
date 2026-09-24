import os
import re
import json
import base64
import hashlib
import logging
import requests
from pathlib import Path
from typing import Dict, Any, List, Set, Optional, Tuple
from urllib.parse import urlparse, urljoin
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

try:
    import cloudscraper
    HAS_CLOUDSCRAPER = True
except ImportError:
    HAS_CLOUDSCRAPER = False

logger = logging.getLogger(__name__)

class SourceUnavailableError(Exception):
    """Raised when no server sources succeeded."""
    pass

class SourceIncompleteError(Exception):
    """Raised when harvested server count is below minimum expected threshold."""
    pass

def is_valid_http_url(url: str) -> bool:
    if not url or not isinstance(url, str):
        return False
    url_str = url.strip()
    if not (url_str.startswith("http://") or url_str.startswith("https://")):
        return False
    try:
        parsed = urlparse(url_str)
        return bool(parsed.netloc)
    except Exception:
        return False

def clean_storage_id(val: str) -> str:
    if not val or not isinstance(val, str):
        return "srv-unknown"
    s = val.strip().lower()
    clean = re.sub(r'[^a-z0-9\-]', '-', s)
    clean = re.sub(r'-+', '-', clean).strip('-')
    return clean or "srv-unknown"

def extract_remote_host_port(ovpn_text: str) -> Tuple[str, int, str]:
    if not ovpn_text or not isinstance(ovpn_text, str):
        return "", 1194, "udp"

    host = ""
    port = 1194
    transport = "udp"

    m_proto = re.search(r'^\s*proto\s+(udp|tcp)', ovpn_text, re.MULTILINE | re.I)
    if m_proto:
        transport = m_proto.group(1).lower()

    m_remote = re.search(r'^\s*remote\s+([^\s]+)(?:\s+(\d+))?', ovpn_text, re.MULTILINE | re.I)
    if m_remote:
        host = m_remote.group(1).strip()
        if m_remote.group(2):
            try:
                port = int(m_remote.group(2))
            except ValueError:
                port = 1194

    return host, port, transport

class ServerInfo:
    def __init__(
        self,
        source_id: str,
        source_name: str = "vpngate",
        profile_id: str = "",
        profile_source_url: str = "",
        server_page_url: str = "",
        country_code: str = "XX",
        country_name: str = "Unknown",
        host: str = "",
        port: int = 1194,
        transport: str = "udp",
        protocol: str = "openvpn",
        speed_mbps: float = 0.0,
        latency_ms: int = 0,
        network_score: int = 0,
        status: str = "verified",
        checked_at: str = "",
        config_sha256: str = "",
        ovpn_content: str = "",
    ):
        self.source_id = str(source_id).strip()
        self.source_name = str(source_name).strip()
        self.profile_id = str(profile_id).strip() if profile_id else ""
        self.profile_source_url = profile_source_url.strip() if profile_source_url else ""
        self.server_page_url = server_page_url.strip() if server_page_url else ""
        self.country_code = (country_code or "XX").upper()
        self.country_name = country_name or "Unknown"
        self.host = host or ""
        self.port = int(port or 1194)
        self.transport = (transport or "udp").lower()
        self.protocol = (protocol or "openvpn").lower()
        self.speed_mbps = float(speed_mbps or 0.0)
        self.latency_ms = int(latency_ms or 0)
        self.network_score = int(network_score or 0)
        self.status = status or "verified"
        self.checked_at = checked_at or ""
        self.config_sha256 = config_sha256 or ""
        self.ovpn_content = ovpn_content or ""

    @property
    def server_id(self) -> str:
        return self.source_id

    @property
    def storage_id(self) -> str:
        if self.profile_id and self.profile_id.isdigit():
            return self.profile_id
        if self.host and self.port:
            host_clean = clean_storage_id(self.host)
            return f"{host_clean}-{self.port}-{self.transport}"
        return clean_storage_id(self.source_id)

    def has_valid_profile_mapping(self) -> bool:
        if self.ovpn_content and len(self.ovpn_content) >= 50:
            return True
        if is_valid_http_url(self.profile_source_url):
            return True
        if self.profile_id and self.profile_id.isdigit():
            return True
        return False

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.source_id,
            "source_id": self.source_id,
            "source_name": self.source_name,
            "profile_id": self.profile_id,
            "profile_source_url": self.profile_source_url,
            "server_page_url": self.server_page_url,
            "country_code": self.country_code,
            "country_name": self.country_name,
            "host": self.host,
            "port": self.port,
            "transport": self.transport,
            "protocol": self.protocol,
            "speed_mbps": self.speed_mbps,
            "latency_ms": self.latency_ms,
            "network_score": self.network_score,
            "status": self.status,
            "checked_at": self.checked_at,
            "config_sha256": self.config_sha256,
            "profile_url": f"/v1/profiles/{self.storage_id}.ovpn" if self.storage_id else None,
            "profile_sha256": self.config_sha256,
        }

class MultiSourceHarvester:
    def __init__(self, session: Optional[requests.Session] = None):
        if session:
            self.session = session
        elif HAS_CLOUDSCRAPER:
            self.session = cloudscraper.create_scraper()
        else:
            self.session = requests.Session()

        retries = Retry(
            total=3,
            connect=3,
            read=3,
            status=3,
            backoff_factor=1.5,
            status_forcelist=[408, 429, 500, 502, 503, 504],
            allowed_methods=frozenset(["GET", "HEAD", "POST"]),
            respect_retry_after_header=True
        )
        adapter = HTTPAdapter(max_retries=retries, pool_connections=15, pool_maxsize=30)
        self.session.mount("https://", adapter)
        self.session.mount("http://", adapter)
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        })

    def harvest_all_sources(self) -> Dict[str, ServerInfo]:
        """
        Harvests and aggregates OpenVPN profiles from all requested public sources:
        1. VPNGate Official API (CSV)
        2. Auto-OVPN GitHub Repo (JSON/Raw)
        3. VPNBook Free OpenVPN API & Credentials (https://www.vpnbook.com)
        4. Riseup VPN (API/Config)
        5. PublicVPNList Native API & Catalog (https://publicvpnlist.com) [LAST]
        """
        harvested: Dict[str, ServerInfo] = {}

        # 1. VPNGate Official CSV API
        try:
            vpngate_servers = self._fetch_vpngate_csv()
            logger.info(f"VPNGate API harvested {len(vpngate_servers)} active servers with decoded OVPN profiles.")
            for sid, s in vpngate_servers.items():
                harvested[sid] = s
        except Exception as e:
            logger.warning(f"VPNGate API harvest error: {e}")

        # 2. Auto-OVPN GitHub Repo
        try:
            auto_servers = self._fetch_auto_ovpn_github()
            logger.info(f"Auto-OVPN GitHub harvested {len(auto_servers)} active servers with decoded OVPN profiles.")
            for sid, s in auto_servers.items():
                if sid not in harvested:
                    harvested[sid] = s
        except Exception as e:
            logger.warning(f"Auto-OVPN GitHub harvest error: {e}")

        # 3. VPNBook Free OpenVPN API with Embedded Credentials
        try:
            vpnbook_servers = self._fetch_vpnbook()
            logger.info(f"VPNBook harvested {len(vpnbook_servers)} active servers with auto-login embedded credentials.")
            for sid, s in vpnbook_servers.items():
                if sid not in harvested:
                    harvested[sid] = s
        except Exception as e:
            logger.warning(f"VPNBook harvest error: {e}")

        # 4. Riseup VPN
        try:
            riseup_servers = self._fetch_riseup_vpn()
            logger.info(f"Riseup VPN harvested {len(riseup_servers)} servers.")
            for sid, s in riseup_servers.items():
                if sid not in harvested:
                    harvested[sid] = s
        except Exception as e:
            logger.warning(f"Riseup VPN harvest error: {e}")

        # 5. PublicVPNList Native API & Catalog (LAST SOURCE)
        try:
            native_servers = self._fetch_publicvpnlist_native()
            logger.info(f"PublicVPNList Native API harvested {len(native_servers)} active servers.")
            for sid, s in native_servers.items():
                if sid not in harvested:
                    harvested[sid] = s
        except Exception as e:
            logger.warning(f"PublicVPNList Native API harvest error: {e}")

        if not harvested:
            raise SourceUnavailableError("Failed to harvest servers from any public source.")

        logger.info(f"Multi-source harvesting completed. Total unique servers harvested: {len(harvested)}")
        return harvested

    def _fetch_publicvpnlist_native(self) -> Dict[str, ServerInfo]:
        url = "https://publicvpnlist.com/local/api/vpn-data.php"
        res = self.session.get(url, headers={"X-Requested-With": "XMLHttpRequest"}, timeout=(10, 30))
        if res.status_code != 200:
            raise SourceUnavailableError(f"PublicVPNList Native API returned HTTP {res.status_code}")

        servers: Dict[str, ServerInfo] = {}
        data = res.json()

        if isinstance(data, list):
            for item in data:
                if not isinstance(item, dict):
                    continue
                if item.get("active") is not True:
                    continue
                if item.get("lastCheckOk") is not True and item.get("checkerStatus") != "tunnel_ok":
                    continue

                sid_num = str(item.get("id", ""))
                if not sid_num:
                    continue

                sid = f"pvl_{sid_num}"
                country_code = str(item.get("country") or "XX").upper()
                country_name = str(item.get("countryName") or "Global")
                host = str(item.get("host") or item.get("ip") or "")
                port = int(item.get("port") or 1194)
                transport = str(item.get("proto") or "udp").lower()
                speed = float(item.get("speed") or item.get("checkerMeasuredThroughputMbps") or 0.0)
                latency = int(item.get("latency") or item.get("checkerMeasuredTunnelRttMs") or 0)

                dl_url = f"https://publicvpnlist.com/download/{sid_num}/"

                servers[sid] = ServerInfo(
                    source_id=sid,
                    source_name="publicvpnlist",
                    profile_id=sid_num,
                    profile_source_url=dl_url,
                    server_page_url=dl_url,
                    country_code=country_code,
                    country_name=country_name,
                    host=host,
                    port=port,
                    transport=transport,
                    protocol="openvpn",
                    speed_mbps=speed,
                    latency_ms=latency,
                    status="verified"
                )

        return servers

    def _fetch_vpnbook(self) -> Dict[str, ServerInfo]:
        url = "https://www.vpnbook.com/freevpn/openvpn"
        res = self.session.get(url, timeout=(10, 30))
        if res.status_code != 200:
            return {}

        soup = BeautifulSoup(res.text, "html.parser")
        page_text = soup.get_text()

        # Extract username
        username = "vpnbook"
        user_match = re.search(r'Username\s*([a-zA-Z0-9]+)', page_text, re.I)
        if user_match:
            raw_u = user_match.group(1).strip()
            if raw_u.lower().startswith("vpnbook"):
                username = "vpnbook"

        # Extract password
        password = ""
        pass_match = re.search(r'Password\s*([a-zA-Z0-9]+)', page_text, re.I)
        if pass_match:
            password = pass_match.group(1).strip()
            password = re.sub(r'Copy.*$', '', password, flags=re.I)

        server_matches = re.findall(r'([A-Za-z]+\s+Server\s+\d+)[^\w]*([a-z0-9\.-]+\.vpnbook\.com)', page_text, re.I)

        ports_protocols = [
            (53, "udp"),
            (25000, "udp"),
            (80, "tcp"),
            (443, "tcp")
        ]

        country_map = {
            'us': ('US', 'United States'),
            'ca': ('CA', 'Canada'),
            'uk': ('GB', 'United Kingdom'),
            'de': ('DE', 'Germany'),
            'fr': ('FR', 'France'),
            'jp': ('JP', 'Japan')
        }

        servers: Dict[str, ServerInfo] = {}
        for label, host in server_matches:
            country_code = "XX"
            country_name = "Global"
            for code, (c_code, c_name) in country_map.items():
                if host.lower().startswith(code):
                    country_code = c_code
                    country_name = c_name
                    break

            for port, proto in ports_protocols:
                sid = f"vpnbook_{host.replace('.', '_')}_{port}_{proto}"

                ovpn_config = f"""client
dev tun
proto {proto}
remote {host} {port}
resolv-retry infinite
nobind
persist-key
persist-tun
remote-cert-tls server
cipher AES-256-GCM
data-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC
verb 3
<auth-user-pass>
{username}
{password}
</auth-user-pass>
"""

                servers[sid] = ServerInfo(
                    source_id=sid,
                    source_name="vpnbook",
                    country_code=country_code,
                    country_name=country_name,
                    host=host,
                    port=port,
                    transport=proto,
                    protocol="openvpn",
                    ovpn_content=ovpn_config
                )

        return servers

    def _fetch_vpngate_csv(self) -> Dict[str, ServerInfo]:
        url = "https://www.vpngate.net/api/iphone/"
        res = self.session.get(url, timeout=(10, 30))
        if res.status_code != 200:
            raise SourceUnavailableError(f"VPNGate API returned HTTP {res.status_code}")

        servers: Dict[str, ServerInfo] = {}
        lines = res.text.splitlines()

        for line in lines:
            line_str = line.strip()
            if not line_str or line_str.startswith("*") or line_str.startswith("#"):
                continue

            parts = line_str.split(",")
            if len(parts) >= 15:
                hostname = parts[0].strip()
                ip = parts[1].strip()
                score = parts[2].strip()
                ping = parts[3].strip()
                speed = parts[4].strip()
                country_long = parts[5].strip()
                country_short = parts[6].strip()
                ovpn_b64 = parts[14].strip()

                if ovpn_b64 and len(ovpn_b64) > 100:
                    try:
                        ovpn_text = base64.b64decode(ovpn_b64).decode("utf-8", "ignore")
                        host, port, transport = extract_remote_host_port(ovpn_text)

                        if not host:
                            host = ip

                        sid = f"vpngate_{ip}_{port}_{transport}"
                        speed_val = round(float(speed) / 1000000.0, 2) if speed.isdigit() else 0.0
                        latency_val = int(ping) if ping.isdigit() else 0
                        score_val = int(score) if score.isdigit() else 0

                        servers[sid] = ServerInfo(
                            source_id=sid,
                            source_name="vpngate",
                            country_code=country_short or "JP",
                            country_name=country_long or "Japan",
                            host=host,
                            port=port,
                            transport=transport,
                            protocol="openvpn",
                            speed_mbps=speed_val,
                            latency_ms=latency_val,
                            network_score=score_val,
                            ovpn_content=ovpn_text
                        )
                    except Exception as b64_err:
                        logger.debug(f"VPNGate Base64 decode error: {b64_err}")

        return servers

    def _fetch_auto_ovpn_github(self) -> Dict[str, ServerInfo]:
        url = "https://raw.githubusercontent.com/9xN/auto-ovpn/main/json/data.json"
        res = self.session.get(url, timeout=(10, 30))
        if res.status_code != 200:
            return {}

        servers: Dict[str, ServerInfo] = {}
        data = res.json()

        items_to_process = []
        if isinstance(data, list):
            for item in data:
                if isinstance(item, dict) and "servers" in item and isinstance(item["servers"], list):
                    items_to_process.extend(item["servers"])
                else:
                    items_to_process.append(item)

        for idx, item in enumerate(items_to_process):
            ovpn_b64 = ""
            c_long = "Global"
            c_short = "XX"
            if isinstance(item, str):
                ovpn_b64 = item
            elif isinstance(item, dict):
                ovpn_b64 = item.get("openvpn_configdata_base64") or item.get("config") or ""
                c_long = item.get("countrylong") or "Global"
                c_short = (item.get("countryshort") or "XX").upper()

            if ovpn_b64:
                try:
                    ovpn_text = base64.b64decode(ovpn_b64).decode("utf-8", "ignore")
                    host, port, transport = extract_remote_host_port(ovpn_text)
                    if not host and isinstance(item, dict):
                        host = item.get("ip", "")
                    if host:
                        sid = f"autoovpn_{host}_{port}_{transport}"
                        servers[sid] = ServerInfo(
                            source_id=sid,
                            source_name="auto_ovpn",
                            country_code=c_short,
                            country_name=c_long,
                            host=host,
                            port=port,
                            transport=transport,
                            protocol="openvpn",
                            ovpn_content=ovpn_text
                        )
                except Exception as err:
                    logger.debug(f"Auto-OVPN decode error idx {idx}: {err}")

        return servers

    def _fetch_ipspeed_info(self) -> Dict[str, ServerInfo]:
        urls = ["https://ipspeed.info/free-openvpn.php", "https://ipspeed.info/"]
        servers: Dict[str, ServerInfo] = {}

        for url in urls:
            try:
                res = self.session.get(url, timeout=(10, 25))
                if res.status_code == 200:
                    soup = BeautifulSoup(res.text, "html.parser")
                    download_re = re.compile(r'/download/(\d+)/?|\.ovpn')
                    for anchor in soup.select("a[href]"):
                        href = anchor.get("href", "")
                        match = download_re.search(href)
                        if match:
                            sid_num = match.group(1) if match.group(1) else hashlib.md5(href.encode("utf-8")).hexdigest()[:12]
                            sid = f"ipspeed_{sid_num}"
                            full_dl_url = urljoin(url, href)
                            servers[sid] = ServerInfo(
                                source_id=sid,
                                source_name="ipspeed_info",
                                profile_id=sid_num if sid_num.isdigit() else "",
                                profile_source_url=full_dl_url,
                                country_name="Global"
                            )
            except Exception as err:
                logger.debug(f"IPSpeed Info fetch error {url}: {err}")

        return servers

    def _fetch_omg_vpn(self) -> Dict[str, ServerInfo]:
        url = "https://omgvpn.com/"
        servers: Dict[str, ServerInfo] = {}
        try:
            res = self.session.get(url, timeout=(10, 25))
            if res.status_code == 200:
                soup = BeautifulSoup(res.text, "html.parser")
                for anchor in soup.select("a[href]"):
                    href = anchor.get("href", "")
                    if ".ovpn" in href or "/download" in href:
                        full_url = urljoin(url, href)
                        sid = f"omg_{hashlib.md5(full_url.encode('utf-8')).hexdigest()[:12]}"
                        servers[sid] = ServerInfo(
                            source_id=sid,
                            source_name="omgvpn",
                            profile_source_url=full_url,
                            country_name="Global"
                        )
        except Exception as err:
            logger.debug(f"OMG VPN fetch error: {err}")

        return servers

    def _fetch_riseup_vpn(self) -> Dict[str, ServerInfo]:
        url = "https://black.riseup.net/3/config/eip-service.json"
        servers: Dict[str, ServerInfo] = {}
        try:
            res = self.session.get(url, timeout=(10, 25))
            if res.status_code == 200:
                data = res.json()
                gateways = data.get("gateways", [])
                for gw in gateways:
                    ip = gw.get("ip_address")
                    host = gw.get("host", ip)
                    location = gw.get("location", "Global")
                    capabilities = gw.get("capabilities", {})
                    ports = capabilities.get("transport", [{}])[0].get("ports", [1194])
                    port = ports[0] if ports else 1194
                    proto = capabilities.get("transport", [{}])[0].get("protocols", ["udp"])[0]

                    if ip:
                        sid = f"riseup_{ip}_{port}_{proto}"
                        servers[sid] = ServerInfo(
                            source_id=sid,
                            source_name="riseup_vpn",
                            country_name=location,
                            host=ip,
                            port=port,
                            transport=proto,
                            protocol="openvpn"
                        )
        except Exception as err:
            logger.debug(f"Riseup VPN fetch error: {err}")

        return servers
