# Android 17 call routing plan

- [x] Map native source and destination APIs and preserve the existing release scope.
- [x] Exercise source matching, preserved activation, and independent recovery. Implement opt-in routing in the existing automatic settings screen.
- [~] Run source-matched host and API 37 device acceptance, including rendered controls, migration and process recovery.
- [x] Verify an in-place upgrade of the authorised dev package on the attached phone.
- [ ] Verify a controlled call from each selected third-party app and a readback from each nominated calendar before claiming that physical route.

Historical import remains bound to its saved phone-call snapshot. An app-call backfill needs an explicit source selection that persists with each confirmed import so recovery can replay the same scope.

## Development checkpoint

- The API 37 ARM64 provider fixture read attributed app calls through Android's unified log and ran WorkManager against the native calendar provider. One approved app call produced one event in each of two calendars across repeated runs. A deleted calendar and its replacement with the same numeric ID received zero events. The test removed its synthetic data and APKs.
- Android CLI installed the exact tested dev APK in place. Version code 12 retained the original installation date and saved manual destination. Android's installed APK SHA-256 matched `46c57ca8ef03506e1efe2b752c740e408fa7c87c7ec4a91dce9310292429afd9`. The launcher opened and automatic routing was inactive. Installed app discovery on the attached phone showed Teams, WhatsApp, Signal and Telegram.
