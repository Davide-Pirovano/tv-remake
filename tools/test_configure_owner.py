import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest

from configure_owner import configure


class OwnerConfigurationTest(unittest.TestCase):
    def test_private_credentials_match_android_hash_vector(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "owner.json"
            configure("123456", "123456", target)
            payload = json.loads(target.read_text())
            salt = bytes.fromhex(payload["salt"])
            self.assertEqual(16, len(salt))
            expected = hashlib.pbkdf2_hmac("sha256", b"123456", salt, 60000, 32).hex()
            self.assertEqual(expected, payload["hash"])
            self.assertEqual({"salt", "hash"}, set(payload))
            if os.name != "nt":
                self.assertEqual(0o600, target.stat().st_mode & 0o777)
            original = target.read_bytes()
            with self.assertRaises(ValueError):
                configure("654321", "654321", target)
            self.assertEqual(original, target.read_bytes())

    def test_invalid_or_mismatched_pin_does_not_create_credentials(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "owner.json"
            for pin, confirmation in [("12345", "12345"), ("123456789", "123456789"),
                                      ("１２３４５６", "１２３４５６"), ("123456", "654321")]:
                with self.subTest(pin=pin), self.assertRaises(ValueError):
                    configure(pin, confirmation, target)
                self.assertFalse(target.exists())


if __name__ == "__main__":
    unittest.main()
