package com.browntowndev.liftlab

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.notifications.NotificationHelper
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.scheduledBackup.LiftLabRoomBackup
import com.browntowndev.liftlab.core.scheduledBackup.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER
import com.browntowndev.liftlab.core.scheduledBackup.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
import com.browntowndev.liftlab.core.scheduledBackup.OnCompleteListener.Companion.EXIT_CODE_ERROR_BY_USER_CANCELED
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {

    private lateinit var roomRestore: LiftLabRoomBackup

    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !LiftLabDatabase.initialized.value
            }
        }

        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)
        val db = LiftLabDatabase.getInstance(this@MainActivity)

        val roomBackup: LiftLabRoomBackup =
            LiftLabRoomBackup(
                context = this@MainActivity,
                roomDatabase = db,
                backupFile = Utils.General.backupFile
            ).apply {
                onCompleteListener { success, roomBackupMessage, code ->
                    if (code != EXIT_CODE_ERROR_BY_USER_CANCELED &&
                        code != EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER &&
                        code != EXIT_CODE_ERROR_BACKUP_FILE_CREATOR) {
                        val message = if (success) "Success!" else roomBackupMessage
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }

                    if (success) {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        restartApp(intent)
                    }
                }
            }

        // --- RoomBackup Instance for Restore ---
        // This instance will be used by the ActivityResultLauncher callback
         roomRestore = LiftLabRoomBackup(
            context = this@MainActivity,
            roomDatabase = db,
            backupFile = Utils.General.backupFile
        ).apply {
            onCompleteListener { success, roomBackupMessage, code ->
                if (code !in listOf(
                        EXIT_CODE_ERROR_BY_USER_CANCELED,
                        // These error codes are less likely for restore via URI,
                        // but good to keep if the library might still emit them.
                        EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER,
                        EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
                    )
                ) {
                    val message = if (success) "Restore Success!" else "Restore Failed: $roomBackupMessage"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }

                if (success) {
                    // Restart the app after successful restore
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    this.restartApp(intent)
                }
            }
        }

        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { selectedUri ->
                // Ensure roomRestore is initialized (it should be by this point)
                if (::roomRestore.isInitialized) {
                    Log.d("MainActivity", "File selected: $selectedUri")
                    Toast.makeText(this, "Beginning restore.", Toast.LENGTH_SHORT).show()
                    // Call the method in LiftLabRoomBackup to handle the URI
                    roomRestore.handleSelectedFileToRestore(selectedUri)
                } else {
                    Toast.makeText(this, "Error: Restore process not ready.", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "roomRestore not initialized when file was selected.")
                }
            } ?: run {
                Toast.makeText(this, "No file selected.", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val isInitialized by LiftLabDatabase.initialized.collectAsState()
            if(isInitialized) {
                val donationViewModel: DonationViewModel = remember {
                    getViewModel(parameters = { parametersOf(BillingClient.newBuilder(this)) })
                }
                val donationState by donationViewModel.state.collectAsState()
                LiftLab(
                    donationState = donationState,
                    onClearBillingError = donationViewModel::clearBillingError,
                    onUpdateDonationProduct = donationViewModel::setNewDonationOption,
                    onBackup = {
                        roomBackup
                            .backup()
                    },
                    onRestore = {
                        if (::roomRestore.isInitialized) {
                            if (roomRestore.prepareRestore()) {
                                // The launcher is used here:
                                openDocumentLauncher.launch(LiftLabRoomBackup.MIME_TYPES_FOR_BACKUP)
                            } else {
                                Toast.makeText(this@MainActivity, "Failed to prepare for restore.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Error: Restore service not ready.", Toast.LENGTH_SHORT).show()
                            Log.e("MainActivity", "Attempted to restore but roomRestore not initialized.")
                        }
                    },
                    onProcessDonation = {
                        donationViewModel.processDonation(this)
                    },
                )
            }

            enableEdgeToEdge(statusBarStyle = getStatusBarStyle())
        }
    }

    override fun onStop() {
        super.onStop()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val programRepository: ProgramsRepository by inject()
            val workoutsRepository: WorkoutsRepository by inject()
            val workoutInProgressRepository: WorkoutInProgressRepository by inject()
            val restTimerInProgressRepository: RestTimerInProgressRepository by inject()

            // TODO: make this injectable via Koin
            val notificationHelper = NotificationHelper(
                programRepository = programRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgressRepository,
            )

            if (!notificationHelper.startRestTimerNotification(context)) {
                notificationHelper.startActiveWorkoutNotification(context)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val programRepository: ProgramsRepository by inject()
            val workoutsRepository: WorkoutsRepository by inject()
            val workoutInProgressRepository: WorkoutInProgressRepository by inject()
            val restTimerInProgressRepository: RestTimerInProgressRepository by inject()

            NotificationHelper(
                programRepository = programRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgressRepository,
            ).stopActiveNotifications(context)
        }
    }

    private fun requestNotificationPermission(context: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
    }

    @Composable
    private fun getStatusBarStyle(): SystemBarStyle = SystemBarStyle.run {
        val color = Color.Transparent.toArgb()
        dark(color)
    }
}