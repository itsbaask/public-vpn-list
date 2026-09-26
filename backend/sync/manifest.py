import json
import hashlib
from datetime import datetime, timezone
from typing import Dict, List, Any, Tuple

def generate_version_string(ts: datetime) -> str:
    iso_ts = ts.strftime("%Y-%m-%dT%H:%M:%SZ")
    random_suffix = hashlib.sha256(iso_ts.encode("utf-8")).hexdigest()[:6]
    return f"{iso_ts}-{random_suffix}"

class ManifestBuilder:
    def __init__(self, schema_version: int = 1):
        self.schema_version = schema_version

    def build_manifest_and_servers(
        self,
        server_entries: Dict[str, Dict[str, Any]],
        changes: Dict[str, int],
        now: datetime = None
    ) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        """
        Builds manifest.json and servers.json data dicts according to specifications.
        server_entries: dict of server_id -> {
            "metadata": dict,
            "sha256": str,
            "size": int,
            "updated_at": str
        }
        """
        if now is None:
            now = datetime.now(timezone.utc)

        version_str = generate_version_string(now)
        iso_now = now.strftime("%Y-%m-%dT%H:%M:%SZ")

        # Psychological & Mathematical Country-Based Tier Distribution Matrix
        from collections import defaultdict
        country_groups = defaultdict(list)
        for sid, info in server_entries.items():
            meta = info["metadata"]
            country = meta.get("country_name") or "Unknown"
            speed_val = float(meta.get("speed_mbps", 0.0) or 0.0)
            score_val = float(meta.get("network_score", 0) or 0)
            lat_val = float(meta.get("latency_ms", 100) or 100)
            comp_score = (speed_val * 100.0) + (score_val / 1000.0) + max(0.0, 100.0 - lat_val)
            country_groups[country].append((sid, comp_score))

        tier_assignment = {}
        for country, s_list in country_groups.items():
            s_list.sort(key=lambda x: x[1], reverse=True)
            n = len(s_list)
            # Scarcity quota: Free gets top 10% (max 2 per country)
            n_free = max(1, min(2, int(n * 0.10)))
            # Premium gets top 50%
            n_premium = max(1, int(n * 0.50))

            for idx, (sid, comp_score) in enumerate(s_list):
                if idx < n_free:
                    tier_assignment[sid] = "free"
                elif idx < n_premium:
                    tier_assignment[sid] = "premium"
                else:
                    tier_assignment[sid] = "premium_plus"

        manifest_servers = {}
        servers_list = []

        for sid, info in server_entries.items():
            meta = info["metadata"]
            sha256_val = info["sha256"]
            size_val = info.get("size", 0)
            updated_at_val = info.get("updated_at", iso_now)
            proto = str(meta.get("protocol", "openvpn")).lower()

            if proto == "openvpn":
                pid = meta.get("profile_id")
                storage_id = str(pid) if (pid and str(pid).isdigit()) else sid.removeprefix("pvl_")
                profile_rel_path = f"profiles/{storage_id}.ovpn"
                profile_full_url = meta.get("profile_url") or f"/v1/profiles/{storage_id}.ovpn"
            else:
                profile_rel_path = meta.get("config_uri") or meta.get("profile_source_url") or ""
                profile_full_url = profile_rel_path

            manifest_servers[sid] = {
                "profile": profile_rel_path,
                "sha256": sha256_val,
                "size": size_val,
                "updated_at": updated_at_val
            }

            srv_copy = dict(meta)
            srv_copy["profile_url"] = profile_full_url
            srv_copy["profile_sha256"] = sha256_val
            srv_copy["tier"] = tier_assignment.get(sid, "free")

            servers_list.append(srv_copy)

        servers_json_data = {
            "schema_version": self.schema_version,
            "version": version_str,
            "count": len(servers_list),
            "servers": servers_list
        }

        servers_bytes = json.dumps(servers_json_data, sort_keys=True).encode("utf-8")
        servers_sha256 = hashlib.sha256(servers_bytes).hexdigest()

        manifest_data = {
            "schema_version": self.schema_version,
            "version": version_str,
            "generated_at": iso_now,
            "source_snapshot": servers_sha256[:16],
            "count": len(manifest_servers),
            "servers_url": "/v1/servers.json",
            "profiles_prefix": "/v1/profiles/",
            "manifest_sha256": servers_sha256,
            "changes": {
                "added": changes.get("added", 0),
                "removed": changes.get("removed", 0),
                "updated": changes.get("updated", 0)
            },
            "servers": manifest_servers
        }

        return manifest_data, servers_json_data
