package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.ui.composables.SnackbarProvider
import com.browntowndev.liftlab.ui.composables.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.Screen
import com.browntowndev.liftlab.ui.viewmodels.liftDetails.LiftDetailsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiftDetails(
    id: Long?,
    paddingValues: PaddingValues,
    screenId: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onMergeLift: () -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
) {

    val liftDetailsViewModel: LiftDetailsViewModel = koinViewModel {
        parametersOf(id, onNavigateBack, onMergeLift)
    }
    val state by liftDetailsViewModel.state.collectAsState()

    liftDetailsViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = liftDetailsViewModel)
    SnackbarProvider(snackbarHostState, liftDetailsViewModel.userMessages)

    LaunchedEffect(key1 = id) {
        if (id == null) {
            setTopAppBarControlVisibility(LiftDetailsScreen.CONFIRM_CREATE_LIFT_ICON, true)
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    controlName = Screen.TITLE,
                    payload = "Create Lift"
                )
            )
        }
    }

    val tabs = listOf("Details", "History", "Charts")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        if (id != null) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { currentPage ->
            when (currentPage) {
                0 -> DetailsTab(
                    liftName = state.lift?.name ?: "",
                    liftNamePlaceholder = remember(id) { if (id == null) "New Lift" else "" },
                    movementPattern = state.lift?.movementPattern ?: MovementPattern.AB_ISO,
                    volumeTypeOptions = state.volumeTypeOptions,
                    volumeTypes = state.lift?.volumeTypes ?: emptyList(),
                    secondaryVolumeTypes = state.lift?.secondaryVolumeTypes ?: emptyList(),
                    onLiftNameChanged = { liftDetailsViewModel.updateName(it) },
                    onAddVolumeType = { liftDetailsViewModel.addVolumeType(it) },
                    onAddSecondaryVolumeType = { liftDetailsViewModel.addSecondaryVolumeType(it) },
                    onRemoveVolumeType = { liftDetailsViewModel.removeVolumeType(it) },
                    onRemoveSecondaryVolumeType = { liftDetailsViewModel.removeSecondaryVolumeType(it) },
                    onUpdateMovementPattern = { liftDetailsViewModel.updateMovementPattern(it) },
                    onUpdateVolumeType = { index, newVolumeType ->
                        liftDetailsViewModel.updateVolumeType(
                            index,
                            newVolumeType
                        )
                    },
                    onUpdateSecondaryVolumeType = { index, newVolumeType ->
                        liftDetailsViewModel.updateSecondaryVolumeType(
                            index,
                            newVolumeType
                        )
                    },
                )

                1 -> HistoryTab(
                    oneRepMax = state.oneRepMax,
                    maxVolume = state.maxVolume,
                    maxWeight = state.maxWeight,
                    topTenPerformances = state.topTenPerformances,
                    totalReps = state.totalReps,
                    totalVolume = state.totalVolume,
                )

                2 -> ChartsTab(
                    oneRepMaxChartModel = state.oneRepMaxChartModel,
                    volumeChartModel = state.volumeChartModel,
                    intensityChartModel = state.intensityChartModel,
                    workoutFilterOptions = state.workoutFilterOptions,
                    selectedOneRepMaxWorkoutFilters = state.selectedOneRepMaxWorkoutFilters,
                    selectedVolumeWorkoutFilters = state.selectedVolumeWorkoutFilters,
                    selectedIntensityWorkoutFilters = state.selectedIntensityWorkoutFilters,
                    onFilterOneRepMaxChartByWorkouts = {
                        liftDetailsViewModel.filterOneRepMaxChart(it)
                    },
                    onFilterVolumeChartByWorkouts = {
                        liftDetailsViewModel.filterVolumeChart(it)
                    },
                    onFilterIntensityChartByWorkouts = {
                        liftDetailsViewModel.filterIntensityChart(it)
                    }
                )
            }
        }
    }
}