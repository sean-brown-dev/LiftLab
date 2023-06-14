package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
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
import com.browntowndev.liftlab.ui.views.main.Lab
import com.browntowndev.liftlab.ui.views.main.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.Workout
import com.browntowndev.liftlab.ui.views.main.WorkoutBuilder
import com.browntowndev.liftlab.ui.views.main.WorkoutHistory


@ExperimentalFoundationApi
@Composable
fun NavigationGraph(
    navHostController: NavHostController,
    paddingValues: PaddingValues,
    screen: Screen?,
    onNavigateBack: () -> Unit,
    showBottomNavBar: () -> Unit,
    hideBottomNavBar: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit
) {
    NavHost(navHostController, startDestination = WorkoutScreen.navigation.route) {
        composable(LiftLibraryScreen.navigation.route) {
            val libraryScreen = screen as? LiftLibraryScreen

            if (libraryScreen != null) {
                showBottomNavBar()
                LiftLibrary(
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    isSearchBarVisible = libraryScreen.isSearchBarVisible,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
        composable(WorkoutScreen.navigation.route) {
            showBottomNavBar()
            Workout(paddingValues)
        }
        composable(LabScreen.navigation.route) {
            val labScreen = screen as? LabScreen

            if (labScreen != null) {
                showBottomNavBar()
                setTopAppBarCollapsed(false)
                setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, false)

                Lab(
                    paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                    setTopAppBarCollapsed = setTopAppBarCollapsed,
                    setTopAppBarControlVisibility = setTopAppBarControlVisibility,
                )
            }
        }
        composable(
            route = WorkoutBuilderScreen.navigation.route + "/{id}",
            arguments = listOf(navArgument("id") {
                type = NavType.LongType
            })
        ) {
            val workoutBuilderScreen = screen as? WorkoutBuilderScreen
            val workoutId = it.arguments?.getLong("id")

            if (workoutBuilderScreen != null && workoutId != null) {
                hideBottomNavBar()
                setTopAppBarCollapsed(true)
                setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)

                WorkoutBuilder(
                    workoutId = workoutId,
                    paddingValues = paddingValues,
                    navHostController = navHostController,
                    mutateTopAppBarControlValue = mutateTopAppBarControlValue,
                )
            }
        }
        composable(WorkoutHistoryScreen.navigation.route) {
            showBottomNavBar()
            WorkoutHistory(paddingValues)
        }
    }
}