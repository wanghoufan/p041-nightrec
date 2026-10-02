"""Drive only the debug Spike, and verify actual service outcomes.

Strict rules (round-2 lesson): never treat a click return code as success.
- start: require a NEW probe directory containing a START event.
- stop: require a STOP event inside the currently active directory.
If the required outcome is missing we exit non-zero and must NOT reinstall/force-stop.
"""
import re, sys, time, subprocess, xml.etree.ElementTree as E
from pathlib import Path

serial = next(l.split('=', 1)[1] for l in Path('local.properties').read_text().splitlines() if l.startswith('ANDROID_SERIAL='))
args = ['adb', '-s', serial]


def adb(*parts):
    return subprocess.check_output(args + list(parts), text=True)


def run_as(*parts):
    return adb('shell', 'run-as', 'com.nightrec.app', *parts)


def numeric_dirs():
    out = run_as('ls', 'files/probe')
    return sorted(x for x in out.split() if x.isdigit())


def events_of(d):
    try:
        return run_as('cat', 'files/probe/' + d + '/events.jsonl')
    except subprocess.CalledProcessError:
        return ''


def has(d, kind):
    return '"event":"%s"' % kind in events_of(d)


def tap(label):
    adb('shell', 'am', 'start', '-n', 'com.nightrec.app/.probe.ProbeActivity')
    adb('shell', 'uiautomator', 'dump', '/sdcard/nightrec-probe.xml')
    xml = adb('shell', 'cat', '/sdcard/nightrec-probe.xml')
    nodes = [n for n in E.fromstring(xml).iter('node') if n.get('text') == label and n.get('package') == 'com.nightrec.app']
    assert len(nodes) == 1, 'Expected exactly one NightRec probe button "%s" but saw %d' % (label, len(nodes))
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', nodes[0].get('bounds')))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def wait_for_new_start(before, timeout=25):
    deadline = time.time() + timeout
    while time.time() < deadline:
        new = [d for d in numeric_dirs() if d not in before]
        for d in new:
            if has(d, 'START'):
                return d
        time.sleep(0.4)
    return None


def wait_for_stop(active, timeout=25):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if has(active, 'STOP'):
            return True
        time.sleep(0.4)
    return False


def main():
    action = sys.argv[1]
    labels = {
        'start': '开始后台测试',
        'stop': '停止并保存测试',
        'play-a': '循环播放官方识曲测试音源',
        'play-b': '循环播放第二首测试音乐',
        'silence': '停止测试音乐',
    }
    if action == 'status':
        dirs = numeric_dirs()
        print('probe dirs:', dirs)
        for d in dirs:
            print(d, 'START' if has(d, 'START') else '-', 'STOP' if has(d, 'STOP') else '-',
                  'SEALED' if has(d, 'SEALED') else '-', 'FAIL' if has(d, 'FAIL') else '-')
        return
    label = labels[action]
    before = numeric_dirs()
    tap(label)
    if action == 'start':
        d = wait_for_new_start(before)
        if not d:
            raise SystemExit('START outcome missing in a NEW dir; DO NOT INSTALL OR FORCE STOP')
        print('Verified START in new probe dir', d)
        if has(d, 'FAIL'):
            raise SystemExit('Probe reported FAIL immediately in ' + d)
    elif action == 'stop':
        active = before[-1]
        if not has(active, 'START'):
            raise SystemExit('Newest dir %s has no START; refusing to stop blindly' % active)
        if not wait_for_stop(active):
            raise SystemExit('STOP outcome missing in %s; DO NOT INSTALL OR FORCE STOP' % active)
        print('Verified STOP in probe dir', active)
    else:
        print('Clicked', action, 'in current observed UI')


if __name__ == '__main__':
    main()