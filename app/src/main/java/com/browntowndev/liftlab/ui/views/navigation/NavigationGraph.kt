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
import androidx.navigation.toRoute
import arrow.core.Either
import arrow.core.left
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.core.common.LIFT_METRIC_CHART_IDS
import com.browntowndev.liftlab.core.common.SHOW_WORKOUT_LOG
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.DonationState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
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
    screen: Screen?,
    donationState: DonationState,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    onNavigateBack: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    NavHost(navHostController, startDestination = Route.Workout()) {
        composable<Route.Home> { homeBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(true)
            }
            Home(
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
                setTopAppBarCollapsed = setTopAppBarCollapsed,
                onNavigateToSettingsMenu = { navHostController.navigate(Route.Settings) },
                onNavigateToLiftLibrary = { chartIds ->
                    navHostController.currentBackStackEntry!!.savedStateHandle[LIFT_METRIC_CHART_IDS] = chartIds
                    navHostController.navigate(Route.LiftLibrary())
                },
            )
        }

        composable<Route.Settings> { settingsBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(false)
            }

            Settings(
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
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

        composable<Route.LiftLibrary> { liftLibraryBackstackEntry ->
            val liftLibraryParams = liftLibraryBackstackEntry.toRoute<Route.LiftLibrary>()
            val liftMetricChartIds = navHostController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Long>>(LIFT_METRIC_CHART_IDS) ?: listOf()

            LaunchedEffect(key1 = Unit) {
                if (liftLibraryParams.workoutId != null || liftMetricChartIds.isNotEmpty()) {
                    setBottomNavBarVisibility(false)
                } else {
                    setBottomNavBarVisibility(true)
                }
            }

            LiftLibrary(
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
                callerRouteId = liftLibraryParams.callerRouteId,
                workoutId = liftLibraryParams.workoutId,
                workoutLiftId = liftLibraryParams.workoutLiftId,
                movementPattern = liftLibraryParams.movementPattern ?: "",
                addAtPosition = liftLibraryParams.addAtPosition,
                liftMetricChartIds = liftMetricChartIds,
                isSearchBarVisible = (screen as LiftLibraryScreen).isSearchBarVisible,
                onNavigateBack = onNavigateBack,
                setTopAppBarCollapsed = setTopAppBarCollapsed,
                onChangeTopAppBarTitle = { mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, it.left())) },
                onToggleTopAppBarControlVisibility = setTopAppBarControlVisibility,
                onClearTopAppBarFilterText = {
                    mutateTopAppBarControlValue(
                        AppBarMutateControlRequest(LiftLibraryScreen.LIFT_NAME_FILTER_TEXTVIEW, "".left())
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

        composable<Route.LiftDetails> { liftDetailsBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(false)
                setTopAppBarCollapsed(true)
            }

            LiftDetails(
                id = liftDetailsBackstackEntry.toRoute<Route.LiftDetails>().liftId,
                screenId = navHostController.currentBackStackEntry?.id,
                paddingValues = paddingValues,
                setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                onNavigateBack = { navHostController.popBackStack() },
                mutateTopAppBarControlValue = { request ->
                    mutateTopAppBarControlValue(
                        AppBarMutateControlRequest(
                            request.controlName,
                            request.payload.left()))
                },
            )
        }

        composable<Route.Workout> { workoutBackstackEntry ->
            var showLog by remember(workoutBackstackEntry.id) {
                mutableStateOf(
                    value =  navHostController.currentBackStackEntry?.savedStateHandle?.get(SHOW_WORKOUT_LOG) ?:
                        workoutBackstackEntry.toRoute<Route.Workout>().showLog ?: false
                )
            }
            navHostController.currentBackStackEntry?.savedStateHandle?.set(SHOW_WORKOUT_LOG, false)

            LaunchedEffect(key1 = navHostController.currentBackStackEntry) {
                if (navHostController.currentBackStackEntry?.id != workoutBackstackEntry.id) {
                    showLog = false
                }
            }

            Workout(
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
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

        composable<Route.WorkoutHistory> { workoutHistoryBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(false)
            }

            WorkoutHistory(
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
                setTopAppBarCollapsed = setTopAppBarCollapsed,
                onNavigateBack = {
                    navHostController.popBackStack()
                },
                onNavigateToEditWorkoutScreen = { workoutLogEntryId ->
                    navHostController.navigate(Route.EditWorkout(workoutLogEntryId = workoutLogEntryId))
                }
            )
        }

        composable<Route.EditWorkout> { editWorkoutBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(false)
                setTopAppBarCollapsed(true)
            }

            EditWorkout(
                workoutLogEntryId = editWorkoutBackstackEntry.toRoute<Route.EditWorkout>().workoutLogEntryId,
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
                mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                onNavigateBack = {
                    navHostController.popBackStack()
                }
            )
        }

        composable<Route.Lab> { labBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(true)
            }

            Lab(
                paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
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

        composable<Route.WorkoutBuilder> { workoutBuilderBackstackEntry ->
            LaunchedEffect(key1 = Unit) {
                setBottomNavBarVisibility(false)
                setTopAppBarCollapsed(true)
                setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)
            }

            WorkoutBuilder(
                workoutId = workoutBuilderBackstackEntry.toRoute<Route.WorkoutBuilder>().workoutId,
                paddingValues = paddingValues,
                screenId = navHostController.currentBackStackEntry?.id,
                mutateTopAppBarControlValue = { request ->
                    mutateTopAppBarControlValue(
                        AppBarMutateControlRequest(
                            request.controlName,
                            request.payload.left()))
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