"""Background sampler for the T096/T097 lock-screen soak.

Every interval, append a JSON line with the facts needed for the soak report:
FGS aliveness, screen wakefulness, segment counts, crash/ANR counts, storage.
Does NOT touch app data or the network budget.
"""
import json, subprocess, sys, time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
serial = next(l.split('=', 1)[1] for l in (ROOT / 'local.properties').read_text().splitlines()
              if l.startswith('ANDROID_SERIAL='))
ADB = ['adb', '-s', serial]
PKG = 'com.nightrec.app'
SESSION = sys.argv[1] if len(sys.argv) > 1 else '5'
DURATION = int(sys.argv[2]) if len(sys.argv) > 2 else 11500
INTERVAL = int(sys.argv[3]) if len(sys.argv) > 3 else 60
OUT = ROOT / 'docs/verification/device/soak-samples.jsonl'


def sh(*p):
    try:
        return subprocess.check_output(list(ADB) + list(p), text=True, stderr=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        return ''


def snap(i):
    svc = sh('shell', 'dumpsys', 'activity', 'services', PKG)
    fgs = 'isForeground=true' in svc
    types = ''
    for line in svc.splitlines():
        if 'foregroundServiceType' in line or 'types=' in line:
            types = line.strip()
            break
    power = sh('shell', 'dumpsys', 'power')
    wake = next((l.split('=')[-1] for l in power.splitlines() if 'mWakefulness=' in l), '?')
    # segment files for the active session
    listing = sh('shell', 'run-as', PKG, 'ls', f'files/original/{SESSION}').split()
    segs = [x for x in listing if x.endswith('.m4a')]
    opens = [x for x in listing if x.endswith('.open')]
    sha = [x for x in listing if x.endswith('.sha256')]
    # crash / ANR from logcat
    log = sh('shell', 'logcat', '-d')
    fatal = log.count('FATAL EXCEPTION')
    anr = log.count('ANR in com.nightrec.app')
    df = sh('shell', 'df', '/data')
    free_kb = 0
    for line in df.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 4:
            try:
                free_kb = int(parts[3])
            except ValueError:
                pass
    return {
        'ts': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'i': i,
        'fgs': fgs,
        'fg_type_line': types,
        'wakefulness': wake,
        'sealed': len([s for s in segs if not s.endswith('.open')]),
        'open': len(opens),
        'sha256': len(sha),
        'fatal_total': fatal,
        'anr_total': anr,
        'data_free_kb': free_kb,
    }


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    start = time.time()
    i = 0
    while time.time() - start < DURATION:
        row = snap(i)
        with OUT.open('a') as f:
            f.write(json.dumps(row, ensure_ascii=False) + '\n')
        print(row, flush=True)
        i += 1
        time.sleep(INTERVAL)
    print('sampler done', flush=True)


if __name__ == '__main__':
    main()