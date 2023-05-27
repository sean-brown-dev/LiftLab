package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.browntowndev.liftlab.ui.models.LabScreen
import com.browntowndev.liftlab.ui.models.NavItem
import com.browntowndev.liftlab.ui.views.main.Lab
import com.browntowndev.liftlab.ui.views.main.LiftLibrary
import com.browntowndev.liftlab.ui.views.main.Workout
import com.browntowndev.liftlab.ui.views.main.WorkoutHistory
import com.browntowndev.liftlab.ui.models.LiftLibraryScreen
import com.browntowndev.liftlab.ui.models.TopAppBarState
import com.browntowndev.liftlab.ui.models.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.models.WorkoutScreen


@Composable
fun NavigationGraph(navController: NavHostController, paddingValues: PaddingValues, topAppBarState: MutableState<TopAppBarState>) {
    NavHost(navController, startDestination = WorkoutScreen.navigation.route) {
        composable(LiftLibraryScreen.navigation.route) {
            LiftLibrary(paddingValues, topAppBarState)
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