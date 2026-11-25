# Android 17 engineering standards

Compile and target API 37. Use the qualified SDK minor level required by the Compose dependency graph. Fail on compiler and deprecated API warnings. Use Android Activity Result contracts, WorkManager, and the platform-backed predictive back APIs. Features requiring credentials, federated sign-in or photo selection use Credential Manager and Photo Picker. Keep app cryptographic material in Android Keystore when required.

Replace active preference storage with typed Proto DataStore. Preserve the source namespace, automatic policy and route bindings, selected calendar, worker status and existing Room recovery databases during upgrade. Use Android's supported one-time migration. Keep UI work and jobs in structured coroutine scopes with Flow.

Render the app in Compose Material 3 Expressive with edge-to-edge insets and predictive back. Use Navigation 3 with a user-owned stack and adaptive scenes across phone, foldable, tablet and desktop windows. Keep interactive nodes labelled, focusable and at least 48 dp. Use stable models, keyed lazy lists and derived state when it prevents avoidable recomposition.

Expose one immutable sealed screen state through StateFlow with a five-second while-subscribed window. Keep the delivery domain platform-independent. Qualify Metro or Hilt for compile-time dependency injection. Retain explicit permissions, bounded provider queries, minimal exported components and redacted evidence.

Qualify `./gradlew ktlintCheck detekt`, `./gradlew lintDebug` and `./gradlew testDebugUnitTest`. Use deterministic coroutine test dispatch and Flow assertions, rendered accessibility checks and screenshots across window sizes, and the existing exact-source process recovery, migration and security gates.

[AndroidX releases](https://developer.android.com/jetpack/androidx/versions) · [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) · [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
