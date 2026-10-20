"""Snapshot/restore only the dedicated emulator settings used by lifecycle acceptance."""
import re
import time


class EmulatorEnvironment:
    def __init__(self, command, settle_timeout=10, poll_seconds=0.2):
        self.command = command
        self.original = None
        self.changed = False
        self.settle_timeout = settle_timeout
        self.poll_seconds = poll_seconds

    def snapshot(self):
        run = self.command
        airplane = run('shell', 'cmd', 'connectivity', 'airplane-mode')
        wifi = run('shell', 'cmd', 'wifi', 'status').splitlines()[0]
        mobile = run('shell', 'settings', 'get', 'global', 'mobile_data')
        stay = run('shell', 'settings', 'get', 'global', 'stay_on_while_plugged_in')
        battery = run('shell', 'dumpsys', 'battery')
        power = run('shell', 'dumpsys', 'power')
        deep = run('shell', 'dumpsys', 'deviceidle', 'get', 'deep')
        awake = re.search(r'mWakefulness=(\w+)', power)
        flags = dict(re.findall(r'(AC|USB|Wireless|Dock) powered: (true|false)', battery))
        if (airplane not in ('enabled', 'disabled') or wifi not in ('Wifi is enabled', 'Wifi is disabled')
                or mobile not in ('0', '1') or not stay.isdigit() or len(flags) != 4
                or 'UPDATES STOPPED' in battery or deep != 'ACTIVE'
                or awake is None or awake[1] not in ('Awake', 'Asleep')):
            raise RuntimeError('Unqualified emulator power/network baseline; no setting changed')
        self.original = dict(airplane=airplane, wifi=wifi, mobile=mobile, stay=stay,
                             power_flags=flags, awake=awake[1])
        return self.original

    def offline(self):
        if self.original is None:
            raise RuntimeError('Snapshot required before emulator mutation')
        self.changed = True  # A command timeout is an uncertain mutation, too.
        run = self.command
        run('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
        run('shell', 'svc', 'wifi', 'disable')
        run('shell', 'svc', 'data', 'disable')
        if run('shell', 'cmd', 'connectivity', 'airplane-mode') != 'enabled':
            raise RuntimeError('Offline setting not confirmed')

    def doze(self):
        if self.original is None:
            raise RuntimeError('Snapshot required before emulator mutation')
        self.changed = True
        run = self.command
        run('shell', 'settings', 'put', 'global', 'stay_on_while_plugged_in', '0')
        run('shell', 'dumpsys', 'battery', 'unplug')
        run('shell', 'input', 'keyevent', 'KEYCODE_SLEEP')
        run('shell', 'dumpsys', 'deviceidle', 'force-idle')
        if run('shell', 'dumpsys', 'deviceidle', 'get', 'deep') != 'IDLE':
            raise RuntimeError('Deep idle not confirmed')

    def restore(self):
        if not self.changed:
            return {'verified': True, 'changed': False}
        old = self.original
        run = self.command
        errors = []
        actions = [
            ('shell', 'dumpsys', 'deviceidle', 'unforce'),
            ('shell', 'dumpsys', 'battery', 'reset'),
            ('shell', 'settings', 'put', 'global', 'stay_on_while_plugged_in', old['stay']),
            ('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable' if old['airplane'] == 'enabled' else 'disable'),
            ('shell', 'svc', 'wifi', 'enable' if old['wifi'] == 'Wifi is enabled' else 'disable'),
            ('shell', 'svc', 'data', 'enable' if old['mobile'] == '1' else 'disable'),
            ('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP' if old['awake'] == 'Awake' else 'KEYCODE_SLEEP'),
        ]
        for action in actions:
            try:
                run(*action)
            except Exception as error:
                errors.append(str(error))
        deadline = time.monotonic() + self.settle_timeout
        restored = False
        last_error = 'Restored emulator settings differ from their snapshot'
        while True:
            try:
                current = self.snapshot()
                if current == old:
                    restored = True
                    break
                last_error = 'Restored emulator settings differ from their snapshot'
            except Exception as error:
                last_error = str(error)
            if time.monotonic() >= deadline:
                break
            time.sleep(self.poll_seconds)
        if not restored:
            errors.append(last_error)
        # Keep the original snapshot for any recovery retry, even if verification failed.
        self.original = old
        if not errors:
            self.changed = False
        return {'verified': not errors, 'changed': True, 'errors': errors}
