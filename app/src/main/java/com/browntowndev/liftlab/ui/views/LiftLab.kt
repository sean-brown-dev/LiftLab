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
import androidx.navigation.toRoute
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.DonationState
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph
import com.browntowndev.liftlab.ui.views.navigation.Route
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab(
    donationState: DonationState,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
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
                    val route = when(backStackEntry.destination.route?.split("?", "/")?.get(0)) {
                        Route.Home::class.qualifiedName -> backStackEntry.toRoute<Route.Home>()
                        Route.Workout::class.qualifiedName -> backStackEntry.toRoute<Route.Workout>()
                        Route.WorkoutHistory::class.qualifiedName -> backStackEntry.toRoute<Route.WorkoutHistory>()
                        Route.EditWorkout::class.qualifiedName -> backStackEntry.toRoute<Route.EditWorkout>()
                        Route.Lab::class.qualifiedName -> backStackEntry.toRoute<Route.Lab>()
                        Route.WorkoutBuilder::class.qualifiedName -> backStackEntry.toRoute<Route.WorkoutBuilder>()
                        Route.LiftLibrary::class.qualifiedName -> backStackEntry.toRoute<Route.LiftLibrary>()
                        Route.LiftDetails::class.qualifiedName -> backStackEntry.toRoute<Route.LiftDetails>()
                        Route.Settings::class.qualifiedName -> backStackEntry.toRoute<Route.Settings>()
                        else -> null
                    }

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
                navHostController = navController,
                paddingValues = scaffoldPaddingValues,
                screen = liftLabTopAppBarState.currentScreen,
                donationState = donationState,
                onClearBillingError = onClearBillingError,
                onUpdateDonationProduct = onUpdateDonationProduct,
                onProcessDonation = onProcessDonation,
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
                onBackup = onBackup,
                onRestore = onRestore,
            )
        }
    }
}
