package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.views.utils.LabeledChips


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabBottomSheet(
    visible: Boolean,
    sheetPeekHeight: Dp,
    bottomSpacerHeight: Dp,
    label: String,
    volumeTypes: List<CharSequence> = listOf(),
    content:  @Composable (PaddingValues) -> Unit = {},
) {
    var scaffoldState by remember {
        mutableStateOf(
            BottomSheetScaffoldState(
                bottomSheetState = SheetState(
                    initialValue = SheetValue.PartiallyExpanded,
                    skipPartiallyExpanded = false,
                    skipHiddenState = visible
                ),
                snackbarHostState = SnackbarHostState()
            )
        )
    }

    LaunchedEffect(key1 = visible) {
        scaffoldState = BottomSheetScaffoldState(
            bottomSheetState = SheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipPartiallyExpanded = false,
                skipHiddenState = visible
            ),
            snackbarHostState = SnackbarHostState()
        )

        if (!visible) {
            scaffoldState.bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.surface,
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        content = content,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(percent = 50)
                    )
                    .size(width = 40.dp, height = 5.dp),
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(
                        top = 10.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 25.dp
                    ),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(10.dp))
                LabeledChips(
                    labels = volumeTypes,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                )
                Spacer(modifier = Modifier.height(bottomSpacerHeight))
            }
        }
    )
}
