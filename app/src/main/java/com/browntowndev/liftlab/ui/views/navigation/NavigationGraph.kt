package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import arrow.core.Either
import arrow.core.left
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.core.common.LIFT_METRIC_CHART_IDS
import com.browntowndev.liftlab.core.common.SHOW_WORKOUT_LOG
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.DonationState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.EditWorkoutScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.SettingsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
import com.browntowndev.liftlab.ui.views.main.home.Home
import com.browntowndev.liftlab.ui.views.main.home.Settings
import com.browntowndev.liftlab.ui.views.main.lab.Lab
import com.browntowndev.liftlab.ui.views.main.liftlibrary.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.LiftDetails
import com.browntowndev.liftlab.ui.views.main.workout.EditWorkout
import com.browntowndev.liftlab.ui.views.main.workout.Workout
import com.browntowndev.liftlab.ui.views.main.workout.WorkoutHistory
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.WorkoutBuilder


@ExperimentalFoundationApi
@Composable
fun NavigationGraph(
    navHostController: NavHostController,
    paddingValues: PaddingValues,
    donationState: DonationState,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSetScreen: (screen: Screen) -> Unit,
) {
    NavHost(navHostController, startDestination = Route.Workout()) {
        composable<Route.Home> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(HomeScreen())
                    setBottomNavBarVisibility(true)
                }

                Home(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onNavigateToSettingsMenu = { navHostController.navigate(Route.Settings) },
                    onNavigateToLiftLibrary = { chartIds ->
                        navHostController.currentBackStackEntry!!.savedStateHandle[LIFT_METRIC_CHART_IDS] = chartIds
                        navHostController.navigate(Route.LiftLibrary())
                    },
                )
            }
        }

        composable<Route.Settings> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(SettingsScreen())
                    setBottomNavBarVisibility(false)
                }

                Settings(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    initialized = donationState.initialized,
                    isProcessingDonation = donationState.isProcessingDonation,
                    activeSubscription = donationState.activeSubscription,
                    newDonationSelection = donationState.newDonationSelection,
                    subscriptionProducts = donationState.subscriptionProducts,
                    oneTimeDonationProducts = donationState.oneTimeDonationProducts,
                    billingCompletionMessage = donationState.billingCompletionMessage,
                    onClearBillingError = onClearBillingError,
                    onUpdateDonationProduct = onUpdateDonationProduct,
                    onProcessDonation = onProcessDonation,
                    onNavigateBack = { navHostController.popBackStack() },
                    onBackup = onBackup,
                    onRestore = onRestore,
                )
            }
        }

        composable<Route.LiftLibrary> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                val liftLibraryParams = backstackEntry.toRoute<Route.LiftLibrary>()
                val liftMetricChartIds = navHostController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<List<Long>>(LIFT_METRIC_CHART_IDS) ?: listOf()

                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(LiftLibraryScreen())

                    if (liftLibraryParams.workoutId != null || liftMetricChartIds.isNotEmpty()) {
                        setBottomNavBarVisibility(false)
                    } else {
                        setBottomNavBarVisibility(true)
                    }
                }

                LiftLibrary(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    callerRouteId = liftLibraryParams.callerRouteId,
                    workoutId = liftLibraryParams.workoutId,
                    workoutLiftId = liftLibraryParams.workoutLiftId,
                    movementPattern = liftLibraryParams.movementPattern ?: "",
                    addAtPosition = liftLibraryParams.addAtPosition,
                    liftMetricChartIds = liftMetricChartIds,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onChangeTopAppBarTitle = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(Screen.TITLE, it.left())
                        )
                    },
                    onToggleTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onClearTopAppBarFilterText = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                LiftLibraryScreen.LIFT_NAME_FILTER_TEXTVIEW,
                                "".left()
                            )
                        )
                    },
                    onNavigateHome = {
                        // Pop back to the start destination
                        navHostController.navigate(navHostController.graph.startDestinationRoute!!) {
                            popUpTo(navHostController.graph.startDestinationRoute!!) {
                                inclusive = true
                            }
                        }

                        // Go back to Home
                        navHostController.navigate(Route.Home)
                    },
                    onNavigateToLiftDetails = { liftId ->
                        val liftDetailsRoute = if (liftId != null)
                            Route.LiftDetails(liftId = liftId)
                        else
                            Route.LiftDetails()

                        navHostController.navigate(liftDetailsRoute)
                    },
                    onNavigateToWorkoutBuilder = { workoutBuilderWorkoutId ->
                        // Pop back to lab
                        navHostController.navigate(Route.Lab) {
                            popUpTo(navHostController.graph.startDestinationRoute!!) {
                                inclusive = false
                            }
                        }
                        navHostController.navigate(Route.WorkoutBuilder(workoutId = workoutBuilderWorkoutId))
                    },
                    onNavigateToActiveWorkout = {
                        // Pop back to lab
                        navHostController.navigate(Route.Workout(showLog = true)) {
                            popUpTo(navHostController.graph.startDestinationRoute!!) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }

        composable<Route.LiftDetails> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(LiftDetailsScreen())
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                }

                LiftDetails(
                    id = backstackEntry.toRoute<Route.LiftDetails>().liftId,
                    screenId = backstackEntry.id,
                    paddingValues = paddingValues,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onNavigateBack = { navHostController.popBackStack() },
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                request.controlName,
                                request.payload.left()
                            )
                        )
                    },
                )
            }
        }

        composable<Route.Workout> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                var showLog by remember(backstackEntry.id) {
                    mutableStateOf(
                        value = navHostController.currentBackStackEntry?.savedStateHandle?.get(
                            SHOW_WORKOUT_LOG
                        ) ?: backstackEntry.toRoute<Route.Workout>().showLog ?: false
                    )
                }
                navHostController.currentBackStackEntry?.savedStateHandle?.set(
                    SHOW_WORKOUT_LOG,
                    false
                )

                LaunchedEffect(key1 = navHostController.currentBackStackEntry) {
                    onSetScreen(WorkoutScreen())
                    if (navHostController.currentBackStackEntry?.id != backstackEntry.id) {
                        showLog = false
                    }
                }

                Workout(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    showLog = showLog,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setBottomNavBarVisibility = setBottomNavBarVisibility,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onNavigateToWorkoutHistory = {
                        navHostController.navigate(WorkoutHistoryScreen.navigation.route)
                    },
                ) { route ->
                    navHostController.navigate(route)
                }
            }
        }

        composable<Route.WorkoutHistory> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(WorkoutHistoryScreen())
                    setBottomNavBarVisibility(false)
                }

                WorkoutHistory(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onNavigateBack = {
                        navHostController.popBackStack()
                    },
                    onNavigateToEditWorkoutScreen = { workoutLogEntryId ->
                        navHostController.navigate(Route.EditWorkout(workoutLogEntryId = workoutLogEntryId))
                    }
                )
            }
        }

        composable<Route.EditWorkout> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(EditWorkoutScreen())
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                }

                EditWorkout(
                    workoutLogEntryId = backstackEntry.toRoute<Route.EditWorkout>().workoutLogEntryId,
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    onNavigateBack = {
                        navHostController.popBackStack()
                    }
                )
            }
        }

        composable<Route.Lab> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(LabScreen())
                    setBottomNavBarVisibility(true)
                }

                Lab(
                    paddingValues,
                    screenId = backstackEntry.id,
                    mutateTopAppBarControlValue = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                it.controlName,
                                it.payload.left()))
                    },
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onNavigateToWorkoutBuilder = { workoutId ->
                        navHostController.navigate(Route.WorkoutBuilder(workoutId = workoutId))
                    }
                )
            }
        }

        composable<Route.WorkoutBuilder> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(WorkoutBuilderScreen())
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                    setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)
                }

                WorkoutBuilder(
                    workoutId = backstackEntry.toRoute<Route.WorkoutBuilder>().workoutId,
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                request.controlName,
                                request.payload.left()
                            )
                        )
                    },
                    onNavigateBack = {
                        navHostController.popBackStack()
                    },
                    onNavigateToLiftLibrary = { route ->
                        navHostController.navigate(route)
                    }
                )
            }
        }
    }
}