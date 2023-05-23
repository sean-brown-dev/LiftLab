package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.browntowndev.liftlab.ui.models.BottomNavItem

@Composable
fun BottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.LiftLibrary,
        BottomNavItem.Workout,
        BottomNavItem.Lab,
        BottomNavItem.WorkoutHistory
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) },
                label = { Text(screen.title, color = MaterialTheme.colorScheme.onBackground) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.screen_route } == true,
                onClick = {
                    navController.navigate(screen.screen_route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}