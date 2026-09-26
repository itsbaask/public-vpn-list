import os
import re
import json
import base64
import hashlib
import logging
import requests
from pathlib import Path
from typing import Dict, Any, List, Set, Optional, Tuple
from urllib.parse import urlparse, urljoin, urlsplit, urlunsplit
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

# ============================================================================
# Enterprise Global Country Normalization Engine (195 ISO Countries)
# ============================================================================

class CountryNormalizer:
    # Official ISO 3166-1 alpha-2 map: Code -> (Name, Flag)
    ISO_COUNTRIES: Dict[str, Tuple[str, str]] = {
        "AF": ("Afghanistan", "🇦🇫"),
        "AL": ("Albania", "🇦🇱"),
        "DZ": ("Algeria", "🇩🇿"),
        "AD": ("Andorra", "🇦🇩"),
        "AO": ("Angola", "🇦🇴"),
        "AG": ("Antigua and Barbuda", "🇦🇬"),
        "AR": ("Argentina", "🇦🇷"),
        "AM": ("Armenia", "🇦🇲"),
        "AU": ("Australia", "🇦🇺"),
        "AT": ("Austria", "🇦🇹"),
        "AZ": ("Azerbaijan", "🇦🇿"),
        "BS": ("Bahamas", "🇧🇸"),
        "BH": ("Bahrain", "🇧🇭"),
        "BD": ("Bangladesh", "🇧🇩"),
        "BB": ("Barbados", "🇧🇧"),
        "BY": ("Belarus", "🇧🇾"),
        "BE": ("Belgium", "🇧🇪"),
        "BZ": ("Belize", "🇧🇿"),
        "BJ": ("Benin", "🇧🇯"),
        "BT": ("Bhutan", "🇧🇹"),
        "BO": ("Bolivia", "🇧🇴"),
        "BA": ("Bosnia and Herzegovina", "🇧🇦"),
        "BW": ("Botswana", "🇧🇼"),
        "BR": ("Brazil", "🇧🇷"),
        "BN": ("Brunei", "🇧🇳"),
        "BG": ("Bulgaria", "🇧🇬"),
        "BF": ("Burkina Faso", "🇧🇫"),
        "BI": ("Burundi", "🇧🇮"),
        "KH": ("Cambodia", "🇰🇭"),
        "CM": ("Cameroon", "🇨🇲"),
        "CA": ("Canada", "🇨🇦"),
        "CV": ("Cape Verde", "🇨🇻"),
        "CF": ("Central African Republic", "🇨🇫"),
        "TD": ("Chad", "🇹🇩"),
        "CL": ("Chile", "🇨🇱"),
        "CN": ("China", "🇨🇳"),
        "CO": ("Colombia", "🇨🇴"),
        "KM": ("Comoros", "🇰🇲"),
        "CG": ("Congo", "🇨🇬"),
        "CD": ("Congo (DRC)", "🇨🇩"),
        "CR": ("Costa Rica", "🇨🇷"),
        "HR": ("Croatia", "🇭🇷"),
        "CU": ("Cuba", "🇨🇺"),
        "CY": ("Cyprus", "🇨🇾"),
        "CZ": ("Czech Republic", "🇨🇿"),
        "DK": ("Denmark", "🇩🇰"),
        "DJ": ("Djibouti", "🇩🇯"),
        "DM": ("Dominica", "🇩🇲"),
        "DO": ("Dominican Republic", "🇩🇴"),
        "EC": ("Ecuador", "🇪🇨"),
        "EG": ("Egypt", "🇪🇬"),
        "SV": ("El Salvador", "🇸🇻"),
        "GQ": ("Equatorial Guinea", "🇬🇶"),
        "ER": ("Eritrea", "🇪🇷"),
        "EE": ("Estonia", "🇪🇪"),
        "SZ": ("Eswatini", "🇸🇿"),
        "ET": ("Ethiopia", "🇪🇹"),
        "FJ": ("Fiji", "🇫🇯"),
        "FI": ("Finland", "🇫🇮"),
        "FR": ("France", "🇫🇷"),
        "GA": ("Gabon", "🇬🇦"),
        "GM": ("Gambia", "🇬🇲"),
        "GE": ("Georgia", "🇬🇪"),
        "DE": ("Germany", "🇩🇪"),
        "GH": ("Ghana", "🇬🇭"),
        "GR": ("Greece", "🇬🇷"),
        "GD": ("Grenada", "🇬🇩"),
        "GT": ("Guatemala", "🇬🇹"),
        "GN": ("Guinea", "🇬🇳"),
        "GW": ("Guinea-Bissau", "🇬🇼"),
        "GY": ("Guyana", "🇬🇾"),
        "HT": ("Haiti", "🇭🇹"),
        "HN": ("Honduras", "🇭🇳"),
        "HK": ("Hong Kong", "🇭🇰"),
        "HU": ("Hungary", "🇭🇺"),
        "IS": ("Iceland", "🇮🇸"),
        "IN": ("India", "🇮🇳"),
        "ID": ("Indonesia", "🇮🇩"),
        "IR": ("Iran", "🇮🇷"),
        "IQ": ("Iraq", "🇮🇶"),
        "IE": ("Ireland", "🇮🇪"),
        "IL": ("Israel", "🇮🇱"),
        "IT": ("Italy", "🇮🇹"),
        "JM": ("Jamaica", "🇯🇲"),
        "JP": ("Japan", "🇯🇵"),
        "JO": ("Jordan", "🇯🇴"),
        "KZ": ("Kazakhstan", "🇰🇿"),
        "KE": ("Kenya", "🇰🇪"),
        "KI": ("Kiribati", "🇰🇮"),
        "KP": ("North Korea", "🇰🇵"),
        "KR": ("South Korea", "🇰🇷"),
        "KW": ("Kuwait", "🇰🇼"),
        "KG": ("Kyrgyzstan", "🇰🇬"),
        "LA": ("Laos", "🇱🇦"),
        "LV": ("Latvia", "🇱🇻"),
        "LB": ("Lebanon", "🇱🇧"),
        "LS": ("Lesotho", "🇱🇸"),
        "LR": ("Liberia", "🇱🇷"),
        "LY": ("Libya", "🇱🇾"),
        "LI": ("Liechtenstein", "🇱🇮"),
        "LT": ("Lithuania", "🇱🇹"),
        "LU": ("Luxembourg", "🇱🇺"),
        "MO": ("Macau", "🇲🇴"),
        "MG": ("Madagascar", "🇲🇬"),
        "MW": ("Malawi", "🇲🇼"),
        "MY": ("Malaysia", "🇲🇾"),
        "MV": ("Maldives", "🇲🇻"),
        "ML": ("Mali", "🇲🇱"),
        "MT": ("Malta", "🇲🇹"),
        "MH": ("Marshall Islands", "🇲🇭"),
        "MR": ("Mauritania", "🇲🇷"),
        "MU": ("Mauritius", "🇲🇺"),
        "MX": ("Mexico", "🇲🇽"),
        "FM": ("Micronesia", "🇫🇲"),
        "MD": ("Moldova", "🇲🇩"),
        "MC": ("Monaco", "🇲🇨"),
        "MN": ("Mongolia", "🇲🇳"),
        "ME": ("Montenegro", "🇲🇪"),
        "MA": ("Morocco", "🇲🇦"),
        "MZ": ("Mozambique", "🇲🇿"),
        "MM": ("Myanmar", "🇲🇲"),
        "NA": ("Namibia", "🇳🇦"),
        "NR": ("Nauru", "🇳🇷"),
        "NP": ("Nepal", "🇳🇵"),
        "NL": ("Netherlands", "🇳🇱"),
        "NZ": ("New Zealand", "🇳🇿"),
        "NI": ("Nicaragua", "🇳🇮"),
        "NE": ("Niger", "🇳🇪"),
        "NG": ("Nigeria", "🇳🇬"),
        "MK": ("North Macedonia", "🇲🇰"),
        "NO": ("Norway", "🇳🇴"),
        "OM": ("Oman", "🇴🇲"),
        "PK": ("Pakistan", "🇵🇰"),
        "PW": ("Palau", "🇵🇼"),
        "PS": ("Palestine", "🇵🇸"),
        "PA": ("Panama", "🇵🇦"),
        "PG": ("Papua New Guinea", "🇵🇬"),
        "PY": ("Paraguay", "🇵🇾"),
        "PE": ("Peru", "🇵🇪"),
        "PH": ("Philippines", "🇵🇭"),
        "PL": ("Poland", "🇵🇱"),
        "PT": ("Portugal", "🇵🇹"),
        "QA": ("Qatar", "🇶🇦"),
        "RO": ("Romania", "🇷🇴"),
        "RU": ("Russia", "🇷🇺"),
        "RW": ("Rwanda", "🇷🇼"),
        "KN": ("Saint Kitts and Nevis", "🇰🇳"),
        "LC": ("Saint Lucia", "🇱🇨"),
        "VC": ("Saint Vincent and the Grenadines", "🇻🇨"),
        "WS": ("Samoa", "🇼🇸"),
        "SM": ("San Marino", "🇸🇲"),
        "ST": ("Sao Tome and Principe", "🇸🇹"),
        "SA": ("Saudi Arabia", "🇸🇦"),
        "SN": ("Senegal", "🇸🇳"),
        "RS": ("Serbia", "🇷🇸"),
        "SC": ("Seychelles", "🇸🇨"),
        "SL": ("Sierra Leone", "🇸🇱"),
        "SG": ("Singapore", "🇸🇬"),
        "SK": ("Slovakia", "🇸🇰"),
        "SI": ("Slovenia", "🇸🇮"),
        "SB": ("Solomon Islands", "🇸🇧"),
        "SO": ("Somalia", "🇸🇴"),
        "ZA": ("South Africa", "🇿🇦"),
        "SS": ("South Sudan", "🇸🇸"),
        "ES": ("Spain", "🇪🇸"),
        "LK": ("Sri Lanka", "🇱🇰"),
        "SD": ("Sudan", "🇸🇩"),
        "SR": ("Suriname", "🇸🇷"),
        "SE": ("Sweden", "🇸🇪"),
        "CH": ("Switzerland", "🇨🇭"),
        "SY": ("Syria", "🇸🇾"),
        "TW": ("Taiwan", "🇹🇼"),
        "TJ": ("Tajikistan", "🇹🇯"),
        "TZ": ("Tanzania", "🇹🇿"),
        "TH": ("Thailand", "🇹🇭"),
        "TL": ("Timor-Leste", "🇹🇱"),
        "TG": ("Togo", "🇹🇬"),
        "TO": ("Tonga", "🇹🇴"),
        "TT": ("Trinidad and Tobago", "🇹🇹"),
        "TN": ("Tunisia", "🇹🇳"),
        "TR": ("Turkey", "🇹🇷"),
        "TM": ("Turkmenistan", "🇹🇲"),
        "TV": ("Tuvalu", "🇹🇻"),
        "UG": ("Uganda", "🇺🇬"),
        "UA": ("Ukraine", "🇺🇦"),
        "AE": ("United Arab Emirates", "🇦🇪"),
        "GB": ("United Kingdom", "🇬🇧"),
        "US": ("United States", "🇺🇸"),
        "UY": ("Uruguay", "🇺🇾"),
        "UZ": ("Uzbekistan", "🇺🇿"),
        "VU": ("Vanuatu", "🇻🇺"),
        "VA": ("Vatican City", "🇻🇦"),
        "VE": ("Venezuela", "🇻🇪"),
        "VN": ("Vietnam", "🇻🇳"),
        "YE": ("Yemen", "🇾🇪"),
        "ZM": ("Zambia", "🇿🇲"),
        "ZW": ("Zimbabwe", "🇿🇼"),
    }

    # Common aliases, typos, and variations mapped to ISO code
    ALIASES: Dict[str, str] = {
        "UK": "GB",
        "UNITED KINDOM": "GB",
        "UNITED KINGDOM": "GB",
        "GREAT BRITAIN": "GB",
        "ENGLAND": "GB",
        "SCOTLAND": "GB",
        "WALES": "GB",
        "USA": "US",
        "UNITED STATES": "US",
        "UNITED STATES OF AMERICA": "US",
        "U.S.A.": "US",
        "U.S.": "US",
        "AMERICA": "US",
        "VIETNAM": "VN",
        "VIET NAM": "VN",
        "VITE NAM": "VN",
        "VINTENM": "VN",
        "TURKEY": "TR",
        "TURKIYE": "TR",
        "TÜRKIYE": "TR",
        "TURKIA": "TR",
        "RUSSIA": "RU",
        "RUSSIAN FEDERATION": "RU",
        "KOREA": "KR",
        "SOUTH KOREA": "KR",
        "KOREA REPUBLIC OF": "KR",
        "REPUBLIC OF KOREA": "KR",
        "ROK": "KR",
        "KOREA, REPUBLIC OF": "KR",
        "UAE": "AE",
        "UNITED ARAB EMIRATES": "AE",
        "EMIRATES": "AE",
        "SAUDI ARABIA": "SA",
        "KSA": "SA",
        "KINGDOM OF SAUDI ARABIA": "SA",
        "CZECHIA": "CZ",
        "CZECH REPUBLIC": "CZ",
        "TAIWAN": "TW",
        "TAIWAN, PROVINCE OF CHINA": "TW",
        "ROC": "TW",
        "HONGKONG": "HK",
        "HONG KONG": "HK",
        "IRAN": "IR",
        "IRAN, ISLAMIC REPUBLIC OF": "IR",
        "PALESTINE": "PS",
        "STATE OF PALESTINE": "PS",
        "SYRIA": "SY",
        "SYRIAN ARAB REPUBLIC": "SY",
        "MOLDOVA": "MD",
        "MOLDOVA REPUBLIC OF": "MD",
        "MOLDOVA, REPUBLIC OF": "MD",
        "NETHERLANDS": "NL",
        "HOLLAND": "NL",
        "DEUTSCHLAND": "DE",
        "GERMANY": "DE",
        "BOSNIA": "BA",
        "BOSNIA AND HERZEGOVINA": "BA",
        "BOSNIA AND HERZEGOWINA": "BA",
    }

    @classmethod
    def normalize(cls, country_raw: str, code_hint: str = "") -> Tuple[str, str, str]:
        """
        Normalizes any country input into (iso_code, country_name, flag_emoji).
        Always returns a valid 2-letter ISO code and clean English country name.
        """
        code = (code_hint or "").strip().upper()
        if code and code in cls.ISO_COUNTRIES:
            name, flag = cls.ISO_COUNTRIES[code]
            return code, name, flag

        raw_str = (country_raw or "").strip()
        if not raw_str:
            return "UN", "Global / Unknown", "🌐"

        # Check raw code if length is 2
        if len(raw_str) == 2 and raw_str.upper() in cls.ISO_COUNTRIES:
            c = raw_str.upper()
            name, flag = cls.ISO_COUNTRIES[c]
            return c, name, flag

        # Clean string for regex search
        clean_upper = re.sub(r'[^A-Z\s]', '', raw_str.upper()).strip()
        clean_upper = re.sub(r'\s+', ' ', clean_upper)

        # Direct Alias Match
        if clean_upper in cls.ALIASES:
            c = cls.ALIASES[clean_upper]
            name, flag = cls.ISO_COUNTRIES[c]
            return c, name, flag

        # Match against official country names
        for iso_code, (official_name, flag) in cls.ISO_COUNTRIES.items():
            off_upper = official_name.upper()
            if off_upper == clean_upper or off_upper in clean_upper or clean_upper in off_upper:
                return iso_code, official_name, flag

        # Fallback partial matching on aliases
        for alias, iso_code in cls.ALIASES.items():
            if alias in clean_upper:
                name, flag = cls.ISO_COUNTRIES[iso_code]
                return iso_code, name, flag

        return "UN", "Global / Unknown", "🌐"

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
        transport_security: str = "",
        exit_ip: str = "",
        speed_mbps: float = 0.0,
        latency_ms: int = 0,
        network_score: int = 0,
        status: str = "verified",
        checked_at: str = "",
        config_sha256: str = "",
        ovpn_content: str = "",
        config_uri: str = "",
    ):
        self.source_id = str(source_id).strip()
        self.source_name = str(source_name).strip()
        self.profile_id = str(profile_id).strip() if profile_id else ""
        self.profile_source_url = profile_source_url.strip() if profile_source_url else ""
        self.server_page_url = server_page_url.strip() if server_page_url else ""

        # Enterprise Country Normalization
        iso_code, norm_name, flag = CountryNormalizer.normalize(country_name, country_code)
        self.country_code = iso_code
        self.country_name = norm_name
        self.flag = flag

        self.host = host or ""
        self.port = int(port or 1194)
        self.transport = (transport or "udp").lower()
        self.protocol = (protocol or "openvpn").lower()
        self.transport_security = (transport_security or "").lower()
        self.exit_ip = exit_ip or self.host
        self.speed_mbps = float(speed_mbps or 0.0)
        self.latency_ms = int(latency_ms or 0)
        self.network_score = int(network_score or 0)
        self.status = status or "verified"
        self.checked_at = checked_at or ""
        self.config_sha256 = config_sha256 or ""
        self.ovpn_content = ovpn_content or ""
        self.config_uri = config_uri or ""

    @property
    def server_id(self) -> str:
        return self.source_id

    @property
    def deduplication_key(self) -> str:
        """Unique key for deduplication across multi-sources"""
        if self.config_sha256:
            return f"hash_{self.config_sha256}"
        if self.host and self.port:
            return f"{self.protocol}_{self.host.lower()}_{self.port}_{self.transport}"
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
        if self.protocol != "openvpn":
            # Modern URI protocols (vless, vmess, shadowsocks, trojan, hysteria2) are valid via config_uri / URL
            return True
        if self.ovpn_content and len(self.ovpn_content) >= 50:
            return True
        if is_valid_http_url(self.profile_source_url):
            return True
        if self.profile_id and self.profile_id.isdigit():
            return True
        return False

    def to_dict(self) -> Dict[str, Any]:
        profile_url = f"/v1/profiles/{self.storage_id}.ovpn" if (self.protocol == "openvpn" and self.storage_id) else None
        return {
            "id": self.source_id,
            "source_id": self.source_id,
            "source_name": self.source_name,
            "profile_id": self.profile_id,
            "profile_source_url": self.profile_source_url,
            "server_page_url": self.server_page_url,
            "country_code": self.country_code,
            "country_name": self.country_name,
            "flag": self.flag,
            "host": self.host,
            "port": self.port,
            "transport": self.transport,
            "protocol": self.protocol,
            "transport_security": self.transport_security,
            "exit_ip": self.exit_ip,
            "speed_mbps": self.speed_mbps,
            "latency_ms": self.latency_ms,
            "network_score": self.network_score,
            "status": self.status,
            "checked_at": self.checked_at,
            "config_sha256": self.config_sha256,
            "config_uri": self.config_uri,
            "profile_url": profile_url,
            "profile_sha256": self.config_sha256,
        }

