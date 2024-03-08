package com.browntowndev.liftlab.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


@Composable
fun FocusableRoundTextField(
    modifier: Modifier = Modifier,
    value: String = "",
    textFieldValue: TextFieldValue? = null,
    supportingText: @Composable() (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(45.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
        unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    placeholder: String = "",
    focus: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    onValueChange: (String) -> Unit = { },
    onTextFieldValueChange: (TextFieldValue) -> Unit = { },
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var text by remember(value) { mutableStateOf(value) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var valueInnitted by remember { mutableStateOf(false) }

    if(focus) {
        LaunchedEffect(valueInnitted) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isFocused) {
        focusManager.clearFocus()
    }

    if (textFieldValue == null) {
        OutlinedTextField(
            modifier = modifier.then(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
            ),
            textStyle = textStyle,
            supportingText = supportingText,
            colors = colors,
            singleLine = true,
            value = text,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = { Text(text = placeholder, color = MaterialTheme.colorScheme.outline) },
            onValueChange = {
                valueInnitted = true
                text = it
                onValueChange(it)
            },
            shape = shape,
        )
    }
    else {
        OutlinedTextField(
            modifier = modifier.then(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
            ),
            textStyle = textStyle,
            colors = colors,
            supportingText = supportingText,
            singleLine = true,
            value = textFieldValue,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = { Text(text = placeholder, color = MaterialTheme.colorScheme.outline) },
            onValueChange = {
                valueInnitted = true
                onTextFieldValueChange(it)
            },
            shape = shape,
        )
    }
}