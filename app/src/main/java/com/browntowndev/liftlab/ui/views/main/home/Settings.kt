package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.SectionLabel
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Settings(
    roomBackup: RoomBackup,
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = koinViewModel {
        parametersOf(roomBackup, navHostController)
    }

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
            SectionLabel(text = "Data Management", fontSize = 14.sp)
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Import Database", fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    settingsViewModel.importDatabase(context)
                }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(id = R.drawable.upload_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.import_database),
                    )
                }
            }
        }
        item {
            Row (
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export Database", fontSize = 18.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    settingsViewModel.exportDatabase(context)
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
    }
}
