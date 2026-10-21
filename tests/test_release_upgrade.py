"""Controlled downgrade rejection must not be mistaken for an unrelated install failure."""
import sys
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import qualify_release_upgrade as upgrade


class UpgradeSafetyTest(unittest.TestCase):
    def test_refuses_physical_device_before_access(self):
        with patch.object(upgrade.subprocess, 'check_output') as read:
            with self.assertRaisesRegex(RuntimeError, 'refuses physical'):
                upgrade.qualify('phone', Path('/unused'))
            read.assert_not_called()

    def test_rejection_requires_failure_and_exact_android_reason(self):
        for code, error in ((0, 'INSTALL_FAILED_VERSION_DOWNGRADE'), (1, 'device offline'), (1, 'INSTALL_FAILED_UPDATE_INCOMPATIBLE')):
            with self.assertRaisesRegex(RuntimeError, 'controlled rejection'):
                upgrade.require_rejection(subprocess.CompletedProcess([], code, '', error), 'INSTALL_FAILED_VERSION_DOWNGRADE')
        upgrade.require_rejection(subprocess.CompletedProcess([], 1, '', 'Failure [INSTALL_FAILED_VERSION_DOWNGRADE]'), 'INSTALL_FAILED_VERSION_DOWNGRADE')
