package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun CustomSetExpandableCard(
    isCollapsed: Boolean,
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
    toggleExpansion: () -> Unit,
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    CustomSetContainer(toggleExpansion) {
        Box(
            modifier = Modifier.animateContentSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (isCollapsed) {
                CustomSetSummary(
                    leftSideSummaryText = leftSideSummaryText,
                    centerIconResourceId = centerIconResourceId,
                    rightSideSummaryText = rightSideSummaryText
                )
            } else {
                CustomSetDetails(headerContent = headerContent) {
                    detailContent()
                }
            }
        }
    }
}