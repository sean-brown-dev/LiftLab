package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
import com.browntowndev.liftlab.ui.views.main.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.WorkoutHistory
import com.browntowndev.liftlab.ui.views.main.lab.Lab
import com.browntowndev.liftlab.ui.views.main.workout.Workout
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.WorkoutBuilder


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun NavigationGraph(
    navHostController: NavHostController,
    paddingValues: PaddingValues,
    screen: Screen?,
    onNavigateBack: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
    setBottomSheetContent: (label: String, volumeChipLabels: List<CharSequence>) -> Unit,
    setBottomSheetVisibility: (visible: Boolean) -> Unit,
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
            val libraryScreen = screen as? LiftLibraryScreen

            if (libraryScreen != null) {
                setBottomSheetVisibility(false)
                if (movementPatternParam.isNotEmpty() && workoutLiftId != null) {
                    setBottomNavBarVisibility(false)
                } else {
                    setBottomNavBarVisibility(true)
                }
                LiftLibrary(
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    workoutId = workoutId,
                    workoutLiftId = workoutLiftId,
                    movementPattern = movementPatternParam,
                    addAtPosition = addAtPosition,
                    isSearchBarVisible = libraryScreen.isSearchBarVisible,
                    onNavigateBack = onNavigateBack,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    onChangeTopAppBarTitle = { mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, it)) },
                    onToggleTopAppBarControlVisibility = { control, visible -> setTopAppBarControlVisibility(control, visible) },
                    onClearTopAppBarFilterText = {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(LiftLibraryScreen.LIFT_NAME_FILTER_TEXTVIEW, "")
                        )
                    }
                )
            }
        }
        composable(WorkoutScreen.navigation.route) {
            if (screen as? WorkoutScreen != null) {
                setBottomSheetVisibility(true)
                setBottomNavBarVisibility(true)

                Workout(
                    paddingValues = paddingValues,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    setBottomSheetContent = setBottomSheetContent,
                )
            }
        }
        composable(LabScreen.navigation.route) {
            val labScreen = screen as? LabScreen

            if (labScreen != null) {
                setBottomSheetVisibility(true)
                setBottomNavBarVisibility(true)
                setTopAppBarCollapsed(false)
                setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, false)

                Lab(
                    paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                ) { label, volumeChips ->
                    setBottomSheetContent(label, volumeChips)
                }
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
            val workoutBuilderScreen = screen as? WorkoutBuilderScreen
            val workoutId = it.arguments?.getLong("id")

            if (workoutBuilderScreen != null && workoutId != null) {
                setBottomSheetVisibility(true)
                setBottomNavBarVisibility(false)
                setTopAppBarCollapsed(true)
                setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)

                WorkoutBuilder(
                    workoutId = workoutId,
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                ) { label, volumeChips ->
                    setBottomSheetContent(label, volumeChips)
                }
            }
        }
        composable(WorkoutHistoryScreen.navigation.route) {
            setBottomSheetVisibility(false)
            setBottomNavBarVisibility(true)
            WorkoutHistory(paddingValues)
        }
    }
}