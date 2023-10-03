package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import arrow.core.Either
import arrow.core.left
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
import com.browntowndev.liftlab.ui.views.main.WorkoutHistory
import com.browntowndev.liftlab.ui.views.main.lab.Lab
import com.browntowndev.liftlab.ui.views.main.liftlibrary.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.LiftDetails
import com.browntowndev.liftlab.ui.views.main.workout.Workout
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.WorkoutBuilder



@ExperimentalFoundationApi
@Composable
fun NavigationGraph(
    navHostController: NavHostController,
    paddingValues: PaddingValues,
    screen: Screen?,
    onNavigateBack: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
) {
    NavHost(navHostController, startDestination = WorkoutScreen.navigation.route) {
        composable(
            route = LiftLibraryScreen.navigation.route,
            arguments = listOf(
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
            val workoutId = it.arguments?.getString("workoutId")?.toLongOrNull()
            val workoutLiftId = it.arguments?.getString("workoutLiftId")?.toLongOrNull()
            val movementPatternParam = it.arguments?.getString("movementPattern") ?: ""
            val addAtPosition = it.arguments?.getString("addAtPosition")?.toIntOrNull()

            if (screen as? LiftLibraryScreen != null) {
                LaunchedEffect(key1 = screen) {
                    if (workoutId != null) {
                        setBottomNavBarVisibility(false)
                    } else {
                        setBottomNavBarVisibility(true)
                    }
                }

                LiftLibrary(
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    workoutId = workoutId,
                    workoutLiftId = workoutLiftId,
                    movementPattern = movementPatternParam,
                    addAtPosition = addAtPosition,
                    isSearchBarVisible = screen.isSearchBarVisible,
                    onNavigateBack = onNavigateBack,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onChangeTopAppBarTitle = { mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, it.left())) },
                    onToggleTopAppBarControlVisibility = { control, visible -> setTopAppBarControlVisibility(control, visible) },
                    onClearTopAppBarFilterText = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(LiftLibraryScreen.LIFT_NAME_FILTER_TEXTVIEW, "".left())
                        )
                    }
                )
            }
        }
        composable(
            route = LiftDetailsScreen.navigation.route,
            arguments = listOf(
                navArgument("id") {
                    nullable = false
                },
            )
        ) {
            val id = it.arguments?.getString("id")?.toLongOrNull()

            if (screen as? LiftDetailsScreen != null) {
                LaunchedEffect(key1 = screen) {
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                }

                LiftDetails(
                    id = id,
                    navHostController = navHostController,
                    paddingValues = paddingValues,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                    mutateTopAppBarControlValue = { request ->
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                request.controlName,
                                request.payload.left()))
                    },
                )
            }
        }
        composable(WorkoutScreen.navigation.route) {
            if (screen as? WorkoutScreen != null) {
                Workout(
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setBottomNavBarVisibility = setBottomNavBarVisibility,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                )
            }
        }
        composable(LabScreen.navigation.route) {
            if (screen as? LabScreen != null) {
                LaunchedEffect(key1 = screen) {
                    setBottomNavBarVisibility(true)
                }

                Lab(
                    paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                it.controlName,
                                it.payload.left()))
                    },
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
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
            if (screen as? WorkoutBuilderScreen != null && workoutId != null) {
                LaunchedEffect(key1 = screen) {
                    setBottomNavBarVisibility(false)
                    setTopAppBarCollapsed(true)
                    setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)
                }

                WorkoutBuilder(
                    workoutId = workoutId,
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                it.controlName,
                                it.payload.left()))
                    },
                )
            }
        }
        composable(WorkoutHistoryScreen.navigation.route) {
            LaunchedEffect(key1 = screen) {
                setBottomNavBarVisibility(true)
            }
            WorkoutHistory(paddingValues)
        }
    }
}