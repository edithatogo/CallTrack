# Android 17 call routing plan

- [x] Map native source and destination APIs and preserve the existing release scope.
- [x] Exercise source matching, preserved activation, and independent recovery. Implement opt-in routing in the existing automatic settings screen.
- [x] Run source-matched host and API 37 emulator acceptance, including rendered controls, migration and process recovery. Complete protected hosted checks.
- [x] Verify an in-place upgrade of the authorised dev package on the attached phone.
- [ ] Verify a controlled call from each selected third-party app and a readback from each nominated calendar before claiming that physical route. The emulator's attributed call fixtures and multi-calendar checks provide development evidence. Physical app-route verification remains deferred with the device work.

## Historical imports and provider discovery

- [x] Offer Messenger as an installed candidate through targeted package visibility. Discover other participating apps through Android's callback or attributed history.
- [x] Bind each new historical preview to the selected source and calendar identity. Use that scope on resume. Preserve earlier phone-call imports, explicit confirmation, and duplicate protection.
- [x] Qualify app import, mixed-source exclusion, keyboard access, recovery, and calendar replacement on the dedicated Android 17 emulator. The existing hosted Android 17 and security checks remain required for merge.

Discovery can display installed candidates separately from observed shared history. Messenger uses targeted package visibility and Google Meet matches Android's callback discovery. Private device readback records the current eligibility signals.

Historical verification can use calls already present in Android's shared history. Live automatic delivery and remote calendar readback remain separate acceptance checks. Outlook uses the existing Calendar Provider route when its account exposes a writable calendar. A selected Outlook destination and remote synthetic-event readback are still required to qualify that route.

## Development checkpoint

- The API 37 ARM64 provider fixture read attributed app calls through Android's unified log and ran WorkManager against the native calendar provider. One approved app call produced one event in each of two calendars across repeated runs. A deleted calendar and its replacement with the same numeric ID received zero events. The test removed its synthetic data and APKs.
- The API 37 settings, historical import and process recovery journeys passed. The accessibility journey exposed a navigation action clipped below the status details. Navigation now precedes those details. The same labels, 48 dp controls and keyboard-focus checks passed at the default phone size and a 1067 dp wide tablet layout. The test harness restored its emulator and verified fixture cleanup.
- Android CLI installed dev build 13 in place, preserving the original installation. Android's installed APK SHA-256 matched `070341d289a69e6139beb3fec5247ef0eb84e7aadea5eb9be494133c6d956dfe` and the launcher opened. The preceding development check verified the saved manual destination and an inactive routing policy. Installed app discovery on the attached phone showed Teams, WhatsApp, Signal and Telegram.
- [PR #133](https://github.com/edithatogo/CallTrack/pull/133) passed the hosted and security requirements and merged. The current Android 17 hosted settings, import, recovery and accessibility suites also passed after [PR #136](https://github.com/edithatogo/CallTrack/actions/runs/45581261845).
- On 26 November 2026, the Android 17 ARM64 import suite passed with a 16 KiB page size and synthetic data. The selected app preview excluded a carrier call and a similarly named app, survived activity recreation, and delivered one event after confirmation. A removed calendar and a replacement with the same row ID each received zero events. Keyboard focus restoration, process death and saved-session resume passed, and the harness verified fixture and package cleanup. The build input fingerprint is `548e31123ba3c6bef850c9a7ef82ba8a94ee68aa715920eabba1634b75418890`.
