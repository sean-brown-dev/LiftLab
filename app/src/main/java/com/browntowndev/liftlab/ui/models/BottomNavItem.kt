package com.browntowndev.liftlab.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(var title:String, var icon:ImageVector, var screen_route:String){

    object LiftLibrary: BottomNavItem("Lifts", Icons.Filled.List, "liftLibrary")
    object Workout: BottomNavItem("Workout", Icons.Filled.Add, "workout")
    object Lab: BottomNavItem("Lab", Icons.Filled.Person, "lab")
    object WorkoutHistory: BottomNavItem("History", Icons.Filled.DateRange, "history")
}