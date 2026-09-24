import unittest
from sync.source import ServerInfo
from sync.ovpn import OvpnDownloader

class TestPvlDownloader(unittest.TestCase):
    def test_pvl_token_download(self):
        downloader = OvpnDownloader()
        server = ServerInfo(
            source_id="pvl_674610",
            source_name="publicvpnlist",
            profile_id="674610",
            profile_source_url="https://publicvpnlist.com/download/674610/",
            server_page_url="https://publicvpnlist.com/download/674610/"
        )
        result = downloader.download_profile(server)
        self.assertTrue(result.is_success, f"Download failed: {result.error}")
        self.assertGreater(result.bytes_downloaded, 100)
        self.assertEqual(result.target_type, "pvl_token_download")

if __name__ == "__main__":
    unittest.main()
