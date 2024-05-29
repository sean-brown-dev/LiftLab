package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TextFieldDialog(
    header: String,
    subHeader: String = "",
    textAboveTextField: String = "",
    placeholder: String = "",
    initialTextFieldValue: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = initialTextFieldValue, selection = TextRange(start = 0, end = initialTextFieldValue.length))) }

    LiftLabDialog(
        isVisible = true, // Managed by if/else's at caller
        header = header,
        subHeader = subHeader,
        textAboveContent = textAboveTextField,
        onDismiss = onCancel
    ) {
        FocusableRoundTextField(
            textFieldValue = textFieldValue,
            focus = textFieldValue.text.isNotEmpty(),
            placeholder = placeholder,
            onTextFieldValueChange = {
                textFieldValue = it
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 25.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                modifier = Modifier
                    .padding(0.dp, 0.dp, 15.dp, 0.dp),
                onClick = onCancel
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = { onConfirm(textFieldValue.text) }) {
                Text(text = "OK")
            }
        }
    }
}