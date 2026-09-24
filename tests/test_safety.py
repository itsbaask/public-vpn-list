import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from sync.safety import validate_sync_safety, validate_source_id, SafetyValidationError

class TestSyncSafety(unittest.TestCase):
    def test_safety_zero_servers_fails(self):
        with self.assertRaises(SafetyValidationError):
            validate_sync_safety({}, previous_count=100)

    def test_safety_empty_or_none_id_fails(self):
        self.assertFalse(validate_source_id(""))
        self.assertFalse(validate_source_id(None))
        self.assertTrue(validate_source_id("pvl_0929170ea9cd9de8d24c106e"))
        self.assertTrue(validate_source_id("148528"))

    def test_safety_mass_drop_fails(self):
        servers = {f"pvl_{i}": {} for i in range(1, 50)}
        with self.assertRaises(SafetyValidationError):
            validate_sync_safety(servers, previous_count=100, min_ratio=0.90)

    def test_safety_valid_pvl_ids_passes(self):
        servers = {f"pvl_{i}": {} for i in range(1, 95)}
        validate_sync_safety(servers, previous_count=100, min_ratio=0.90)

if __name__ == "__main__":
    unittest.main()
