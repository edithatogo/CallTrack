package org.calltrack.composeprototype

/** Synthetic screen policy only; no provider, permission or synchronization operations. */
data class PreviewState(
    val calendarAccess: Boolean = false,
    val calendars: List<Long> = listOf(7, 9),
    val selected: Long = -1,
    val busy: Boolean = false,
    val refreshing: Boolean = false,
    val requests: Int = 0,
) {
    init {
        require(calendars.all { it >= 0 } && calendars.distinct().size == calendars.size)
    }

    val controlsEnabled: Boolean
        get() = calendarAccess && !busy && !refreshing

    val canLog: Boolean
        get() = controlsEnabled && selected in calendars

    fun replaceCalendars(ids: List<Long>): PreviewState =
        copy(calendars = ids, selected = selected.takeIf { it in ids } ?: -1)
}
