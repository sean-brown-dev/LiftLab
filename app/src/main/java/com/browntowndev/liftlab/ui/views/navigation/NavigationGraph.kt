package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
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
import de.raphaelebner.roomdatabasebackup.core.RoomBackup


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
    NavHost(navHostController, startDestination = WorkoutScreen.navigation.route) {
        composable(HomeScreen.navigation.route) {
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isHomeScreen = remember(screen, currentRoute) {
                screen is HomeScreen &&
                        currentRoute == HomeScreen.navigation.route
            }
            if (isHomeScreen) {
                LaunchedEffect(key1 = Unit) {
                    setBottomNavBarVisibility(true)
                }
                Home(
                    paddingValues = paddingValues,
                    screenId = navHostController.currentBackStackEntry?.id,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onNavigateToSettingsMenu = { navHostController.navigate(SettingsScreen.navigation.route) },
                    onNavigateToLiftLibrary = { chartIds ->
                        navHostController.currentBackStackEntry!!.savedStateHandle[LIFT_METRIC_CHART_IDS] = chartIds
                        navHostController.navigate(LiftLibraryScreen.navigation.route)
                    },
                )
            }
        }
        composable(SettingsScreen.navigation.route) {
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isSettingsScreen = remember(screen, currentRoute) {
                screen is SettingsScreen &&
                        currentRoute == SettingsScreen.navigation.route
            }
            if (isSettingsScreen) {
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
        }
        composable(
            route = LiftLibraryScreen.navigation.route,
            arguments = listOf(
                navArgument("callerRoute") {
                  nullable = true
                },
                navArgument("workoutId") {
                    nullable = true
                },
                navArgument("workoutLiftId") {
                    nullable = true
                },
                navArgument("movementPattern") {
                    nullable = true
                },
                navArgument("addAtPosition") {
                    nullable = true
                },
            )
        ) { it ->
            val callerRoute = it.arguments?.getString("callerRoute")
            val workoutId = it.arguments?.getString("workoutId")?.toLongOrNull()
            val workoutLiftId = it.arguments?.getString("workoutLiftId")?.toLongOrNull()
            val movementPatternParam = it.arguments?.getString("movementPattern") ?: ""
            val addAtPosition = it.arguments?.getString("addAtPosition")?.toIntOrNull()
            val liftMetricChartIds = navHostController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Long>>(LIFT_METRIC_CHART_IDS) ?: listOf()

            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isLiftLibraryScreen = remember(screen, currentRoute) {
                screen is LiftLibraryScreen &&
                        (currentRoute?.startsWith(LiftLibraryScreen.navigation.route) ?: false)
            }
            if (isLiftLibraryScreen) {
                LaunchedEffect(key1 = Unit) {
                    if (workoutId != null || liftMetricChartIds.isNotEmpty()) {
                        setBottomNavBarVisibility(false)
                    } else {
                        setBottomNavBarVisibility(true)
                    }
                }

                LiftLibrary(
                    paddingValues = paddingValues,
                    screenId = navHostController.currentBackStackEntry?.id,
                    callerRoute = callerRoute,
                    workoutId = workoutId,
                    workoutLiftId = workoutLiftId,
                    movementPattern = movementPatternParam,
                    addAtPosition = addAtPosition,
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
                        navHostController.navigate(HomeScreen.navigation.route)
                    },
                    onNavigateToLiftDetails = { liftId ->
                        val liftDetailsRoute = if (liftId != null) {
                            LiftDetailsScreen.navigation.route
                                .replace("{id}", liftId.toString())
                        } else LiftDetailsScreen.navigation.route

                        navHostController.navigate(liftDetailsRoute)
                    },
                    onNavigateToWorkoutBuilder = { workoutBuilderWorkoutId ->
                        // Pop back to lab
                        navHostController.navigate(LabScreen.navigation.route) {
                            popUpTo(navHostController.graph.startDestinationRoute!!) {
                                inclusive = false
                            }
                        }

                        // Go back to workout builder
                        val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", workoutBuilderWorkoutId.toString())
                        navHostController.navigate(workoutBuilderRoute)
                    },
                    onNavigateToActiveWorkout = {
                        // Pop back to lab
                        navHostController.navigate(WorkoutScreen.navigation.route.replace("{showLog}", true.toString())) {
                            popUpTo(navHostController.graph.startDestinationRoute!!) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
        composable(
            route = LiftDetailsScreen.navigation.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                },
            )
        ) {
            val id = it.arguments?.getString("id")?.toLongOrNull()
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isLiftDetailsScreen = remember(screen, currentRoute) {
                screen is LiftDetailsScreen &&
                        (currentRoute?.startsWith(LiftDetailsScreen.navigation.route) ?: false)
            }

            if (isLiftDetailsScreen) {
                LaunchedEffect(key1 = Unit) {
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                }

                LiftDetails(
                    id = id,
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
        }
        composable(
            route = WorkoutScreen.navigation.route,
            arguments = listOf(
                navArgument("showLog") {
                    nullable = true
                },
            )
        ) {
            val backStackEntryId = remember(it.id) { it.id }
            var showLog by remember(it.id) {
                mutableStateOf(
                    value =  navHostController.currentBackStackEntry?.savedStateHandle?.get(SHOW_WORKOUT_LOG) ?:
                        it.arguments?.getString("showLog")?.toBoolean() ?: false
                )
            }
            navHostController.currentBackStackEntry?.savedStateHandle?.set(SHOW_WORKOUT_LOG, false)

            LaunchedEffect(key1 = navHostController.currentBackStackEntry) {
                if (navHostController.currentBackStackEntry?.id != backStackEntryId) {
                    showLog = false
                }
            }

            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isWorkoutScreen = remember(screen, currentRoute) {
                screen is WorkoutScreen &&
                        currentRoute == WorkoutScreen.navigation.route
            }

            if (isWorkoutScreen) {
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
        }
        composable(WorkoutHistoryScreen.navigation.route) {
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isWorkoutHistoryScreen = remember(screen, currentRoute) {
                screen is WorkoutHistoryScreen &&
                        currentRoute == WorkoutHistoryScreen.navigation.route
            }
            if (isWorkoutHistoryScreen) {
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
                        val editWorkoutRoute = EditWorkoutScreen.navigation.route.replace("{workoutLogEntryId}", workoutLogEntryId.toString())
                        navHostController.navigate(editWorkoutRoute)
                    }
                )
            }
        }
        composable(
            route = EditWorkoutScreen.navigation.route,
            arguments = listOf(
                navArgument("workoutLogEntryId") {
                    type = NavType.LongType
                    nullable = false
                },
            )
        ) {
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isEditWorkoutScreen = remember(screen, currentRoute) {
                screen is EditWorkoutScreen &&
                        currentRoute == EditWorkoutScreen.navigation.route
            }
            if (isEditWorkoutScreen) {
                val workoutLogEntryId = it.arguments?.getLong("workoutLogEntryId")
                LaunchedEffect(key1 = Unit) {
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                }

                EditWorkout(
                    workoutLogEntryId = workoutLogEntryId!!,
                    paddingValues = paddingValues,
                    screenId = navHostController.currentBackStackEntry?.id,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    onNavigateBack = {
                        navHostController.popBackStack()
                    }
                )
            }
        }
        composable(LabScreen.navigation.route) {
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isLabScreen = remember(screen, currentRoute) {
                screen is LabScreen &&
                        currentRoute == LabScreen.navigation.route
            }
            if (isLabScreen) {
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
                        val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", workoutId.toString())
                        navHostController.navigate(workoutBuilderRoute)
                    }
                )
            }
        }
        composable(
            route = WorkoutBuilderScreen.navigation.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                },
            )
        ) {
            val workoutId = it.arguments?.getLong("id")
            val currentScreen: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentScreen?.destination?.route

            val isWorkoutBuilderScreen = remember(screen, currentRoute) {
                screen is WorkoutBuilderScreen &&
                    (currentRoute?.startsWith(WorkoutBuilderScreen.navigation.route) ?: false)
            }
            if (isWorkoutBuilderScreen && workoutId != null) {
                LaunchedEffect(key1 = Unit) {
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                    setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)
                }

                WorkoutBuilder(
                    workoutId = workoutId,
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
}