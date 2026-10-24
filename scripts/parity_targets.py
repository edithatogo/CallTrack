"""Exact dedicated emulator identities; never infer authorization from an emulator prefix."""
AVD = 'calltrack-parity-api36-20261020'
RECOVERY_AVD = 'calltrack-parity-api36-recovery-20261021'
MATRIX_AVDS = ('calltrack-release-api26-20261022', 'calltrack-release-api33-20261022')
TRACK14_UI_AVD = 'calltrack-track14-api36'


def is_dedicated_avd(output):
    lines = output.splitlines()
    return bool(lines) and lines[0].strip() in (AVD, RECOVERY_AVD, *MATRIX_AVDS)


def is_dedicated_ui_avd(output):
    """Allow the Track 14 guest only for rendered UI acceptance."""
    lines = output.splitlines()
    return bool(lines) and lines[0].strip() in (AVD, RECOVERY_AVD, *MATRIX_AVDS, TRACK14_UI_AVD)
