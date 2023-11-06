package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.browntowndev.liftlab.core.common.INCREMENT_OPTIONS
import com.browntowndev.liftlab.ui.views.composables.NumberPickerMenuItem

@Composable
fun IncrementPicker(
    increment: Float,
    onBackPressed: () -> Unit,
    onChangeIncrement: (newIncrement: Float) -> Unit,
) {
    Column {
        NumberPickerMenuItem(
            initialValue = increment,
            label = "Weight Increment",
            options = INCREMENT_OPTIONS,
            onChanged = {
                onChangeIncrement(it)
            },
            onBackPressed = onBackPressed,
        )
    }
}