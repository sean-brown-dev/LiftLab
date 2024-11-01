package com.browntowndev.liftlab.ui.views.navigation

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.browntowndev.liftlab.ui.models.BottomNavItem
import com.browntowndev.liftlab.ui.viewmodels.states.screens.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen

@ExperimentalFoundationApi
@Composable
fun BottomNavigation(navController: NavController, isVisible: Boolean) {
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
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = remember(currentBackStackEntry?.id) {
                currentBackStackEntry?.destination?.route?.split("?", "/")?.first()
            }
            val screens: List<BottomNavItem> = remember {
                listOf(
                    HomeScreen.navigation,
                    WorkoutScreen.navigation,
                    LabScreen.navigation,
                    LiftLibraryScreen.navigation,
                )
            }

            screens.fastForEach { screen ->
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = screen.bottomNavIconResourceId), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(screen.title, color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    selected = remember(currentRoute) { isSelectedRoute(currentRoute, screen) },
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

private fun isSelectedRoute(currentRoute: String?, screen: BottomNavItem): Boolean =
    when (screen.route) {
        is Route.Home -> currentRoute?.endsWith("Home") ?: false
        is Route.Workout -> currentRoute?.endsWith("Workout") ?: false
        is Route.Lab -> currentRoute?.endsWith("Lab") ?: false
        is Route.LiftLibrary -> currentRoute?.endsWith("LiftLibrary") ?: false
        else -> throw Exception("Unrecognized bottom navigation screen.")
    }