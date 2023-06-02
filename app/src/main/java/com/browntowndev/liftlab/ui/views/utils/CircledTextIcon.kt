package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircledTextIcon(text: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
    }
}