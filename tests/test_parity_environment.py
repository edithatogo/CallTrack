"""Emulator state control-flow tests; these are not native power/network acceptance."""
import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from parity_environment import EmulatorEnvironment, EmulatorDisplayEnvironment


class EnvironmentSafety(unittest.TestCase):
    def setUp(self):
        self.values = {'airplane': 'disabled', 'wifi': 'Wifi is enabled', 'mobile': '1',
                       'stay': '1', 'battery': False, 'deep': 'ACTIVE', 'awake': 'Awake'}
        self.calls = []
        self.fail = None
        self.environment = EmulatorEnvironment(self.command, settle_timeout=0)

    def command(self, *args):
        self.calls.append(args)
        if self.fail == args:
            raise RuntimeError('Injected uncertain emulator command')
        tail = args[1:]
        v = self.values
        if tail[:3] == ('cmd', 'connectivity', 'airplane-mode'):
            if len(tail) == 4: v['airplane'] = 'enabled' if tail[-1] == 'enable' else 'disabled'
            return v['airplane']
        if tail == ('cmd', 'wifi', 'status'): return v['wifi']
        if tail[:2] == ('svc', 'wifi'):
            v['wifi'] = 'Wifi is enabled' if tail[-1] == 'enable' else 'Wifi is disabled'; return ''
        if tail[:2] == ('svc', 'data'):
            v['mobile'] = '1' if tail[-1] == 'enable' else '0'; return ''
        if tail[:2] == ('settings', 'get'): return v['mobile' if tail[-1] == 'mobile_data' else 'stay']
        if tail[:2] == ('settings', 'put'): v['stay'] = tail[-1]; return ''
        if tail == ('dumpsys', 'battery'):
            return ('UPDATES STOPPED\n' if v['battery'] else '') + 'AC powered: true\nUSB powered: false\nWireless powered: false\nDock powered: false'
        if tail[:2] == ('dumpsys', 'battery'): v['battery'] = tail[-1] == 'unplug'; return ''
        if tail == ('dumpsys', 'power'): return 'mWakefulness=' + v['awake']
        if tail == ('dumpsys', 'deviceidle', 'get', 'deep'): return v['deep']
        if tail[:2] == ('dumpsys', 'deviceidle'):
            v['deep'] = 'IDLE' if tail[-1] == 'force-idle' else 'ACTIVE'; return ''
        if tail[:2] == ('input', 'keyevent'):
            v['awake'] = 'Awake' if tail[-1] == 'KEYCODE_WAKEUP' else 'Asleep'; return ''
        raise AssertionError(args)

    def test_mutation_requires_complete_snapshot(self):
        with self.assertRaisesRegex(RuntimeError, 'Snapshot'): self.environment.offline()
        self.assertFalse(self.calls)
        self.values['battery'] = True
        with self.assertRaisesRegex(RuntimeError, 'baseline'): self.environment.snapshot()
        self.assertIsNone(self.environment.original)

    def test_legacy_network_commands_restore_the_original_native_state(self):
        original = self.command
        def legacy(*args):
            if args[:4] == ('shell', 'cmd', 'connectivity', 'airplane-mode'):
                return 'No shell command implementation.'
            if args == ('shell', 'dumpsys', 'wifi'):
                return self.values['wifi'].replace('Wifi', 'Wi-Fi')
            if args == ('shell', 'settings', 'get', 'global', 'airplane_mode_on'):
                return '1' if self.values['airplane'] == 'enabled' else '0'
            if args[:5] == ('shell', 'settings', 'put', 'global', 'airplane_mode_on'):
                self.values['airplane'] = 'enabled' if args[-1] == '1' else 'disabled'
                return ''
            if args[:3] == ('shell', 'am', 'broadcast'):
                return 'Broadcast completed: result=0'
            return original(*args)
        self.environment = EmulatorEnvironment(legacy, settle_timeout=0, sdk='26')
        before = self.environment.snapshot().copy()
        self.assertTrue(self.environment.legacy_network)
        self.environment.offline()
        self.assertEqual('enabled', self.values['airplane'])
        self.assertTrue(self.environment.restore()['verified'])
        self.assertEqual(before, self.environment.snapshot())

    def test_older_battery_service_without_dock_flag_is_qualified(self):
        original = self.command
        def old_battery(*args):
            value = original(*args)
            return value.replace('\nDock powered: false', '') if args == ('shell', 'dumpsys', 'battery') else value
        self.environment.command = old_battery
        self.assertEqual(3, len(self.environment.snapshot()['power_flags']))
        self.environment.doze()
        self.assertTrue(self.environment.restore()['verified'])

    def test_offline_and_doze_restore_original_settings(self):
        original = self.environment.snapshot().copy()
        self.environment.offline()
        self.environment.doze()
        self.assertEqual('IDLE', self.values['deep'])
        self.assertTrue(self.environment.restore()['verified'])
        self.assertEqual(original, self.environment.snapshot())

    def test_uncertain_mutation_still_requires_restoration(self):
        self.environment.snapshot()
        self.fail = ('shell', 'svc', 'wifi', 'disable')
        with self.assertRaisesRegex(RuntimeError, 'uncertain'): self.environment.offline()
        self.assertTrue(self.environment.changed)
        self.fail = None
        self.assertTrue(self.environment.restore()['verified'])

    def test_reboot_requires_complete_snapshot(self):
        with self.assertRaisesRegex(RuntimeError, 'Snapshot'):
            self.environment.reboot()
        self.assertFalse(self.calls)

    def test_uncertain_reboot_restores_original_awake_state(self):
        original = self.environment.snapshot().copy()
        original_command = self.environment.command
        def uncertain_reboot(*args):
            if args == ('reboot',):
                self.values['awake'] = 'Asleep'
                raise RuntimeError('Injected uncertain reboot')
            return original_command(*args)
        self.environment.command = uncertain_reboot
        with self.assertRaisesRegex(RuntimeError, 'uncertain'):
            self.environment.reboot()
        self.assertTrue(self.environment.changed)
        self.assertTrue(self.environment.restore()['verified'])
        self.assertEqual(original, self.environment.snapshot())
        self.assertIn(('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP'), self.calls)

    def test_failed_restore_continues_other_actions_and_retains_retry_snapshot(self):
        original = self.environment.snapshot().copy()
        self.environment.doze()
        self.fail = ('shell', 'dumpsys', 'battery', 'reset')
        self.assertFalse(self.environment.restore()['verified'])
        self.assertIn(('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP'), self.calls)
        self.assertTrue(self.environment.changed)
        self.assertEqual(original, self.environment.original)
        self.fail = None
        self.assertTrue(self.environment.restore()['verified'])


    def test_asynchronous_wifi_readback_must_settle_before_reporting_restore_failure(self):
        self.environment.snapshot()
        self.environment.offline()
        original_command = self.environment.command
        reads = []
        def delayed_wifi(*args):
            result = original_command(*args)
            if args == ('shell', 'cmd', 'wifi', 'status'):
                reads.append(result)
                if len(reads) == 1: return 'Wifi is disabled'
            return result
        self.environment.command = delayed_wifi
        self.environment.settle_timeout = 0.1
        self.environment.poll_seconds = 0.001
        self.assertTrue(self.environment.restore()['verified'])
        self.assertGreaterEqual(len(reads), 2)


class DisplayEnvironment(unittest.TestCase):
    def test_display_only_restore_uses_no_network_commands(self):
        calls = []
        state = {'awake': 'Asleep'}
        def command(*args):
            calls.append(args)
            if args == ('shell', 'dumpsys', 'power'):
                return 'mWakefulness=' + state['awake']
            if args[:3] == ('shell', 'input', 'keyevent'):
                state['awake'] = 'Asleep' if args[-1] == 'KEYCODE_SLEEP' else 'Awake'
                return ''
            raise AssertionError(args)
        env = EmulatorDisplayEnvironment(command)
        old = env.snapshot().copy()
        env.changed = True
        state['awake'] = 'Awake'
        self.assertTrue(env.restore()['verified'])
        self.assertEqual(old, env.snapshot())
        self.assertFalse(any('connectivity' in args or 'wifi' in args for args in calls))

    def test_unknown_display_state_is_refused(self):
        env = EmulatorDisplayEnvironment(lambda *args: 'mWakefulness=Dreaming')
        with self.assertRaisesRegex(RuntimeError, 'display baseline'):
            env.snapshot()
        self.assertFalse(env.changed)
