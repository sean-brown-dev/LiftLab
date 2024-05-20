package com.browntowndev.liftlab.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.compose.rememberNavController
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab(
    roomBackup: RoomBackup,
    onDonationRequested: (priceInCents: Long, monthly: Boolean) -> Unit,
) {
    LiftLabTheme {
        val navController = rememberNavController()
        val bottomNavBarViewModel: BottomNavBarViewModel = koinViewModel()
        val topAppBarViewModel: TopAppBarViewModel = koinViewModel()
        val liftLabTopAppBarState by topAppBarViewModel.state.collectAsState()
        val bottomNavBarState by bottomNavBarViewModel.state.collectAsState()
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

        LaunchedEffect(key1 = Unit) {
            navController.currentBackStackEntryFlow
                .distinctUntilChanged()
                .collect { backStackEntry ->
                    val route = backStackEntry.destination.route
                    topAppBarViewModel.setScreen(route)
                }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = {
                BottomNavigation(
                    navController = navController,
                    isVisible = bottomNavBarState.isVisible
                )
            },
            topBar = {
                LiftLabTopAppBar(
                    state = liftLabTopAppBarState,
                    scrollBehavior = scrollBehavior,
                )
            }
        ) { scaffoldPaddingValues ->
            NavigationGraph(
                roomBackup = roomBackup,
                navHostController = navController,
                paddingValues = scaffoldPaddingValues,
                screen = liftLabTopAppBarState.currentScreen,
                onNavigateBack = { liftLabTopAppBarState.onNavigationIconClick?.invoke() },
                setTopAppBarCollapsed = { collapsed -> topAppBarViewModel.setCollapsed(collapsed) },
                setTopAppBarControlVisibility = { control, visible ->
                    topAppBarViewModel.setControlVisibility(
                        control,
                        visible
                    )
                },
                mutateTopAppBarControlValue = { request ->
                    request.payload.onLeft {
                        topAppBarViewModel.mutateControlValue(
                            AppBarMutateControlRequest(
                                request.controlName,
                                it
                            )
                        )
                    }.onRight {
                        topAppBarViewModel.mutateControlValue(
                            AppBarMutateControlRequest(
                                request.controlName,
                                it
                            )
                        )
                    }
                },
                setBottomNavBarVisibility =  { bottomNavBarViewModel.setVisibility(it) },
                onDonationRequested = onDonationRequested,
            )
        }
    }
}
