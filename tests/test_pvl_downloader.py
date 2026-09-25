import unittest
import requests
import re
from sync.source import ServerInfo
from sync.ovpn import OvpnDownloader

class TestPvlDownloader(unittest.TestCase):
    def test_pvl_token_download(self):
        downloader = OvpnDownloader()
        sid = "58392"
        try:
            r = requests.get("https://publicvpnlist.com/", timeout=10)
            if r.status_code == 200:
                matches = re.findall(r'/server/(\d+)', r.text)
                if matches:
                    sid = matches[0]
        except Exception:
            pass

        server = ServerInfo(
            source_id=f"pvl_{sid}",
            source_name="publicvpnlist",
            profile_id=sid,
            profile_source_url=f"https://publicvpnlist.com/download/{sid}/",
            server_page_url=f"https://publicvpnlist.com/download/{sid}/"
        )
        result = downloader.download_profile(server)
        self.assertTrue(result.is_success, f"Download failed: {result.error}")
        self.assertGreater(result.bytes_downloaded, 100)
        self.assertEqual(result.target_type, "pvl_token_download")

if __name__ == "__main__":
    unittest.main()