class MultiSourceHarvester:
    def __init__(self, access_key: Optional[str] = None, session: Optional[requests.Session] = None):
        self.access_key = (access_key or "").strip() or "pvlk_eb8cc33936641d9492cf0a2740c8511bb737e1a611fa1035cb7c5c3006513bcc"
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
            "User-Agent": "PublicVPNList-Enterprise-Sync/1.0",
            "Accept": "application/json, text/html, */*",
            "Accept-Language": "en-US,en;q=0.9",
        })

    def harvest_all_sources(self) -> Dict[str, ServerInfo]:
        """
        Harvests, normalizes, and deduplicates VPN servers from all requested multi-sources:
        1. PublicVPNList Official v1 REST API (All 6 protocols: openvpn, vless, vmess, shadowsocks, trojan, hysteria2)
        2. VPNGate Official API (CSV)
        3. Auto-OVPN GitHub Repo (JSON/Raw)
        4. VPNBook Free OpenVPN API (https://www.vpnbook.com)
        5. Riseup VPN
        6. PublicVPNList Legacy Native API (Fallback)
        """
        harvested: Dict[str, ServerInfo] = {}
        seen_dedup_keys: Set[str] = set()

        def add_server(server: ServerInfo):
            dedup_key = server.deduplication_key
            if dedup_key in seen_dedup_keys:
                existing = harvested.get(server.source_id)
                if existing and server.speed_mbps > existing.speed_mbps:
                    harvested[server.source_id] = server
                return
            seen_dedup_keys.add(dedup_key)
            harvested[server.source_id] = server

        # 1. PublicVPNList Official v1 REST API (Primary Multi-Protocol Source)
        try:
            pvl_v1_servers = self._fetch_publicvpnlist_v1_api()
            logger.info(f"PublicVPNList v1 REST API harvested {len(pvl_v1_servers)} active multi-protocol servers.")
            for s in pvl_v1_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"PublicVPNList v1 REST API harvest error: {e}")

        # 2. VPNGate Official CSV API
        try:
            vpngate_servers = self._fetch_vpngate_csv()
            logger.info(f"VPNGate API harvested {len(vpngate_servers)} active servers with decoded OVPN profiles.")
            for s in vpngate_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"VPNGate API harvest error: {e}")

        # 3. Auto-OVPN GitHub Repo
        try:
            auto_servers = self._fetch_auto_ovpn_github()
            logger.info(f"Auto-OVPN GitHub harvested {len(auto_servers)} active servers with decoded OVPN profiles.")
            for s in auto_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"Auto-OVPN GitHub harvest error: {e}")

        # 4. VPNBook
        try:
            vpnbook_servers = self._fetch_vpnbook()
            logger.info(f"VPNBook harvested {len(vpnbook_servers)} active servers with embedded credentials.")
            for s in vpnbook_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"VPNBook harvest error: {e}")

        # 5. Riseup VPN
        try:
            riseup_servers = self._fetch_riseup_vpn()
            logger.info(f"Riseup VPN harvested {len(riseup_servers)} servers.")
            for s in riseup_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"Riseup VPN harvest error: {e}")

        # 6. PublicVPNList Legacy Native API (Fallback if v1 REST API incomplete)
        try:
            native_servers = self._fetch_publicvpnlist_native()
            logger.info(f"PublicVPNList Legacy Native API harvested {len(native_servers)} active servers.")
            for s in native_servers.values():
                add_server(s)
        except Exception as e:
            logger.warning(f"PublicVPNList Legacy Native API harvest error: {e}")

        if not harvested:
            raise SourceUnavailableError("Failed to harvest servers from any public source.")

        logger.info(f"Multi-source harvesting & deduplication completed. Total unique servers harvested: {len(harvested)}")
        return harvested

    def _fetch_publicvpnlist_v1_api(self) -> Dict[str, ServerInfo]:
        """
        Fetches multi-protocol servers (vless, vmess, shadowsocks, trojan, hysteria2)
        from publicvpnlist.com/api/v1/servers using the permanent Bearer Key.
        """
        servers: Dict[str, ServerInfo] = {}
        protocols = ["vless", "vmess", "shadowsocks", "trojan", "hysteria2"]

        headers = {
            "Authorization": f"Bearer {self.access_key}",
            "Accept": "application/json"
        }

        for proto in protocols:
            url = f"https://publicvpnlist.com/api/v1/servers?protocol={proto}&status=online&per_page=100"
            try:
                res = self.session.get(url, headers=headers, timeout=(10, 30))
                if res.status_code == 200:
                    payload = res.json()
                    data_list = payload.get("data", [])
                    for item in data_list:
                        if not isinstance(item, dict):
                            continue

                        dl_url = str(item.get("config_download_url") or "").strip()
                        if not dl_url:
                            continue

                        sid = str(item.get("id") or "").strip()
                        if not sid:
                            continue

                        ip = str(item.get("ip") or item.get("exit_ip") or "")
                        port = int(item.get("port") or 443)
                        transport = str(item.get("transport") or "tcp").lower()
                        transport_sec = str(item.get("transport_security") or "")
                        c_code = str(item.get("country_code") or "").upper()
                        c_name = str(item.get("country_name") or "")

                        speed = float(item.get("speed_mbps") or item.get("checker_measured_throughput_mbps") or 0.0)
                        latency = int(item.get("latency_ms") or item.get("checker_measured_tunnel_rtt_ms") or 0)
                        score = int(item.get("technical_quality_score") or 50)

                        config_uri = str(item.get("config_uri") or dl_url)
                        sha256_hash = str(item.get("config_sha256") or "")

                        server_info = ServerInfo(
                            source_id=sid,
                            source_name="publicvpnlist_v1",
                            profile_source_url=dl_url,
                            server_page_url=dl_url,
                            country_code=c_code,
                            country_name=c_name,
                            host=ip,
                            port=port,
                            transport=transport,
                            protocol=proto,
                            transport_security=transport_sec,
                            exit_ip=ip,
                            speed_mbps=speed,
                            latency_ms=latency,
                            network_score=score,
                            config_sha256=sha256_hash,
                            config_uri=config_uri
                        )
                        servers[sid] = server_info
                else:
                    logger.warning(f"PublicVPNList v1 API returned HTTP {res.status_code} for protocol {proto}: {res.text[:120]}")

            except Exception as e:
                logger.warning(f"Failed to fetch PublicVPNList v1 protocol {proto}: {e}")

        return servers

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

        username = "vpnbook"
        user_match = re.search(r'Username\s*([a-zA-Z0-9]+)', page_text, re.I)
        if user_match:
            raw_u = user_match.group(1).strip()
            if raw_u.lower().startswith("vpnbook"):
                username = "vpnbook"

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
