package com.browntowndev.liftlab

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.backupFile
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.notifications.ActiveWorkoutNotificationService
import com.browntowndev.liftlab.core.notifications.NotificationHelper
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BY_USER_CANCELED
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_SUCCESS
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {
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

        val roomBackup: RoomBackup =
            RoomBackup(this@MainActivity)
                .database(db)
                .enableLogDebug(false)
                .backupIsEncrypted(true)
                .customEncryptPassword(this@MainActivity.getString(R.string.db_encryption_key))
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(backupFile)
                .apply {
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

        val roomRestore: RoomBackup =
            RoomBackup(this@MainActivity)
                .database(db)
                .enableLogDebug(false)
                .backupIsEncrypted(true)
                .customEncryptPassword(this@MainActivity.getString(R.string.db_encryption_key))
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .customRestoreDialogTitle("Choose a backup to restore.")
                .apply {
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

        setContent {
            val isInitialized by LiftLabDatabase.initialized.collectAsState()
            if(isInitialized) {
                KoinAndroidContext {
                    val donationViewModel: DonationViewModel = koinViewModel {
                        parametersOf(BillingClient.newBuilder(this))
                    }
                    val donationState by donationViewModel.state.collectAsState()
                    LiftLab(
                        donationState = donationState,
                        onClearBillingError = donationViewModel::clearBillingError,
                        onUpdateDonationProduct = donationViewModel::setNewDonationOption,
                        onBackup = roomBackup::backup,
                        onRestore = roomRestore::restore,
                        onProcessDonation = {
                            donationViewModel.processDonation(this)
                        },
                    )
                }
            }
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
}