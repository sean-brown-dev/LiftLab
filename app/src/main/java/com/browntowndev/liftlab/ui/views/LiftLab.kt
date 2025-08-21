package com.browntowndev.liftlab.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import arrow.core.Either
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.ui.composables.LiftLabSnackbar
import com.browntowndev.liftlab.ui.composables.dialog.LiftLabDialog
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.theme.LiftLabTheme
import com.browntowndev.liftlab.ui.viewmodels.appBar.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.bottomNav.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.donation.DonationState
import com.browntowndev.liftlab.ui.views.navigation.BottomNavigation
import com.browntowndev.liftlab.ui.views.navigation.LiftLabTopAppBar
import com.browntowndev.liftlab.ui.views.navigation.NavigationGraph
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLab(
    initializing: Boolean,
    showSyncFailedDialog: Boolean,
    donationState: DonationState,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    onCloseSyncFailedDialog: () -> Unit,
    onBeginSync: () -> Unit,
) {
    LiftLabTheme {
        if (initializing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(.5f),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Setting up the lab...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        } else {
            val navController = rememberNavController()
            val bottomNavBarViewModel: BottomNavBarViewModel = koinViewModel()
            val topAppBarViewModel: TopAppBarViewModel = koinViewModel()
            val liftLabTopAppBarState by topAppBarViewModel.state.collectAsState()
            val timerState by topAppBarViewModel.timerState.collectAsStateWithLifecycle()
            val bottomNavBarState by bottomNavBarViewModel.state.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            val allowsCollapse = !liftLabTopAppBarState.isCollapsed
            val topAppBarState =
                if (allowsCollapse) rememberTopAppBarState()
                else rememberTopAppBarState()
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

            Scaffold(
                modifier = if(allowsCollapse) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier,
                bottomBar = {
                    BottomNavigation(
                        navController = navController,
                        isVisible = bottomNavBarState.isVisible
                    )
                },
                topBar = {
                    LiftLabTopAppBar(
                        state = liftLabTopAppBarState,
                        timerState = timerState,
                        allowCollapse = allowsCollapse,
                        scrollBehavior = scrollBehavior,
                        onCancelRestTimer = topAppBarViewModel::cancelRestTimer,
                        onSetControlVisibility = topAppBarViewModel::setControlVisibility,
                        onMutateControlValue = topAppBarViewModel::mutateControlValue,
                    )
                },
                snackbarHost = {
                    LiftLabSnackbar(snackbarHostState)
                }
            ) { scaffoldPaddingValues ->
                NavigationGraph(
                    navHostController = navController,
                    paddingValues = scaffoldPaddingValues,
                    donationState = donationState,
                    snackbarHostState = snackbarHostState,
                    onClearBillingError = onClearBillingError,
                    onUpdateDonationProduct = onUpdateDonationProduct,
                    onProcessDonation = onProcessDonation,
                    setTopAppBarCollapsed = { collapsed -> topAppBarViewModel.setCollapsed(collapsed) },
                    onSetScreen = { screen ->
                        topAppBarViewModel.setScreen(screen)
                    },
                    setTopAppBarControlVisibility = { control, visible ->
                        topAppBarViewModel.setControlVisibility(
                            control,
                            visible
                        )
                    },
                    mutateTopAppBarControlValue = { request ->
                        var payload: Any? = request.payload
                        if (request.payload is Either<*, *>) {
                            request.payload.onLeft {
                                payload = it
                            }.onRight {
                                payload = it
                            }
                        }

                        topAppBarViewModel.mutateControlValue(
                            request = AppBarMutateControlRequest(
                                controlName = request.controlName,
                                payload = payload
                            )
                        )
                    },
                    setBottomNavBarVisibility =  { bottomNavBarViewModel.setVisibility(it) },
                    onBeginSync = onBeginSync,
                )
            }
        }

        LiftLabDialog(
            isVisible = showSyncFailedDialog,
            header = "Sync Failed",
            onDismiss = onCloseSyncFailedDialog,
        ) {
            Text(
                text = "Failed to sync data. Try a manual sync from the sync menu on the Home screen.",
                textAlign = TextAlign.Center,
            )
        }
    }
}
