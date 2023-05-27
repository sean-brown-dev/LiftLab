package com.browntowndev.liftlab.ui.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.browntowndev.liftlab.ui.models.TopAppBarState
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab() {
    LiftLabTheme {
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        val topAppBarState = remember { mutableStateOf(TopAppBarState(navController, coroutineScope)) }
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = { BottomNavigation(navController = navController) },
            topBar = {
                LiftLabTopAppBar(
                    state = topAppBarState,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            NavigationGraph(
                navController = navController,
                paddingValues = paddingValues,
                topAppBarState = topAppBarState,
            )
        }
    }
}