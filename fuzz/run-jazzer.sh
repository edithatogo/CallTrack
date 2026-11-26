#!/bin/sh
set -eu

: "${JAVA_HOME:?Set JAVA_HOME to the qualified JDK 25}"
: "${JAZZER_HOME:?Set JAZZER_HOME to the verified Jazzer distribution}"
: "${FUZZ_OUTPUT_DIR:?Set FUZZ_OUTPUT_DIR to a private output directory}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the qualified Android SDK}"

repo_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_dir"
sh gradlew --no-daemon --console=plain :app:compileDebugKotlin

kotlin_version=$(sed -n 's/^kotlin[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' gradle/libs.versions.toml)
test -n "$kotlin_version"
gradle_home=${GRADLE_USER_HOME:-"$HOME/.gradle"}
stdlib=$(find "$gradle_home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/$kotlin_version" \
    -type f -name "kotlin-stdlib-$kotlin_version.jar" -print -quit)
test -n "$stdlib" && test -f "$stdlib"

mkdir -p "$FUZZ_OUTPUT_DIR"
work_dir=$(mktemp -d "$FUZZ_OUTPUT_DIR/run.XXXXXXXX")
mkdir "$work_dir/classes" "$work_dir/corpus"
cp fuzz/corpus/seed "$work_dir/corpus/seed"
app_classes="$repo_dir/app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
test -f "$app_classes/com/markobl/calllogsync/slice/CallEvent.class"
"$JAVA_HOME/bin/javac" -cp "$JAZZER_HOME/jazzer_standalone.jar:$app_classes:$stdlib" \
    -d "$work_dir/classes" fuzz/CallDataFuzzer.java
"$JAZZER_HOME/jazzer" \
    "--cp=$app_classes:$work_dir/classes:$stdlib" \
    --target_class=CallDataFuzzer \
    '--instrumentation_includes=com.markobl.calllogsync.slice.**:CallDataFuzzer' \
    "--reproducer_path=$work_dir" -max_total_time=30 -timeout=10 -rss_limit_mb=2048 \
    "-artifact_prefix=$work_dir/" "$work_dir/corpus"
