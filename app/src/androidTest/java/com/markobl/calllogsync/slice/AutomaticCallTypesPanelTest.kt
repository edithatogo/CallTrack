package com.markobl.calllogsync.slice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AutomaticCallTypesPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun optionsReflectStateAndEmitOnlyExplicitChanges() {
        var state by mutableStateOf(AutomaticCallTypesUiState())
        var changes = 0
        compose.setContent {
            AutomaticCallTypesPanel(
                state = state,
                incomingLabel = "Incoming calls",
                outgoingLabel = "Outgoing calls",
                missedLabel = "Missed calls",
                enabled = true,
                onStateChange = {
                    state = it
                    changes++
                },
            )
        }

        compose.runOnIdle {
            assertEquals(0, changes)
            assertTrue(state.incoming && state.outgoing && state.missed)
        }
        compose.onNodeWithTag("auto_incoming").assertIsDisplayed().assertIsOn()
        compose.onNodeWithTag("auto_outgoing").performClick()
        compose.runOnIdle {
            assertEquals(1, changes)
            assertTrue(state.incoming && !state.outgoing && state.missed)
        }
    }

    @Test
    fun optionsRemainAccessibleInRtlWithLargeTextAndRespectDisabledState() {
        compose.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density = 1f, fontScale = 2f),
            ) {
                AutomaticCallTypesPanel(
                    state = AutomaticCallTypesUiState(),
                    incomingLabel = "Incoming calls",
                    outgoingLabel = "Outgoing calls",
                    missedLabel = "Missed calls",
                    enabled = false,
                    onStateChange = {},
                )
            }
        }

        val outgoing = compose.onNodeWithTag("auto_outgoing")
        outgoing.assertIsDisplayed().assertIsOn().assertIsNotEnabled()
        val semantics = outgoing.fetchSemanticsNode()
        assertEquals(Role.Checkbox, semantics.config[SemanticsProperties.Role])
        assertTrue(semantics.boundsInRoot.height >= 48f)
    }
}
