"""Check the real hook AAR without loading Android classes or executing SDK code.

Usage: python scripts/verify_hook_artifact.py path/to/mipush_hook-debug.aar [...]
Requires the build JDK's javap on PATH or JAVA_HOME.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
# These are the existing production aspects, not a generated probe.
ASPECTS = {
    'com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect',
    'com.nihility.MethodHooker',
    'com.xiaomi.channel.commonutils.android.MIUIUtilsAspect',
    'com.xiaomi.clientreport.manager.ClientReportClientAspect',
    'com.xiaomi.mipush.sdk.ManifestCheckerAspect',
    'com.xiaomi.network.FallbackAspect',
    'com.xiaomi.push.service.PushHostManagerFactoryAspect',
    'com.xiaomi.push.service.XMPushServiceAspect',
    'com.xiaomi.push.service.clientReport.PushClientReportManagerAspect',
}
# Existing hook boundaries that must remain woven during a build-only migration.
BOUNDARIES = {
    'com/xiaomi/push/service/XMPushService.class': [
        'onCreate', 'onStartCommand', 'onStart', 'onBind', 'onDestroy', 'sendMessage',
    ],
    'com/xiaomi/smack/Connection.class': ['setConnectionStatus'],
    'com/xiaomi/push/service/MIPushEventProcessor.class': [
        'shouldSendBroadcast', 'postProcessMIPushMessage', 'buildIntent',
        'buildContainerHook', 'isIntentAvailable', 'processMIPushMessage',
    ],
    'com/xiaomi/push/service/MiPushMessageDuplicate.class': ['isDuplicateMessage'],
    'com/xiaomi/push/service/MIPushNotificationHelper.class': ['notifyPushMessage'],
}


def classes(data):
    result = {}
    with zipfile.ZipFile(data) as jar:
        for entry in jar.infolist():
            if entry.filename.endswith('.class'):
                if entry.filename in result:
                    raise ValueError(f'Duplicate class: {entry.filename}')
                result[entry.filename] = jar.read(entry)
    return result


def verify(aar_path, sdk, javap):
    with zipfile.ZipFile(aar_path) as aar:
        packaged = {}
        main_jar = aar.read('classes.jar')
        for entry in aar.infolist():
            if entry.filename == 'classes.jar' or (
                entry.filename.startswith('libs/') and entry.filename.endswith('.jar')
            ):
                batch = classes(io.BytesIO(aar.read(entry)))
                duplicate = packaged.keys() & batch.keys()
                if duplicate:
                    raise ValueError(f'Duplicate AAR classes: {sorted(duplicate)}')
                packaged.update(batch)
    missing = sdk.keys() - packaged.keys()
    if missing:
        raise ValueError(f'Missing SDK classes: {sorted(missing)}')
    for name in BOUNDARIES:
        if sdk[name] == packaged[name]:
            raise ValueError(f'Existing SDK hook boundary was not changed: {name}')
    for name in ['Hook', 'com.nihility.Dependencies', 'com.nihility.HookedMethodHandler']:
        if name.replace('.', '/') + '.class' not in packaged:
            raise ValueError(f'Missing hook implementation: {name}')
    with tempfile.TemporaryDirectory(prefix='mipush-hook-check-') as directory:
        jar = Path(directory) / 'classes.jar'
        jar.write_bytes(main_jar)
        result = subprocess.run(
            [javap, '-classpath', str(jar), '-p', *sorted(ASPECTS)],
            capture_output=True, check=True,
        )
        output = result.stdout.decode('utf-8', errors='replace')
        initialized = set(re.findall(r'public static ([\w.$]+) aspectOf\(\);', output))
        if initialized != ASPECTS:
            raise ValueError(f'Unexpected aspect initialization surface: {sorted(initialized)}')
        for name, advice_names in BOUNDARIES.items():
            result = subprocess.run(
                [javap, '-classpath', str(jar), '-c', '-p', name[:-6].replace('/', '.')],
                capture_output=True, check=True,
            )
            code = result.stdout.decode('utf-8', errors='replace')
            for advice in advice_names:
                call = f'com/nihility/MethodHooker.{advice}:'
                if call not in code:
                    raise ValueError(f'Missing handler invocation in {name}: {advice}')
    changed = sorted(name for name in sdk if sdk[name] != packaged[name])
    return {
        'aar': str(aar_path),
        'sha256': hashlib.sha256(aar_path.read_bytes()).hexdigest(),
        'sdk_classes': len(sdk),
        'packaged_classes': len(packaged),
        'byte_identical_sdk_classes': len(sdk) - len(changed),
        'changed_sdk_classes': changed,
        'initialized_aspects': sorted(initialized),
        'verified_handler_invocations': BOUNDARIES,
        'scope': 'AAR structure only; not advice ordering, ART execution or delivery proof',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('aar', type=Path, nargs='+')
    args = parser.parse_args()
    java_home = os.environ.get('JAVA_HOME')
    javap = str(Path(java_home) / 'bin' / ('javap.exe' if os.name == 'nt' else 'javap')) if java_home else shutil.which('javap')
    if not javap:
        parser.error('Set JAVA_HOME to the build JDK or add javap to PATH.')
    sdk = classes(ROOT / 'mipush_hook/libs/miuipushsdkshared_3_7_9.jar')
    print(json.dumps([verify(path, sdk, javap) for path in args.aar], indent=2))


if __name__ == '__main__':
    main()
