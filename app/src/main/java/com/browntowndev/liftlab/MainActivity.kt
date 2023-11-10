package com.browntowndev.liftlab

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.ui.notifications.RestTimerNotificationService
import com.browntowndev.liftlab.ui.views.LiftLab
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener.Companion.EXIT_CODE_ERROR_BY_USER_CANCELED
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)
        val db = LiftLabDatabase.getInstance(this@MainActivity)

        val roomBackup: RoomBackup =
            RoomBackup(this@MainActivity)
                .database(db)
                .enableLogDebug(false)
                .backupIsEncrypted(true)
                .customEncryptPassword(this@MainActivity.getString(R.string.db_encryption_key))
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .customRestoreDialogTitle("Please choose a backup to restore.")
                .apply {
                    onCompleteListener { success, _, code ->
                        if (code != EXIT_CODE_ERROR_BY_USER_CANCELED &&
                            code != EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER &&
                            code != EXIT_CODE_ERROR_BACKUP_FILE_CREATOR) {
                            val message = if (success) "Success!" else "Error!"
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
            val isDbInitialized by LiftLabDatabase.initialized.collectAsState()
            if(isDbInitialized) {
                KoinAndroidContext {
                    LiftLab(roomBackup)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val restTimeRemaining: Long = getRestTimeRemaining()

            if (restTimeRemaining > 0L) {
                val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                restTimerIntent.putExtra(
                    RestTimerNotificationService.EXTRA_COUNT_DOWN_FROM,
                    restTimeRemaining
                )
                context.startForegroundService(restTimerIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val context = super.getApplicationContext()

        val restTimeRepo: RestTimerInProgressRepository by inject()
        lifecycleScope.launch {
            val restTimeRemaining: Long = getRestTimeRemaining()
            if (restTimeRemaining <= 0L) {
                restTimeRepo.deleteAll()
            }

            val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
            context.stopService(restTimerIntent)
        }
    }

    private suspend fun getRestTimeRemaining(): Long {
        val restTimeRepo: RestTimerInProgressRepository by inject()
        val inProgressRestTimer = restTimeRepo.get()

        val restTimeRemaining = if (inProgressRestTimer != null) {
            val totalRestTime = inProgressRestTimer.restTime
            val timeElapsed = Utils.getCurrentDate().time - inProgressRestTimer.timeStartedInMillis
            totalRestTime - timeElapsed
        } else 0L

        Log.d(Log.DEBUG.toString(), "Rest time remaining: $restTimeRemaining")
        return restTimeRemaining
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