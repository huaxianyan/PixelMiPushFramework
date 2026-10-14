"""Validate the universal APK against the ABI filters declared by this project."""
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def universal_abis(apk):
    text = (ROOT / 'push/build.gradle').read_text(encoding='utf-8')
    filters = re.findall(r'^\s*abiFilters\s+(.+)$', text, re.M)
    if len(filters) != 1:
        raise ValueError('Expected one ABI filter declaration in push/build.gradle')
    declared = set(re.findall(r"['\"]([^'\"]+)['\"]", filters[0]))
    with zipfile.ZipFile(apk) as archive:
        libraries = {}
        for name in archive.namelist():
            match = re.fullmatch(r'lib/([^/]+)/([^/]+\.so)', name)
            if match:
                libraries.setdefault(match.group(1), set()).add(match.group(2))
    if not declared or set(libraries) != declared:
        raise ValueError(f'Universal APK ABI mismatch: declared={sorted(declared)}, packaged={sorted(libraries)}')
    if any(names != next(iter(libraries.values())) for names in libraries.values()):
        raise ValueError('Native library coverage differs between declared ABIs')
    return sorted(libraries)
