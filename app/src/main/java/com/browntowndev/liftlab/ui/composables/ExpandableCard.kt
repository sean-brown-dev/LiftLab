package com.browntowndev.liftlab.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ExpandableCard(
    paddingValues: PaddingValues,
    isCollapsed: Boolean,
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
    toggleExpansion: () -> Unit,
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    ClickableCard(paddingValues = paddingValues, onClick = toggleExpansion) {
        Box(
            modifier = Modifier.animateContentSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (isCollapsed) {
                ExpandableCardSummary(
                    leftSideSummaryText = leftSideSummaryText,
                    centerIconResourceId = centerIconResourceId,
                    rightSideSummaryText = rightSideSummaryText
                )
            } else {
                ExpandableCardDetails(headerContent = headerContent) {
                    detailContent()
                }
            }
        }
    }
}

@Composable
fun ExpandableCard(
    paddingValues: PaddingValues,
    isCollapsed: Boolean,
    summaryText: String,
    toggleExpansion: () -> Unit,
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    ClickableCard(
        paddingValues = paddingValues,
        onClick = toggleExpansion,
        borderColor = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.animateContentSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (isCollapsed) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    text = summaryText,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                ExpandableCardDetails(modifier = Modifier.fillMaxWidth(), headerContent = headerContent) {
                    detailContent()
                }
            }
        }
    }
}

@Composable
private fun ClickableCard(
    paddingValues: PaddingValues,
    onClick: () -> Unit,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .padding(paddingValues = paddingValues)
            .clip(shape = CardDefaults.shape)
            .clickable { onClick() },
        shape = CardDefaults.shape,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
    ) {
        content()
    }
}

@Composable
private fun ExpandableCardSummary(
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = leftSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = centerIconResourceId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rightSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ExpandableCardDetails(
    modifier: Modifier = Modifier,
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            headerContent()
        }
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            detailContent()
        }
    }
}
