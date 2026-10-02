#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
phase="${1:?provide phase name}"
[[ "$phase" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
export ANDROID_SERIAL="$(python3 - <<'PYSERIAL'
from pathlib import Path
for line in Path('local.properties').read_text().splitlines():
    if line.startswith('ANDROID_SERIAL='): print(line.partition('=')[2]); break
PYSERIAL
)"
[[ -n "$ANDROID_SERIAL" ]]
mkdir -p docs/verification/device
./gradlew :app:installDebug --console=plain > "docs/verification/${phase}-install.log" 2>&1
adb shell am force-stop com.nightrec.app
adb shell am start -W -n com.nightrec.app/.MainActivity > "docs/verification/${phase}-start.log"
sleep 2
adb exec-out screencap -p > "docs/verification/device/${phase}.png"
app_pid="$(adb shell pidof com.nightrec.app | tr -d '\r')"
[[ -n "$app_pid" ]]
adb logcat -d --pid="$app_pid" -v brief > "docs/verification/${phase}-app-logcat.log"
if rg -n 'FATAL EXCEPTION|ANR in com.nightrec.app' "docs/verification/${phase}-app-logcat.log"; then exit 1; fi
python3 - "$phase" <<'PYVERIFY'
from pathlib import Path
import sys,struct
p=Path('docs/verification/device')/(sys.argv[1]+'.png')
b=p.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n';assert len(b)>1000
print('Screenshot verified:',p,struct.unpack('>II',b[16:24]))
PYVERIFY
