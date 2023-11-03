package com.browntowndev.liftlab.ui.views.main.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Settings(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = koinViewModel {
        parametersOf(navHostController)
    }
    val state by settingsViewModel.state.collectAsState()

    BackHandler(state.isPerformingIo) { }

    LazyColumn(
        modifier = Modifier.padding(paddingValues = paddingValues),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            val openDirectoryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    settingsViewModel.exportDatabase(context, uri)
                }
            }

            Row (verticalAlignment = Alignment.CenterVertically) {
                Text("Export Database")
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { openDirectoryLauncher.launch(null) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.download_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.export_database),
                    )
                }
            }
        }
    }
}
