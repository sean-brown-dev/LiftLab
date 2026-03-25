package com.browntowndev.liftlab.ui.composables.keyboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class CustomKeyboardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keyboard_hidden_when_visible_false() {
        compose.setContent {
            Box {
                CustomKeyboard(
                    modifier = Modifier.testTag("keyboard"),
                    visible = false,
                    animationEnabled = false
                ) { /* no-op */ }
            }
        }
        compose.onNodeWithTag("keyboard").assertDoesNotExist()
    }

    @Test
    fun keyboard_shows_immediately_when_animation_off() {
        compose.setContent {
            Box {
                CustomKeyboard(
                    modifier = Modifier.testTag("keyboard"),
                    visible = true,
                    animationEnabled = false
                ) { /* no-op */ }
            }
        }
        compose.onNodeWithTag("keyboard").assertExists()
    }

    @Test
    fun keyboard_appears_with_animation_enabled_too() {
        compose.setContent {
            Box {
                CustomKeyboard(
                    modifier = Modifier.testTag("keyboard"),
                    visible = true,
                    animationEnabled = true
                ) { /* no-op */ }
            }
        }
        compose.onNodeWithTag("keyboard").assertExists()
    }
}
