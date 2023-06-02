package com.browntowndev.liftlab.ui.views.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.browntowndev.liftlab.core.data.dtos.LiftDto
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLibraryScreen
import com.browntowndev.liftlab.ui.views.utils.CircledTextIcon
import org.koin.androidx.compose.getViewModel


@Composable
fun LiftLibrary(
    paddingValues: PaddingValues,
    topAppBarState: LiftLabTopAppBarState,
    topAppBarViewModel: TopAppBarViewModel,
    liftLibraryViewModel: LiftLibraryViewModel = getViewModel(),
) {
    val state by liftLibraryViewModel.state.collectAsState()
    val screen = topAppBarState.currentScreen as? LiftLibraryScreen

    LaunchedEffect(key1 = screen) {
        liftLibraryViewModel.watchActionBarActions(screen, topAppBarViewModel)
    }

    BackHandler(topAppBarState.navigationIconVisible == true) {
        topAppBarState.onNavigationIconClick?.invoke();
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.TopStart)
            .padding(paddingValues)
    ) {
        items(state.lifts) {lift ->
            LiftListItem(lift = lift)
        }
    }
}

@Composable
fun LiftListItem(lift: LiftDto) {
    ListItem(
        headlineContent = { Text(lift.name) },
        supportingContent = { Text(lift.categoryDisplayName) },
        leadingContent = { CircledTextIcon(text = lift.name[0].toString()) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            headlineColor = MaterialTheme.colorScheme.onBackground,
            supportingColor = MaterialTheme.colorScheme.onBackground,
            leadingIconColor = MaterialTheme.colorScheme.onBackground
        )
    )
}