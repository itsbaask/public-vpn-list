import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from sync.diff import compute_diff

class TestSyncDiff(unittest.TestCase):
    def test_diff_computation(self):
        prev_manifest = {
            "101": {"sha256": "abc"},
            "102": {"sha256": "def"},
            "103": {"sha256": "ghi"}
        }
        current_ids = {"102", "103", "104"}

        diff = compute_diff(prev_manifest, current_ids)

        self.assertEqual(diff.new_ids, {"104"})
        self.assertEqual(diff.removed_ids, {"101"})
        self.assertEqual(diff.common_ids, {"102", "103"})
        self.assertTrue(diff.has_changes)

if __name__ == "__main__":
    unittest.main()
