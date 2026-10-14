"""Archive-layout fixtures only, not executable ELF or Android runtime evidence.

The missing-ABI case enforces the universal asset contract in docs/RELEASING.md.
"""
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from apk_abi import universal_abis


class UniversalAbiTest(unittest.TestCase):
    def check_archive(self, missing=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'push').mkdir()
            (root / 'push/build.gradle').write_text(
                "abiFilters 'x86', 'armeabi-v7a', 'x86_64', 'arm64-v8a'\n",
                encoding='utf-8')
            apk = root / 'fixture.apk'
            with zipfile.ZipFile(apk, 'w') as archive:
                for abi in ('armeabi-v7a', 'x86_64', 'arm64-v8a', 'x86'):
                    if abi != missing:
                        archive.writestr(f'lib/{abi}/libandroidx.graphics.path.so', b'')
            with patch('apk_abi.ROOT', root):
                return universal_abis(apk)

    def test_complete_multi_architecture_package_lists_supported_devices(self):
        self.assertEqual(self.check_archive(), ['arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'])

    def test_package_missing_a_declared_device_architecture_cannot_use_universal_name(self):
        with self.assertRaises(ValueError):
            self.check_archive(missing='x86_64')


if __name__ == '__main__':
    unittest.main()
