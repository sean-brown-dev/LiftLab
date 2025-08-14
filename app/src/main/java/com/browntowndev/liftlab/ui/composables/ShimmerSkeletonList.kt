package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerSkeletonList(
    modifier: Modifier = Modifier,
    cardCount: Int = 5,
    rowCount: Int = 4,
) {
    val shimmerInstance = rememberShimmer(
        shimmerBounds = ShimmerBounds.View
    )

    LazyColumn(
        modifier = modifier.then(Modifier
            .fillMaxSize()
            .shimmer(shimmerInstance))
    ) {
        items(cardCount) {
            ElevatedCard (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp,
                    pressedElevation = 0.dp
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(80.dp))
                    repeat(rowCount) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(vertical = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                )
                        )
                    }
                }
            }
        }
    }
}
