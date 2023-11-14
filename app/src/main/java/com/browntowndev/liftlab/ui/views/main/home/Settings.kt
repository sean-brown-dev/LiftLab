package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.INCREMENT_OPTIONS
import com.browntowndev.liftlab.core.common.REST_TIME_RANGE
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import com.browntowndev.liftlab.ui.views.composables.ConfirmationModal
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.NumberPickerSpinner
import com.browntowndev.liftlab.ui.views.composables.SectionLabel
import com.browntowndev.liftlab.ui.views.composables.TimeSelectionSpinner
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun Settings(
    roomBackup: RoomBackup,
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val settingsViewModel: SettingsViewModel = koinViewModel {
        parametersOf(roomBackup, navHostController)
    }
    val state by settingsViewModel.state.collectAsState()

    settingsViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = settingsViewModel)

    LazyColumn(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item {
            SectionLabel(text = "DATA MANAGEMENT", fontSize = 14.sp)
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Import Database", fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    settingsViewModel.toggleImportConfirmationDialog()
                }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(id = R.drawable.upload_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.import_database),
                    )
                }
            }
            if (state.importConfirmationDialogShown) {
                ConfirmationModal(
                    header = "Warning!",
                    body = "This will replace all of your data with the data in the imported database. There is no way to undo this.",
                    onConfirm = { settingsViewModel.importDatabase() },
                    onCancel = { settingsViewModel.toggleImportConfirmationDialog() }
                )
            }
        }
        item {
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Export Database", fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    settingsViewModel.exportDatabase()
                }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(id = R.drawable.download_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.export_database),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 25.dp, bottom = 25.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Divider(
                    modifier = Modifier.fillMaxWidth(.95f),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            SectionLabel(text = "DEFAULTS", fontSize = 14.sp)
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Default Increment", fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                NumberPickerSpinner(
                    modifier = Modifier.padding(start = 165.dp),
                    options = INCREMENT_OPTIONS,
                    initialValue = state.defaultIncrement ?: DEFAULT_INCREMENT_AMOUNT,
                    onChanged = { settingsViewModel.updateIncrement(it) }
                )
            }
        }
        item {
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rest Time", fontSize = 18.sp)
                TimeSelectionSpinner(
                    modifier = Modifier.padding(start = 100.dp),
                    time = state.defaultRestTimeString ?: DEFAULT_REST_TIME.toDuration(DurationUnit.MILLISECONDS),
                    onTimeChanged = { settingsViewModel.updateDefaultRestTime(it) },
                    rangeInMinutes = REST_TIME_RANGE,
                    secondsStepSize = 5,
                )
            }
        }
    }
}

