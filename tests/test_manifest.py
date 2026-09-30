import os
import sys
import unittest
from datetime import datetime, timezone

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from sync.manifest import ManifestBuilder

class TestManifestBuilder(unittest.TestCase):
    def test_manifest_builder(self):
        builder = ManifestBuilder(schema_version=1)
        server_entries = {
            "101": {
                "metadata": {"id": "101", "country_code": "US", "country_name": "United States"},
                "sha256": "hash101",
                "size": 1200,
                "updated_at": "2026-09-23T16:00:00Z"
            }
        }
        changes = {"added": 1, "removed": 0, "updated": 0}
        now = datetime(2026, 9, 23, 16, 0, 0, tzinfo=timezone.utc)

        manifest, servers = builder.build_manifest_and_servers(server_entries, changes, now=now)

        self.assertEqual(manifest["schema_version"], 1)
        self.assertEqual(manifest["count"], 1)
        self.assertIn("101", manifest["servers"])
        self.assertEqual(manifest["servers"]["101"]["profile"], "profiles/101.ovpn")
        self.assertEqual(manifest["servers"]["101"]["sha256"], "hash101")

        self.assertEqual(servers["count"], 1)
        self.assertEqual(servers["servers"][0]["id"], "101")
        self.assertEqual(servers["servers"][0]["profile_url"], "/v1/profiles/101.ovpn")

if __name__ == "__main__":
    unittest.main()
