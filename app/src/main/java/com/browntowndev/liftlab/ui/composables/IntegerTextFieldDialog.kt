package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntegerTextFieldDialog(
    header: String,
    subHeader: String = "",
    textAboveTextField: String = "",
    placeholder: String = "",
    initialTextFieldValue: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    var text by remember(initialTextFieldValue) { mutableStateOf<Int?>(initialTextFieldValue) }
    var textAsInt by remember(initialTextFieldValue) { mutableIntStateOf(initialTextFieldValue) }

    LiftLabDialog(
        isVisible = true, // Handled by if/else at caller
        header = header,
        subHeader = subHeader,
        textAboveContent = textAboveTextField,
        onDismiss = onCancel
    ) {
        IntegerTextField(
            modifier = Modifier.width(100.dp),
            placeholder = placeholder,
            value = text,
            onValueChanged = {
                text = it
                textAsInt = it ?: 1
            }
        )
        if (extraContent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            extraContent()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Button(onClick = { onConfirm(textAsInt) }) {
                Text(text = "OK")
            }
        }
    }
}