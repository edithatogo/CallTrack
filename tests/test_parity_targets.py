"""Keep the recovery contingency confined to exact owned AVD identities."""
import sys
from pathlib import Path
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from parity_targets import AVD, RECOVERY_AVD, MATRIX_AVDS, TRACK14_UI_AVD, is_dedicated_avd, is_dedicated_ui_avd


class DedicatedTargets(unittest.TestCase):
    def test_original_and_isolated_recovery_avds_are_accepted(self):
        for name in (AVD, RECOVERY_AVD, *MATRIX_AVDS):
            self.assertTrue(is_dedicated_avd(name + '\nOK\n'))

    def test_unrelated_prefix_suffix_or_embedded_identity_is_refused(self):
        for name in ('', 'unrelated', RECOVERY_AVD + '-other', 'unrelated\n' + AVD, 'x' + AVD):
            self.assertFalse(is_dedicated_avd(name))

    def test_track14_guest_is_ui_only_and_exact(self):
        self.assertTrue(is_dedicated_ui_avd(TRACK14_UI_AVD + '\nOK'))
        self.assertFalse(is_dedicated_avd(TRACK14_UI_AVD + '\nOK'))
        for name in (TRACK14_UI_AVD + '-other', 'x' + TRACK14_UI_AVD, 'unrelated\n' + TRACK14_UI_AVD):
            self.assertFalse(is_dedicated_ui_avd(name))
