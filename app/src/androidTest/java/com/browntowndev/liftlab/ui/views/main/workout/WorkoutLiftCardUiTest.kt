package com.browntowndev.liftlab.ui.views.main.workout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class WorkoutLiftCardUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()


    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun card_renders_and_add_set_invokes_callback() {
        val lift = FakeUiData.lift(
            id = 303, name = "Row", position = 0,
            sets = listOf(FakeUiData.standardSet(0, "Set 1"))
        )
        val addSetClicked = AtomicBoolean(false)

        compose.setContent {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { paddingValues ->
                LazyColumn (
                    modifier = Modifier.consumeWindowInsets(paddingValues),
                    contentPadding = paddingValues
                ) {
                    item {
                        WorkoutLiftCard(
                            workoutLift = lift,
                            isEdit = true, // shows Add Set
                            animationEnabled = false,
                            lazyListState = rememberLazyListState(),
                            onUpdatePickerSpacer = { },
                            onShowRpePicker = { _, _, _, _ -> },
                            onHideRpePicker = { },
                            onWeightChanged = { _, _, _, _ -> },
                            onRepsChanged = { _, _, _, _ -> },
                            onSetCompleted = { _, _, _, _, _, _, _, _, _, _, _ -> },
                            onUndoSetCompletion = { _, _, _ -> },
                            onChangeRestTime = { _, _, _ -> },
                            onReplaceLift = { _, _ -> },
                            onNoteChanged = { _, _ -> },
                            onAddSet = {
                                addSetClicked.store(true)
                            }
                        )
                    }
                }
            }
        }

        // Ensure we’re hitting the actual clickable node; scroll if needed.
        val addSetButton = compose.onNode(hasTestTag("add-set-button-${lift.id}") and hasClickAction())
        addSetButton.assertExists()
        addSetButton.assertIsDisplayed()
        addSetButton.performClick()

        // Read on the UI thread to avoid races.
        compose.runOnIdle {
            assert(addSetClicked.load())
        }
    }

    @Test
    fun note_entry_calls_callback_on_ime_done() {
        val lift = FakeUiData.lift(
            id = 101, name = "Incline DB Press", position = 0,
            sets = listOf(FakeUiData.standardSet(0, "Set 1"))
        )
        var notedId: Long? = null
        var notedText: String? = null

        compose.setContent {
            WorkoutLiftCard(
                workoutLift = lift,
                isEdit = false,
                animationEnabled = false,
                lazyListState = rememberLazyListState(),
                onUpdatePickerSpacer = { },
                onShowRpePicker = { _,_,_,_ -> },
                onHideRpePicker = { },
                onWeightChanged = { _,_,_,_ -> },
                onRepsChanged = { _,_,_,_ -> },
                onSetCompleted = { _,_,_,_,_,_,_,_,_,_,_ -> },
                onUndoSetCompletion = { _,_,_ -> },
                onChangeRestTime = { _,_,_ -> },
                onReplaceLift = { _,_ -> },
                onNoteChanged = { id, text -> notedId = id; notedText = text },
                onAddSet = { }
            )
        }

        // Find a text field and enter a note; assume the first editable field inside card is the note field
        val fields = compose.onAllNodes(hasSetTextAction())
        if (fields.fetchSemanticsNodes().isNotEmpty()) {
            fields[0].performTextInput("Elbows tucked felt better")
            fields[0].performImeAction()
        }

        assert(notedId == 101L)
        assert(notedText == "Elbows tucked felt better")
    }
}
