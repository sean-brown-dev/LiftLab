package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.browntowndev.liftlab.ui.models.BottomNavItem
import com.browntowndev.liftlab.ui.viewmodels.states.screens.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen

@ExperimentalFoundationApi
@Composable
fun BottomNavigation(navController: NavController, isVisible: Boolean) {
    val screens: List<BottomNavItem> = listOf(
        HomeScreen.navigation,
        WorkoutScreen.navigation,
        LabScreen.navigation,
        LiftLibraryScreen.navigation,
    )

    AnimatedVisibility(
        modifier = Modifier.animateContentSize(),
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)),
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            screens.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = screen.bottomNavIconResourceId), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(screen.title, color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        navController.navigate(screen.route) {
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
}