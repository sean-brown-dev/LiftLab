package com.browntowndev.liftlab.ui.views.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IntegerOnlyTextField(
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    maxValue: Int = 99,
    minValue: Int = 0,
    value: Int,
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.outline,
    labelFontSize: TextUnit = 10.sp,
    useRpePicker: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onValueChanged: (Int) -> Unit
) {
    var text by remember { mutableStateOf(value.toString()) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val integerOnlyTextField: @Composable (width: Dp?) -> Unit = { width ->
        val txtMod = if (width != null) Modifier
            .height(37.dp)
            .width(width)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
        else Modifier
            .height(37.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }

        LiftLabOutlinedTextField(
            modifier = txtMod,
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 0.dp),
            value = text,
            isError = text.isEmpty(),

            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = if(useRpePicker) ImeAction.Done else ImeAction.None
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
            }),
            onValueChange = { newValue ->
                var newValueAsInt = newValue.trim().toIntOrNull()
                if (newValueAsInt != null) {
                    if (newValueAsInt <= maxValue) {
                        newValueAsInt = if (newValueAsInt >= minValue) newValueAsInt else minValue
                        text = newValueAsInt.toString()
                        onValueChanged(newValueAsInt)
                    }
                } else {
                    text = newValue
                }
            },
        )
    }

    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    maxLines = 2,
                    fontSize = labelFontSize,
                    color = labelColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = modifier.height(5.dp))
            if (useRpePicker) {
                CompositionLocalProvider(
                    LocalTextInputService provides null
                ) {
                    integerOnlyTextField(null)
                }
            } else {
                integerOnlyTextField(null)
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    maxLines = 1,
                    fontSize = labelFontSize,
                    color = labelColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = modifier.weight(1f))
            Box(modifier = Modifier.padding(vertical = 2.dp, horizontal = 10.dp)) {
                if (useRpePicker) {
                    CompositionLocalProvider(
                        LocalTextInputService provides null
                    ) {
                        integerOnlyTextField(60.dp)
                    }
                } else {
                    integerOnlyTextField(60.dp)
                }
            }
        }
    }

    LaunchedEffect(value) {
        text = value.toString()
    }

    BackHandler(isFocused) {
        focusManager.clearFocus()
    }
}
