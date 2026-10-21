package com.markobl.calllogsync.slice

/** Counts span all destinations; verified is historical readback, not current existence. */
class DeliveryCounts(
    @JvmField val pending: Long,
    @JvmField val verified: Long,
    @JvmField val review: Long,
    @JvmField val suppressed: Long,
)
