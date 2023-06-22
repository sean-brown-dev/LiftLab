package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


@Composable
fun FocusedRoundTextField(
    value: String = "",
    textFieldValue: TextFieldValue? = null,
    onValueChange: (String) -> Unit = { },
    onTextFieldValueChange: (TextFieldValue) -> Unit = { },
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    var valueInnitted by remember { mutableStateOf(false) }

    LaunchedEffect(valueInnitted) {
        focusRequester.requestFocus()
    }

    if (textFieldValue == null) {
        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            singleLine = true,
            value = value,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            onValueChange = {
                valueInnitted = true
                onValueChange(it)
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(45.dp)
        )
    }
    else {
        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            singleLine = true,
            value = textFieldValue,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            onValueChange = {
                valueInnitted = true
                onTextFieldValueChange(it)
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(45.dp)
        )
    }
}