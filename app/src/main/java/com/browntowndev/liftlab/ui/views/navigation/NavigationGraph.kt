package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import arrow.core.left
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.core.common.LIFT_METRIC_CHART_IDS
import com.browntowndev.liftlab.core.common.MERGE_LIFT_ID
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.controls.Route
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.EditWorkoutScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.Screen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.SettingsScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.WorkoutScreen
import com.browntowndev.liftlab.ui.viewmodels.donation.DonationState
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
    snackbarHostState: SnackbarHostState,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Any>) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    onSetScreen: (screen: Screen) -> Unit,
    onBeginSync: () -> Unit,
) {
    NavHost(navHostController, startDestination = Route.Workout()) {
        composable<Route.Home> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()
            val homeLazyListState = rememberLazyListState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(HomeScreen())
                    setBottomNavBarVisibility(true)
                }

                Home(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    snackbarHostState = snackbarHostState,
                    lazyListState = homeLazyListState,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onNavigateToSettingsMenu = { navHostController.navigate(Route.Settings) },
                    onNavigateToLiftLibrary = { chartIds ->
                        navHostController.currentBackStackEntry!!.savedStateHandle[LIFT_METRIC_CHART_IDS] = chartIds
                        navHostController.navigate(Route.LiftLibrary())
                    },
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = request.payload
                            )
                        )
                    },
                    onBeginSync = onBeginSync,
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
                    snackbarHostState = snackbarHostState,
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
                )
            }
        }

        composable<Route.LiftLibrary> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()
            val liftLazyListState = rememberLazyListState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                val liftLibraryParams = backstackEntry.toRoute<Route.LiftLibrary>()
                val mergeLiftId = backstackEntry.savedStateHandle.get<Long>(MERGE_LIFT_ID)
                val liftMetricChartIds = navHostController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<List<Long>>(LIFT_METRIC_CHART_IDS) ?: listOf()

                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(LiftLibraryScreen())

                    if (liftLibraryParams.workoutId != null || liftMetricChartIds.isNotEmpty() || mergeLiftId != null) {
                        setBottomNavBarVisibility(false)
                    } else {
                        setBottomNavBarVisibility(true)
                    }
                }

                LiftLibrary(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    snackbarHostState = snackbarHostState,
                    lazyListState = liftLazyListState,
                    callerRouteId = liftLibraryParams.callerRouteId,
                    workoutId = liftLibraryParams.workoutId,
                    workoutLiftId = liftLibraryParams.workoutLiftId,
                    mergeLiftId = mergeLiftId,
                    movementPattern = liftLibraryParams.movementPattern ?: "",
                    addAtPosition = liftLibraryParams.addAtPosition,
                    liftMetricChartIds = liftMetricChartIds,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onChangeTopAppBarTitle = { title ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(Screen.TITLE, title)
                        )
                    },
                    onToggleTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onClearTopAppBarFilterText = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                LiftLibraryScreen.LIFT_NAME_FILTER_TEXTVIEW,
                                ""
                            )
                        )
                    },
                    onNavigateHome = {
                        navHostController.popBackStack()
                    },
                    onNavigateToLiftDetails = { liftId ->
                        backstackEntry.savedStateHandle.remove<Long>(MERGE_LIFT_ID)
                        val liftDetailsRoute = if (liftId != null)
                            Route.LiftDetails(liftId = liftId)
                        else
                            Route.LiftDetails()

                        navHostController.navigate(liftDetailsRoute)
                    },
                    onNavigateToWorkoutBuilder = { workoutBuilderWorkoutId ->
                        navHostController.popBackStack()
                    },
                    onNavigateToActiveWorkout = {
                        navHostController.popBackStack()
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

                val liftId = backstackEntry.toRoute<Route.LiftDetails>().liftId
                LiftDetails(
                    id = liftId,
                    screenId = backstackEntry.id,
                    snackbarHostState = snackbarHostState,
                    paddingValues = paddingValues,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onNavigateBack = { navHostController.popBackStack() },
                    onMergeLift = {
                        navHostController.previousBackStackEntry?.savedStateHandle?.set(MERGE_LIFT_ID, liftId)
                        navHostController.popBackStack()
                    },
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = request.payload as Any
                            )
                        )
                    },
                )
            }
        }

        composable<Route.Workout> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = navHostController.currentBackStackEntry) {
                    onSetScreen(WorkoutScreen())
                }

                Workout(
                    paddingValues = paddingValues,
                    screenId = backstackEntry.id,
                    snackbarHostState = snackbarHostState,
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = request.payload))
                    },
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setBottomNavBarVisibility = setBottomNavBarVisibility,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    onNavigateToWorkoutHistory = {
                        navHostController.navigate(WorkoutHistoryScreen.navigation.route)
                    },
                    onNavigateToLiftLibrary = { workoutId, workoutLiftId, movementPattern ->
                        navHostController.navigate(
                            Route.LiftLibrary(
                                callerRouteId = Route.Workout.id,
                                workoutId = workoutId,
                                workoutLiftId = workoutLiftId,
                                movementPattern = movementPattern.displayName()
                            )
                        )
                    }
                )
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
                    snackbarHostState = snackbarHostState,
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
                    snackbarHostState = snackbarHostState,
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = request.payload))
                    },
                    onNavigateBack = {
                        navHostController.popBackStack()
                    }
                )
            }
        }

        composable<Route.Lab> { backstackEntry ->
            val currentBackstackEntry by navHostController.currentBackStackEntryAsState()
            val labLazyListState = rememberLazyListState()

            if (currentBackstackEntry?.id == backstackEntry.id) {
                LaunchedEffect(key1 = backstackEntry.id) {
                    onSetScreen(LabScreen())
                    setBottomNavBarVisibility(true)
                }

                Lab(
                    paddingValues = paddingValues,
                    lazyListState = labLazyListState,
                    screenId = backstackEntry.id,
                    snackbarHostState = snackbarHostState,
                    mutateTopAppBarControlValue = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = it.controlName,
                                payload = it.payload as Any)
                        )
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
                    snackbarHostState = snackbarHostState,
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = request.payload.left()
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