"""Drive the NightRec production UI over adb for verification scenarios.

Usage:
  python3 ui.py dump                  # print visible texts
  python3 ui.py tap "<text>" [index]  # tap n-th (default first) node with exact text
  python3 ui.py tap-contains "<sub>"  # tap first node whose text contains sub
  python3 ui.py shot <path>           # screencap to local path
"""
import re, sys, subprocess
from pathlib import Path

serial = next(l.split('=', 1)[1] for l in Path('local.properties').read_text().splitlines()
              if l.startswith('ANDROID_SERIAL='))
ADB = ['adb', '-s', serial]
PKG = 'com.nightrec.app'


def adb(*p):
    return subprocess.check_output(list(ADB) + list(p), text=True)


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    xml = adb('shell', 'cat', '/sdcard/ui.xml')
    return re.findall(r'<node[^>]*>', xml)


def text_of(n):
    m = re.search(r'text="([^"]*)"', n)
    return m.group(1) if m else ''


def bounds(n):
    b = re.findall(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n)
    if not b:
        return None
    x1, y1, x2, y2 = map(int, b[0])
    return (x1 + x2) // 2, (y1 + y2) // 2


def main():
    cmd = sys.argv[1]
    if cmd == 'dump':
        for n in nodes():
            if f'package="{PKG}"' in n and text_of(n):
                print(text_of(n))
        return
    if cmd == 'list':
        for n in nodes():
            if f'package="{PKG}"' in n and text_of(n):
                print(text_of(n), bounds(n))
        return
    if cmd == 'tap':
        want = sys.argv[2]
        idx = int(sys.argv[3]) if len(sys.argv) > 3 else 0
        hits = [n for n in nodes() if f'package="{PKG}"' in n and text_of(n) == want and bounds(n)]
        if len(hits) <= idx:
            raise SystemExit(f'no node "{want}" idx={idx} (found {len(hits)})')
        x, y = bounds(hits[idx])
        adb('shell', 'input', 'tap', str(x), str(y))
        print('tapped', want, idx, x, y)
        return
    if cmd == 'tap-contains':
        want = sys.argv[2]
        hits = [n for n in nodes() if f'package="{PKG}"' in n and want in text_of(n) and bounds(n)]
        if not hits:
            raise SystemExit(f'no node containing "{want}"')
        x, y = bounds(hits[0])
        adb('shell', 'input', 'tap', str(x), str(y))
        print('tapped~', want, x, y)
        return
    if cmd == 'shot':
        adb('shell', 'screencap', '-p', '/sdcard/ui-shot.png')
        subprocess.check_call(list(ADB) + ['pull', '/sdcard/ui-shot.png', sys.argv[2]],
                              stdout=subprocess.DEVNULL)
        print('saved', sys.argv[2])
        return
    raise SystemExit('unknown command ' + cmd)


if __name__ == '__main__':
    main()