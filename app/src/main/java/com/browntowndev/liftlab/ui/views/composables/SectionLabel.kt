package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionLabel(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 10.sp,
) {
    Text(
        modifier = modifier.then(Modifier.padding(start = 10.dp)),
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
    )
}