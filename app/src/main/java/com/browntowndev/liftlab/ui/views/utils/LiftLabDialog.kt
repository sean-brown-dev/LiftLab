package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
private fun <T> LiftLabDialog(
    header: String,
    subtext: String = "",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable (() -> Unit),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Dialog(
            onDismissRequest = onCancel
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column (
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = header,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Divider(modifier = Modifier.height(2.dp))
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = subtext,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Divider(thickness = 20.dp, color = MaterialTheme.colorScheme.background)
                    content()
                    Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 15.dp, 0.dp)
                                .clickable { onCancel() },
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(onClick = onConfirm ) {
                            Text(text = "OK")
                        }
                    }
                }
            }
        }
    }
}