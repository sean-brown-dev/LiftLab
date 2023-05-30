package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.WorkoutScreen
import com.browntowndev.liftlab.ui.views.main.Lab
import com.browntowndev.liftlab.ui.views.main.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.Workout
import com.browntowndev.liftlab.ui.views.main.WorkoutHistory


@Composable
fun NavigationGraph(navController: NavHostController, paddingValues: PaddingValues, liftLabTopAppBarState: LiftLabTopAppBarState, topAppBarViewModel: TopAppBarViewModel) {
    NavHost(navController, startDestination = WorkoutScreen.navigation.route) {
        composable(LiftLibraryScreen.navigation.route) {
            LiftLibrary(paddingValues, liftLabTopAppBarState, topAppBarViewModel)
        }
        composable(WorkoutScreen.navigation.route) {
            Workout(paddingValues)
        }
        composable(LabScreen.navigation.route) {
            Lab(paddingValues)
        }
        composable(WorkoutHistoryScreen.navigation.route) {
            WorkoutHistory(paddingValues)
        }
    }
}