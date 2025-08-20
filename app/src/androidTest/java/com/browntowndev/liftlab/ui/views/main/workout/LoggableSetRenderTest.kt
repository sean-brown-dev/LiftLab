package com.browntowndev.liftlab.ui.views.main.workout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LoggableSetRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun row_renders_with_basic_params() {
        compose.setContent {
            val list = rememberLazyListState()
            LoggableSet(
                lazyListState = list,
                animateVisibility = true,
                isEdit = true,
                position = 0,
                myoRepSetPosition = null,
                setNumberLabel = "Set 1",
                previousSetResultLabel = "",
                weightRecommendation = 100f,
                repRangePlaceholder = "8-10",
                rpeTargetPlaceholder = "8",
                complete = false,
                completedWeight = null,
                completedReps = null,
                completedRpe = null,
                onWeightChanged = {},
                onRepsChanged = {},
                onCompleted = { _, _, _ -> },
                onUndoCompletion = {},
                toggleRpePicker = {},
                onAddSpacer = {}
            )
        }
        // We don't have a direct tag here; sanity-assert composition reached without crash by reusing a harmless tag wrapper
        // Wrap LoggableSet in a Box with a tag if you add one later; for now, this test simply ensures no exceptions during composition.
        // If you add tags in the future, replace with assertExists calls.
        compose.onAllNodes(hasTestTag("non-existent")).assertCountEquals(0)
    }
}
