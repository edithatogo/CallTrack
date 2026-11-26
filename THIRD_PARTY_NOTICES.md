# Runtime third-party notices — development candidate

The application source retains the GPLv3 licence and upstream attribution in
[LICENSE](LICENSE) and [NOTICE.md](NOTICE.md). Runtime licence texts are included
with the [packaged notices](app/src/main/assets/open_source/).

This inventory accompanies the exact-source candidate bundle; it does not
authorise publication or assert permanent trademark rights. Published source
archives, retained copyright headers and all discovered binary/source licence and
notice documents accompany the bundle. A clean app rebuild uses the pinned
prebuilt dependencies; rebuilding every dependency from its upstream repository
is a separate unqualified scope. Exact runtime binary and published source/POM
hashes are recorded in the runtime source inventory; a matching canonical
binary audit remains a separate release prerequisite. Listenablefuture 1.0
has no direct POM licence declaration; its parent POM and published source
header supply the Apache 2.0 evidence.

Build-only dependency licences are recorded separately in the Track5 tool audit;
those tools are not included as application runtime libraries. Do not bundle SDK,
JDK or Gradle distributions with an app/source release by default.

The active runtime coordinates, licence declarations and retained attribution
are listed in the [packaged third-party notices](app/src/main/assets/open_source/THIRD_PARTY_NOTICES.md).
The [runtime source inventory](qualification/release/runtime-sources.json) binds
the published sources and POMs to their audited runtime binaries.

## Modified project and assets

The fork and original replacement vector artwork use GPLv3; upstream attribution
is retained in NOTICE.md. See [asset provenance](ASSET_PROVENANCE.md) and
[modification notices](MODIFICATIONS.md). Retained notices include earlier
audited origins and the current runtime dependencies. The APK carries the
packaged notices and licence texts under `assets/open_source/`. No original CallTrack APK material, upstream launcher bitmap,
font, screenshot or store artwork is used in the current app.
