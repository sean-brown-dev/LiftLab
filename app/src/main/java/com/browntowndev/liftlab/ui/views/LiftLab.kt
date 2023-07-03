package com.browntowndev.liftlab.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.BottomSheetViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.BottomSheetState
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabBottomSheet
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab() {
    LiftLabTheme {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val bottomNavBarViewModel: BottomNavBarViewModel = getViewModel()
        val topAppBarViewModel: TopAppBarViewModel = getViewModel()
        val topAppBarState by topAppBarViewModel.state.collectAsState()
        val bottomNavBarState by bottomNavBarViewModel.state.collectAsState()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        LaunchedEffect(key1 = Unit) {
            navController.currentBackStackEntryFlow
                .distinctUntilChanged()
                .onEach { backStackEntry ->
                    val route = backStackEntry.destination.route
                    topAppBarViewModel.setScreen(route)
                }
                .launchIn(scope)
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = {
                BottomNavigation(
                    navController = navController,
                    isVisible = bottomNavBarState.isVisible
                )
            },
            topBar = {
                LiftLabTopAppBar(
                    state = topAppBarState,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { scaffoldPaddingValues ->
            val bottomSheetViewModel: BottomSheetViewModel = koinViewModel()
            val bottomSheetState: BottomSheetState by bottomSheetViewModel.state.collectAsState()
            var sheetPeekHeight by remember { mutableStateOf(if(bottomNavBarState.isVisible) 110.dp else 55.dp) }
            var bottomSpacerHeight by remember { mutableStateOf(if(bottomNavBarState.isVisible) 55.dp else 0.dp) }
            LaunchedEffect(key1 = bottomNavBarState.isVisible) {
                sheetPeekHeight = if(bottomNavBarState.isVisible) 115.dp else 35.dp
                bottomSpacerHeight = if(bottomNavBarState.isVisible) 75.dp else 0.dp
            }

            LiftLabBottomSheet(
                visible = bottomSheetState.visible,
                sheetPeekHeight = sheetPeekHeight,
                bottomSpacerHeight = bottomSpacerHeight,
                label = bottomSheetState.label,
                volumeTypes = bottomSheetState.volumeChipLabels,
            ) {
                NavigationGraph(
                    navHostController = navController,
                    paddingValues = scaffoldPaddingValues,
                    screen = topAppBarState.currentScreen,
                    onNavigateBack = { topAppBarState.onNavigationIconClick?.invoke() },
                    setBottomNavBarVisibility = { bottomNavBarViewModel.setVisibility(it) },
                    mutateTopAppBarControlValue = { request ->
                        topAppBarViewModel.mutateControlValue(
                            request
                        )
                    },
                    setTopAppBarCollapsed = { collapsed -> topAppBarViewModel.setCollapsed(collapsed) },
                    setTopAppBarControlVisibility = { control, visible ->
                        topAppBarViewModel.setControlVisibility(
                            control,
                            visible
                        )
                    },
                    setBottomSheetContent = { title, filterChipLabels ->
                        bottomSheetViewModel.updateSheetContent(
                            label = title,
                            volumeChipLabels = filterChipLabels,
                        )
                    },
                    setBottomSheetVisibility = { bottomSheetViewModel.setSheetVisibility(it) }
                )
            }
        }
    }
}
