package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.browntowndev.liftlab.ui.models.BottomNavItem
import com.browntowndev.liftlab.ui.views.Lab
import com.browntowndev.liftlab.ui.views.LiftLibrary
import com.browntowndev.liftlab.ui.views.Workout
import com.browntowndev.liftlab.ui.views.WorkoutHistory


@Composable
fun NavigationGraph(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(navController, startDestination = BottomNavItem.Workout.screen_route) {
        composable(BottomNavItem.LiftLibrary.screen_route) {
            LiftLibrary(paddingValues)
        }
        composable(BottomNavItem.Workout.screen_route) {
            Workout(paddingValues)
        }
        composable(BottomNavItem.Lab.screen_route) {
            Lab(paddingValues)
        }
        composable(BottomNavItem.WorkoutHistory.screen_route) {
            WorkoutHistory(paddingValues)
        }
    }
}