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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.views.utils.CircledTextIcon
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import org.koin.androidx.compose.getViewModel


@Composable
fun LiftLibrary(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    isSearchBarVisible: Boolean,
    onNavigateBack: () -> Unit,
) {
    val liftLibraryViewModel: LiftLibraryViewModel = getViewModel()
    val state by liftLibraryViewModel.state.collectAsState()

    liftLibraryViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = liftLibraryViewModel)

    BackHandler(isSearchBarVisible) {
        onNavigateBack.invoke()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.TopStart)
            .padding(paddingValues)
    ) {
        items(state.lifts) {lift ->
            ListItem(
                headlineContent = { Text(lift.name) },
                supportingContent = { Text(lift.movementPatternDisplayName) },
                leadingContent = { CircledTextIcon(text = lift.name[0].toString()) },
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