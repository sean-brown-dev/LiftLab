package com.browntowndev.liftlab.ui.composables.keyboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RpeKeyboardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rpeKeyboard_visible_selects_value() {
        var picked: Float? = null

        compose.setContent {
            Box {
                RpeKeyboard(
                    modifier = Modifier.testTag("rpe-keyboard"),
                    visible = true,
                    animationEnabled = false,
                    selectedRpe = null,
                    onRpeSelected = { picked = it }
                )
            }
        }

        // Tap a value that exists in your grid (6.0..10.0 in 0.5 steps)
        compose.onNodeWithText("8.5", substring = false).performClick()
        assertEquals(8.5f, picked)
    }
}
