package com.browntowndev.liftlab.ui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Stable
class TopAppBarState(
    private val navController: NavController,
    private val scope: CoroutineScope,
) {
    init {
        navController.currentBackStackEntryFlow
            .distinctUntilChanged()
            .onEach { backStackEntry ->
                val route = backStackEntry.destination.route
                currentScreen = getScreen(route)
            }
            .launchIn(scope)
    }

    var currentScreen by mutableStateOf<Screen?>(null)
        private set

    val isVisible: Boolean
        @Composable get() = currentScreen?.isAppBarVisible == true

    val navigationIcon: ImageVector?
        @Composable get() = currentScreen?.navigationIcon

    val navigationIconVisible: Boolean?
        @Composable get() = currentScreen?.navigationIconVisible

    val navigationIconContentDescription: String?
        @Composable get() = currentScreen?.navigationIconContentDescription

    val onNavigationIconClick: (() -> Unit)?
        @Composable get() = currentScreen?.onNavigationIconClick

    val title: String
        @Composable get() = currentScreen?.title.orEmpty()

    val actions: MutableList<ActionMenuItem>
        @Composable get() = currentScreen?.actions ?: mutableListOf()

    private fun getScreen(route: String?): Screen? = when (route) {
        LiftLibraryScreen.navigation.route -> LiftLibraryScreen()
        WorkoutScreen.navigation.route -> WorkoutScreen()
        LabScreen.navigation.route -> LabScreen()
        WorkoutHistoryScreen.navigation.route -> WorkoutHistoryScreen()
        else -> null
    }
}