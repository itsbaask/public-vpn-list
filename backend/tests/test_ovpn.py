import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from sync.source import ServerInfo
from sync.ovpn import OvpnDownloader

class TestOvpnDownloader(unittest.TestCase):
    def test_inline_decoded_profile_download(self):
        downloader = OvpnDownloader(base_url="https://publicvpnlist.com")

        server = ServerInfo(
            source_id="vpngate_219_100_37_100_443",
            host="219.100.37.100",
            port=443,
            transport="tcp",
            ovpn_content="client\ndev tun\nproto tcp\nremote 219.100.37.100 443\n<ca>\nTEST_CERT\n</ca>\n"
        )

        result = downloader.download_profile(server)

        self.assertTrue(result.is_success)
        self.assertEqual(result.target_type, "inline_decoded_profile")
        self.assertIn("remote 219.100.37.100 443", result.content)

    def test_target_resolution_profile_source_url(self):
        downloader = OvpnDownloader(base_url="https://publicvpnlist.com")

        server = ServerInfo(
            source_id="ipspeed_12345",
            profile_source_url="https://ipspeed.info/download.php?id=12345"
        )

        target_type, target_url = downloader.resolve_download_target(server)

        self.assertEqual(target_type, "profile_source_url")
        self.assertEqual(target_url, "https://ipspeed.info/download.php?id=12345")

if __name__ == "__main__":
    unittest.main()
