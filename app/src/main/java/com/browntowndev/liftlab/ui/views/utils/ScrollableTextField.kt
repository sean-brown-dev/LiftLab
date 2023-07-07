package com.browntowndev.liftlab.ui.views.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScrollableTextField(
    modifier: Modifier = Modifier,
    placeholder: String = "",
    listState: LazyListState? = null,
    vertical: Boolean = true,
    disableSystemKeyboard: Boolean = true,
    value: String,
    label: String = "",
    errorOnEmptyString: Boolean = true,
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    onFocusChanged: (Boolean) -> Unit,
    onValueChanged: ((String) -> String)? = null,
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    var text by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenDensity = LocalDensity.current
    val pickerHeightInPixels = with(screenDensity) {
        (screenHeight * .30f).toPx()
    }
    val screenInPixels = with(screenDensity) {
        screenHeight.toPx()
    }
    var topOfTextFieldPosition by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        text = value
    }

    val customKeyboardTextField: @Composable (width: Dp?) -> Unit = { width ->
        val txtMod = (if (width != null) Modifier.width(width) else Modifier)
            .height(50.dp)
            .onGloballyPositioned { coordinates ->
                topOfTextFieldPosition = coordinates.positionInRoot().y
            }
            .onFocusChanged {
                isFocused = it.isFocused

                if (isFocused && disableSystemKeyboard && listState != null) {
                    coroutineScope.launch {
                        scrollToYLocation(
                            listState = listState,
                            screenInPixels = screenInPixels,
                            pickerHeightInPixels = pickerHeightInPixels,
                            screenDensity = screenDensity,
                            currentYLocation = { topOfTextFieldPosition },
                            positionBuffer = 60f,
                            onPixelOverflowChanged = onPixelOverflowChanged,
                        )
                        delay(50)
                        onFocusChanged(isFocused)
                    }
                } else {
                    onPixelOverflowChanged(0.dp)
                    onFocusChanged(isFocused)
                }
            }

        val customTextSelectionColors = TextSelectionColors(
            handleColor = if (disableSystemKeyboard) Color.Transparent else MaterialTheme.colorScheme.primary,
            backgroundColor = if (disableSystemKeyboard) Color.Transparent else MaterialTheme.colorScheme.primary,
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors){
            OutlinedTextField(
                modifier = txtMod,
                value = text,
                isError = errorOnEmptyString && text.isEmpty(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 16.sp,
                        )
                    }
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                    errorContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    cursorColor = if (disableSystemKeyboard) Color.Transparent else MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    unfocusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                onValueChange = { newValue ->
                    text = if (onValueChanged != null) {
                        onValueChanged(newValue)
                    } else {
                        newValue
                    }
                },
            )
        }
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

            if (disableSystemKeyboard) {
                CompositionLocalProvider(
                    LocalTextInputService provides null
                ) {
                    customKeyboardTextField(null)
                }
            } else {
                customKeyboardTextField(null)
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
                if(disableSystemKeyboard) {
                    CompositionLocalProvider(
                        LocalTextInputService provides null
                    ) {
                        customKeyboardTextField(100.dp)
                    }
                } else {
                    customKeyboardTextField(100.dp)
                }
            }
        }
    }

    BackHandler(isFocused) {
        focusManager.clearFocus()
    }
}
