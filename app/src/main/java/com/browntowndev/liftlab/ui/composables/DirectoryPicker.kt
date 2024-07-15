package com.browntowndev.liftlab.ui.composables

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.browntowndev.liftlab.R
import kotlinx.coroutines.launch


@Composable
fun DirectoryPicker(
    startingDirectory: String,
    onDirectoryChosen: (directory: String) -> Unit,
) {
    val context = LocalContext.current
    val externalStorageUri = remember { "/tree/primary:" }
    val coroutineScope = rememberCoroutineScope()
    var startingDirectoryUri by remember(startingDirectory) { mutableStateOf(Uri.parse(startingDirectory))}

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) {
        it?.let { selectedUri ->
            if (selectedUri.path.toString().startsWith(externalStorageUri)) {
                startingDirectoryUri = selectedUri
                val newDirectory = selectedUri.path.toString()
                    .replace(
                        oldValue = externalStorageUri,
                        newValue = "${Environment.getExternalStorageDirectory().path}/")

                onDirectoryChosen(newDirectory)
            } else {
                Toast.makeText(
                    context,
                    "Please only select folders from your local storage.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    IconButton(onClick = {
        coroutineScope.launch {
            directoryPickerLauncher.launch(startingDirectoryUri)
        }
    }) {
        Icon(
            painter = painterResource(id = R.drawable.folder),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Backup folder",
        )
    }
}