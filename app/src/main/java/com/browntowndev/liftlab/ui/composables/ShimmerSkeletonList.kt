package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(16.dp)
            .shimmer(shimmerInstance))
    ) {
        items(cardCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    repeat(rowCount) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(vertical = 6.dp)
                                .background(
                                    color = Color.LightGray, // Or any neutral loading color
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
