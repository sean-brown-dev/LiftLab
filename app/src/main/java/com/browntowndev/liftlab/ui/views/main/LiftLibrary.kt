package com.browntowndev.liftlab.ui.views.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.data.dtos.LiftDTO
import com.browntowndev.liftlab.ui.models.LiftLibraryScreen
import com.browntowndev.liftlab.ui.models.TopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import org.koin.androidx.compose.getViewModel


@Composable
fun LiftLibrary(paddingValues: PaddingValues, appBarState: MutableState<TopAppBarState>) {
    val liftLibraryViewModel: LiftLibraryViewModel = getViewModel()
    val allLifts = remember { mutableStateListOf<LiftDTO>() }
    val screen = appBarState.value.currentScreen as? LiftLibraryScreen;
    var filterText by remember { screen?.filterText ?: mutableStateOf("") }

    LaunchedEffect(Unit) {
        var liftEntities = liftLibraryViewModel.getAllLifts()
        allLifts.addAll(liftEntities)
    }

    BackHandler(appBarState.value.navigationIconVisible == true) {
        screen?.onNavigationIconClick?.invoke();
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.TopStart)
            .padding(paddingValues)
    ) {
        val filteredLifts = allLifts.filter {it.lift.name.contains(filterText, true)}.toList()
        items(filteredLifts.toList()) {lift ->
            ListItem(
                headlineContent = { Text(lift.lift.name) },
                supportingContent = { Text(lift.lift.category.displayName()) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = lift.lift.name[0].toString(),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    headlineColor = MaterialTheme.colorScheme.onBackground,
                    supportingColor = MaterialTheme.colorScheme.onBackground,
                    leadingIconColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}