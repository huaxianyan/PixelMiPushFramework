"""Check an unsigned normal Release APK; does not install or execute Android code."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile

from dex_inventory import read_dex
from verify_hook_artifact import ASPECTS, ROOT

# Existing framework API declarations must not shadow Android's own classes.
STUBS = {
    'android.app.ActivityManager', 'android.app.AppGlobals', 'android.app.AppOpsManager',
    'android.content.ContentResolver', 'android.content.IContentProvider',
    'android.content.IntentSender', 'android.telephony.TelephonyManager',
    'android.content.pm.IPackageDataObserver', 'android.content.pm.IPackageDeleteObserver',
    'android.content.pm.IPackageStatsObserver', 'android.content.pm.KeySet',
    'android.content.pm.PackageManager',
}
# Frozen 3.7.9 notification/dedup signatures, including the return type.
METHODS = {
    'Lcom/xiaomi/push/service/MIPushNotificationHelper;':
        'notifyPushMessage(Landroid/content/Context;Lcom/xiaomi/xmpush/thrift/XmPushActionContainer;[B)Lcom/xiaomi/push/service/MIPushNotificationHelper$NotifyPushMessageInfo;',
    'Lcom/xiaomi/push/service/MiPushMessageDuplicate;':
        'isDuplicateMessage(Lcom/xiaomi/push/service/XMPushService;Ljava/lang/String;Ljava/lang/String;)Z',
}


def descriptor(name):
    return 'L' + name.replace('.', '/') + ';'


def expected_sdks():
    """Read the SDK levels from the root build script instead of repeating them.

    They were hard-coded here once, and the assertion went stale the moment a later
    batch raised minSdk from 21 to 23: the value lives one directory up, so the copy
    is what silently rots.
    """
    text = (ROOT / 'build.gradle').read_text(encoding='utf-8')

    def level(name):
        match = re.search(r'%s\s*=\s*(\d+)' % name, text)
        if not match:
            raise ValueError('Cannot read %s from the root build script' % name)
        return match.group(1)

    return level('minSdkVersion'), level('targetSdkVersion')


def verify(apk, tools):
    def tool(name, *args):
        executable = tools / (name + ('.exe' if os.name == 'nt' else ''))
        result = subprocess.run([str(executable), *map(str, args)], capture_output=True, check=True)
        return result.stdout.decode('utf-8', errors='replace')

    badging = tool('aapt2', 'dump', 'badging', apk)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not package or package.group(1, 2) != ('com.xiaomi.xmsf', '1003003000'):
        raise ValueError('Package identity or normal variant versionCode changed')
    min_sdk, target_sdk = expected_sdks()
    if (f"minSdkVersion:'{min_sdk}'" not in badging
            or f"targetSdkVersion:'{target_sdk}'" not in badging):
        raise ValueError(
            f'Unexpected minSdk or targetSdk: expected {min_sdk}/{target_sdk} '
            f'as declared in build.gradle'
        )
    if 'application-debuggable' in badging:
        raise ValueError('Expected a non-debuggable Release APK')
    manifest = tool('aapt2', 'dump', 'xmltree', apk, '--file', 'AndroidManifest.xml')
    tool('zipalign', '-c', '-P', '16', '4', apk)

    components, tag = set(), None
    for line in manifest.splitlines():
        element = re.search(r' E: ([\w-]+)', line)
        if element:
            tag = element.group(1)
        if tag in {'application', 'activity', 'service', 'receiver', 'provider'}:
            name = re.search(r':name\(0x01010003\)="([^"]+)"', line)
            if name:
                value = name.group(1)
                if value.startswith('.'):
                    value = package.group(1) + value
                elif '.' not in value:
                    value = package.group(1) + '.' + value
                components.add(descriptor(value))
    if ':name(0x01010003)="Hook"' not in manifest:
        raise ValueError('Missing production AndroidX Startup hook registration')

    wanted = {descriptor(name) for name in ASPECTS} | METHODS.keys()
    all_classes, definitions, dex_reports = set(), {}, []
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if any(re.fullmatch(r'META-INF/[^/]+\.(RSA|DSA|EC|SF)', name, re.IGNORECASE) for name in names):
            raise ValueError('Release APK unexpectedly contains a JAR signature')
        dex_names = sorted(name for name in names if re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', name))
        if not dex_names:
            raise ValueError('APK contains no DEX files')
        for name in dex_names:
            magic, classes, methods = read_dex(archive.read(name), wanted)
            duplicates = all_classes & classes
            if duplicates:
                raise ValueError(f'Duplicate definitions across DEX files: {sorted(duplicates)}')
            all_classes.update(classes)
            definitions.update(methods)
            dex_reports.append({'file': name, 'magic': magic, 'classes': len(classes)})
        native = sorted(name for name in names if name.startswith('lib/') and name.endswith('.so'))

    data = apk.read_bytes()
    eocd = data.rfind(b'PK\x05\x06', max(0, len(data) - 65557))
    if eocd < 0 or eocd + 22 + struct.unpack_from('<H', data, eocd + 20)[0] != len(data):
        raise ValueError('Invalid ZIP end record')
    central_directory = struct.unpack_from('<I', data, eocd + 16)[0]
    if data[central_directory - 16:central_directory] == b'APK Sig Block 42':
        raise ValueError('Release APK unexpectedly contains an APK signing block')

    with zipfile.ZipFile(ROOT / 'mipush_hook/libs/miuipushsdkshared_3_7_9.jar') as sdk:
        sdk_classes = {'L' + name[:-6] + ';' for name in sdk.namelist() if name.endswith('.class')}
    required = sdk_classes | components | {
        'LHook;', 'Lcom/nihility/HookHandler;', 'Lcom/nihility/Dependencies;',
        'Landroid/app/AppOpsManagerExtender;', 'Lorg/aspectj/lang/ProceedingJoinPoint;',
        'Lorg/aspectj/runtime/reflect/Factory;',
        'Ltop/trumeet/mipush/provider/gen/db/DaoMaster;',
        'Ltop/trumeet/mipush/provider/gen/db/EventDao;',
        'Ltop/trumeet/mipush/provider/gen/db/RegisteredApplicationDao;',
    }
    missing = required - all_classes
    if missing:
        raise ValueError(f'Missing APK classes: {sorted(missing)}')
    for name in ASPECTS:
        owner = descriptor(name)
        if not definitions.get(owner, {}).get('aspectOf()' + owner, {}).get('has_code'):
            raise ValueError(f'Missing executable aspectOf definition: {name}')
    for owner, signature in METHODS.items():
        if not definitions.get(owner, {}).get(signature, {}).get('has_code'):
            raise ValueError(f'Missing SDK method code: {owner} {signature}')
    leaked = sorted(name for name in all_classes for stub in STUBS
                    if name == descriptor(stub) or name.startswith(descriptor(stub)[:-1] + '$'))
    if leaked:
        raise ValueError(f'Android compile-time stubs leaked into APK: {leaked}')
    return {
        'apk': str(apk), 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
        'package': package.group(1), 'version_code': int(package.group(2)),
        'version_name': package.group(3), 'min_sdk': int(min_sdk), 'target_sdk': int(target_sdk),
        'unsigned': True, 'alignment_verified': True, 'native_libraries': native,
        'dex': dex_reports, 'classes': len(all_classes), 'sdk_classes_retained': len(sdk_classes),
        'manifest_components_present': len(components), 'aspects_with_code': len(ASPECTS),
        'sdk_methods_with_code': METHODS,
        'scope': 'Static APK verification only; not ART, server registration or delivery proof',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--build-tools', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(verify(args.apk, args.build_tools), indent=2))


if __name__ == '__main__':
    main()
