#!/bin/sh
set -eu
umask 077

repo_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_dir"
version=2.0.0
case "$(uname -s)/$(uname -m)" in
    Linux/x86_64)
        asset=opengrep_linux_x86
        checksum=37e93f4da5fd04a18c5c1febbdd12f7e98dfccd4d0c9cf2593bcb25eeb462da0
        ;;
    Darwin/arm64)
        asset=opengrep_osx_arm64
        checksum=b51b3d83f165a2e0934497b8f1b08abb1c3041ef20ef911c97e04cb39af1ad55
        ;;
    *)
        printf '%s\n' 'OpenGrep is qualified for Linux x64 and Apple Silicon.' >&2
        exit 1
        ;;
esac

verify_binary() {
    if [ "$asset" = opengrep_osx_arm64 ]; then
        actual=$(shasum -a 256 "$1")
    else
        actual=$(sha256sum "$1")
    fi
    [ "${actual%% *}" = "$checksum" ]
}

tools_dir="$repo_dir/.local/security-tools/opengrep-v$version"
mkdir -p "$tools_dir"
run_dir=$(mktemp -d "$tools_dir/run.XXXXXXXX")
trap 'rm -f -- "$run_dir/download" "$run_dir/scan.json" "$run_dir/scan.log"; rmdir -- "$run_dir"' 0
trap 'exit 1' HUP INT TERM
binary="$tools_dir/$asset"

if [ ! -f "$binary" ]; then
    curl --fail --location --silent --show-error --proto '=https' --proto-redir '=https' --tlsv1.2 \
        "https://github.com/opengrep/opengrep/releases/download/v$version/$asset" -o "$run_dir/download"
    verify_binary "$run_dir/download"
    chmod u+x "$run_dir/download"
    mv -n -- "$run_dir/download" "$binary"
fi
if ! verify_binary "$binary"; then
    printf '%s\n' 'OpenGrep binary checksum did not match the pinned release.' >&2
    exit 1
fi
test "$("$binary" --version)" = "$version"

scan_result=0
"$binary" scan --config config/opengrep.yml --error --strict --disable-nosem --no-git-ignore \
    --json app/src/main scripts > "$run_dir/scan.json" 2> "$run_dir/scan.log" || scan_result=$?
if ! jq -e '(.paths.scanned | length) > 0' "$run_dir/scan.json" >/dev/null 2>&1; then
    printf 'OpenGrep produced no usable report with scanned sources (exit %s).\n' "$scan_result" >&2
    exit 1
fi
jq -c '{scanned_files: (.paths.scanned | length),
        findings: [.results[] | {rule: .check_id, path, line: .start.line}],
        errors: [.errors[].type | if type == "array" then .[0] else . end]}' "$run_dir/scan.json"
exit "$scan_result"
