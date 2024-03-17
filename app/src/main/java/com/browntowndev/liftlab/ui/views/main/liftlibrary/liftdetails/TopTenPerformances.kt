package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.SectionLabel
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry

fun topTenPerformances(
    lazyListScope: LazyListScope,
    topTenPerformances: List<OneRepMaxEntry>,
) {
    lazyListScope.item {
        SectionLabel(
            modifier = Modifier.padding(bottom = 10.dp),
            text = "TOP TEN ESTIMATED 1RM PERFORMANCES",
            fontSize = 14.sp
        )
    }
    lazyListScope.itemsIndexed(topTenPerformances) { index, performance ->
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (index + 1).toString(),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = performance.setsAndRepsLabel,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp
                )
                Text(
                    text = performance.date,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = performance.oneRepMax,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 18.sp
            )
        }
    }
}