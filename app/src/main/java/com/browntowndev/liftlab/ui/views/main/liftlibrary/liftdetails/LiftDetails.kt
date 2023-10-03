package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.ui.viewmodels.LiftDetailsViewModel
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LiftDetails(
    id: Long,
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val liftDetailsViewModel: LiftDetailsViewModel = koinViewModel { parametersOf(id, navHostController) }
    val state by liftDetailsViewModel.state.collectAsState()

    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = liftDetailsViewModel)

    if (state.lift != null) {
        var tabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Details", "History", "Charts")

        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = tabIndex, contentColor = MaterialTheme.colorScheme.primary) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }

            when (tabIndex) {
                0 -> DetailsTab(
                    liftName = state.lift!!.name,
                    volumeTypes = state.volumeTypeDisplayNames,
                    secondaryVolumeTypes = state.secondaryVolumeTypeDisplayNames,
                    onLiftNameChanged = { liftDetailsViewModel.updateName(it) },
                    onAddVolumeType = { liftDetailsViewModel.addVolumeType(it) },
                    onAddSecondaryVolumeType = { liftDetailsViewModel.addSecondaryVolumeType(it) },
                    onRemoveVolumeType = { liftDetailsViewModel.removeVolumeType(it) },
                    onRemoveSecondaryVolumeType = { liftDetailsViewModel.removeSecondaryVolumeType(it) },
                    onUpdateVolumeType = { index, newVolumeType ->  liftDetailsViewModel.updateVolumeType(index, newVolumeType) },
                    onUpdateSecondaryVolumeType = { index, newVolumeType -> liftDetailsViewModel.updateSecondaryVolumeType(index, newVolumeType) },
                )

                1, 2 -> {}
            }
        }
    }
}