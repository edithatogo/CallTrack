"""Read-only readiness gate after dedicated-emulator identity validation."""


def require_healthy_boot(command):
    if command('shell', 'getprop', 'sys.boot_completed') != '1':
        raise RuntimeError('Boot incomplete')
    # A boot flag was observed while the phone service was absent and fixture
    # processes could not start. Refuse installation into that unhealthy guest.
    if command('shell', 'service', 'check', 'phone') != 'Service phone: found':
        raise RuntimeError('Android phone service unavailable; repair the guest before acceptance')
