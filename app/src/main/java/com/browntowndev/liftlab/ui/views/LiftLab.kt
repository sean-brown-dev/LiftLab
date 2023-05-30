package com.browntowndev.liftlab.ui.views

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.compose.rememberNavController
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab(topAppBarViewModel: TopAppBarViewModel = getViewModel()) {
    LiftLabTheme {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val topAppBarState by topAppBarViewModel.state.collectAsState()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        LaunchedEffect(key1 = Unit) {
            navController.currentBackStackEntryFlow
                .distinctUntilChanged()
                .onEach { backStackEntry ->
                    val route = backStackEntry.destination.route
                    topAppBarViewModel.setScreen(route)
                }
                .launchIn(scope)
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = { BottomNavigation(navController = navController) },
            topBar = {
                LiftLabTopAppBar(
                    state = topAppBarState,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            NavigationGraph(
                navController = navController,
                paddingValues = paddingValues,
                liftLabTopAppBarState = topAppBarState,
                topAppBarViewModel = topAppBarViewModel
            )
        }
    }
}