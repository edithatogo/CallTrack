# Android 17 call routing plan

- [x] Map native source and destination APIs and preserve the existing release scope.
- [x] Exercise source matching, preserved activation, and independent recovery. Implement opt-in routing in the existing automatic settings screen.
- [x] Run source-matched host and API 37 emulator acceptance, including rendered controls, migration and process recovery. Complete protected hosted checks.
- [x] Verify an in-place upgrade of the authorised dev package on the attached phone.
- [ ] Verify a controlled call from each selected third-party app and a readback from each nominated calendar before claiming that physical route.

Historical import remains bound to its saved phone-call snapshot. An app-call backfill needs an explicit source selection that persists with each confirmed import so recovery can replay the same scope.

## Development checkpoint

- The API 37 ARM64 provider fixture read attributed app calls through Android's unified log and ran WorkManager against the native calendar provider. One approved app call produced one event in each of two calendars across repeated runs. A deleted calendar and its replacement with the same numeric ID received zero events. The test removed its synthetic data and APKs.
- The API 37 settings, historical import and process recovery journeys passed. The accessibility journey exposed a navigation action clipped below the status details. Navigation now precedes those details. The same labels, 48 dp controls and keyboard-focus checks passed at the default phone size and a 1067 dp wide tablet layout. The test harness restored its emulator and verified fixture cleanup.
- Android CLI installed dev build 13 in place, preserving the original installation. Android's installed APK SHA-256 matched `070341d289a69e6139beb3fec5247ef0eb84e7aadea5eb9be494133c6d956dfe` and the launcher opened. The preceding development check verified the saved manual destination and an inactive routing policy. Installed app discovery on the attached phone showed Teams, WhatsApp, Signal and Telegram.
- [PR #133](https://github.com/edithatogo/CallTrack/pull/133) passed the hosted and security requirements and merged. The current Android 17 hosted settings, import, recovery and accessibility suites also passed after [PR #136](https://github.com/edithatogo/CallTrack/actions/runs/45581261845).
