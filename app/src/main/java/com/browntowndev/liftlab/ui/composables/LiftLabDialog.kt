package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
public fun LiftLabDialog(
    isVisible: Boolean,
    header: String,
    subHeader: String = "",
    textAboveContent: String = "",
    textAboveContentFontSize: TextUnit = 14.sp,
    textAboveContentAlignment: TextAlign = TextAlign.Center,
    textAboveContentPadding: PaddingValues = PaddingValues(bottom = 20.dp),
    onDismiss: () -> Unit,
    content: @Composable (() -> Unit),
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column (
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = header,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (subHeader.isNotEmpty()) {
                        Text(
                            text = subHeader,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 5.dp, bottom = 20.dp).height(1.dp),
                        color = MaterialTheme.colorScheme.outline,
                    )

                    if (textAboveContent.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(textAboveContentPadding),
                            text = textAboveContent,
                            textAlign = textAboveContentAlignment,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = textAboveContentFontSize,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    content()
                }
            }
        }
    }
}