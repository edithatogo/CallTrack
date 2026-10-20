package org.calltrack.composeprototype

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals

/** Semantics actions alone can pass behind a blocking system window. */
fun requirePrototypeWindow() {
    val root =
        checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
    assertEquals(
        "The prototype must be the active accessible window",
        "org.calltrack.community.composeprototype",
        root.packageName.toString(),
    )
}
