package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionLabel(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.then(Modifier.padding(start = 10.dp)),
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}