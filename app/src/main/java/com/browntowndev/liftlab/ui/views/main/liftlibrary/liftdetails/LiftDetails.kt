package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.LiftDetailsViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LiftDetails(
    id: Long?,
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
) {
    val liftDetailsViewModel: LiftDetailsViewModel = koinViewModel { parametersOf(id, navHostController) }
    val state by liftDetailsViewModel.state.collectAsState()

    liftDetailsViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = liftDetailsViewModel)

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

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "History", "Charts")
    Column(modifier = Modifier.padding(paddingValues)) {
        if (id != null) {
            TabRow(selectedTabIndex = tabIndex, contentColor = MaterialTheme.colorScheme.primary, containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
        }

        when (tabIndex) {
            0 -> DetailsTab(
                liftName = state.lift?.name ?: "",
                liftNamePlaceholder = remember(id) { if (id == null) "New Lift" else "" },
                movementPattern = state.lift?.movementPattern ?: MovementPattern.AB_ISO,
                volumeTypes = state.volumeTypeDisplayNames,
                secondaryVolumeTypes = state.secondaryVolumeTypeDisplayNames,
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

            2 -> ChartsTab(oneRepMaxChartValues = state.oneRepMaxChartValues)
        }
    }
}