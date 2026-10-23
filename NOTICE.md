# Source and qualification provenance

This repository's development direction is a GPL-3.0 fork of
[MarkoBL/AndroidCallLogSync](https://github.com/MarkoBL/AndroidCallLogSync), audited at
`aaa56443a294aa61bad48b620427e53f9521c5fe`.

The upstream source is preserved in an excluded reference checkout and adopted into
the root app workspace for Track 3. The new manual slice replaces the launcher,
disables legacy boot entry, removes network permission and adds local-provider
source/mapping/calendar/ledger adapters and tests. Preparation scripts
apply documented build-only changes to a separate copy and add our qualification
tests. The upstream GPL notice is preserved in both source copies. LICENSE contains
the GPL version 3 text. No original CallTrack APK implementation is incorporated.

Existing CallTrack APKs and extracted implementation remain private evidence. A
source licence for that original application has not been established.

Development APKs use a disposable qualification identity and local debug signing.
They are not public releases or replacements for the original developer's app.
