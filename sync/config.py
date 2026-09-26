import os
from dataclasses import dataclass
from typing import Optional

@dataclass
class Config:
    base_url: str = os.getenv("PUBLICVPNLIST_BASE_URL", "https://publicvpnlist.com")
    access_key: Optional[str] = os.getenv("PUBLICVPNLIST_ACCESS_KEY", "pvlk_eb8cc33936641d9492cf0a2740c8511bb737e1a611fa1035cb7c5c3006513bcc")
    public_data_base_url: str = os.getenv("PUBLIC_DATA_BASE_URL", "https://vpn.example.com")

    # R2 Storage
    r2_bucket_name: str = os.getenv("R2_BUCKET_NAME", "publicvpnlist-profiles")
    cloudflare_account_id: Optional[str] = os.getenv("CLOUDFLARE_ACCOUNT_ID")
    r2_endpoint: Optional[str] = os.getenv("R2_ENDPOINT")
    r2_access_key_id: Optional[str] = os.getenv("R2_ACCESS_KEY_ID")
    r2_secret_access_key: Optional[str] = os.getenv("R2_SECRET_ACCESS_KEY")

    # Rate limiting & Safety
    max_workers: int = int(os.getenv("MAX_WORKERS", "4"))
    min_server_ratio: float = float(os.getenv("MIN_SERVER_RATIO", "0.90")) # Allow max 10% drop
    min_absolute_servers: int = int(os.getenv("MIN_ABSOLUTE_SERVERS", "1"))
    dry_run: bool = os.getenv("DRY_RUN", "false").lower() in ("true", "1", "yes")

    def get_r2_endpoint_url(self) -> str:
        if self.r2_endpoint:
            return self.r2_endpoint
        if self.cloudflare_account_id:
            return f"https://{self.cloudflare_account_id}.r2.cloudflarestorage.com"
        return ""
