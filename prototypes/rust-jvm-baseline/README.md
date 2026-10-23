# Current JVM source baseline

Standalone Kotlin/JVM project outside root Gradle settings. It compiles the actual
CallIdentity.kt and CallRecord.java from the app via narrowly filtered source sets;
it does not copy or replace those implementations. The main function checks their
four existing golden source signatures before running exploratory host timings.

From the repository root with JDK17:

```sh
JAVA_HOME=/path/to/jdk17 bash ./gradlew -p prototypes/rust-jvm-baseline run --console=plain
```

Kotlin2.3.21/Gradle8.13, JDK17.10000 warmup calls,5 batches of10000 synthetic
signatures; checksum prevents simply discarding the outputs. No call/provider
access or Android dependency is used. JVM compilation does not qualify Android
runtime timing or app acceptance. Future changes to app source intentionally change
this baseline, so compare the recorded input hashes before reusing old results.

Run sequentially with the [Rust trial](../rust-identity/README.md). Neither run is a
statistically controlled benchmark. Cold start, FFI, memory/battery/size, target
packaging and native Rust integration costs are absent. Compare preserving JVM
formatting improvements before attributing the observed gap to language choice.
Remove both experiments if there is no demonstrated future reuse need; production
code and global caches remain intact.
