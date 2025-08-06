package com.browntowndev.liftlab

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.RemoteSyncViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {
    val remoteSyncViewModel: RemoteSyncViewModel by inject()
    val donationViewModel: DonationViewModel by inject()

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setKeepOnScreenCondition {
            remoteSyncViewModel.syncState.value.syncing
        }

        remoteSyncViewModel.syncAll()
        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)

        setContent {
            val syncState by remoteSyncViewModel.syncState.collectAsState()
            val donationState by donationViewModel.state.collectAsState()
            LiftLab(
                initializing = syncState.syncing,
                showSyncFailedDialog = syncState.showSyncFailedDialog,
                donationState = donationState,
                onClearBillingError = donationViewModel::clearBillingError,
                onUpdateDonationProduct = donationViewModel::setNewDonationOption,
                onProcessDonation = {
                    donationViewModel.processDonation(this)
                },
                onCloseSyncFailedDialog = remoteSyncViewModel::toggleSyncErrorDialog,
                onBeginSync = remoteSyncViewModel::syncAll,
            )

            enableEdgeToEdge(getStatusBarStyle())
        }
    }

    override fun onStop() {
        super.onStop()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val notificationHelper: NotificationHelper by inject()
            if (!notificationHelper.startRestTimerNotification(context)) {
                notificationHelper.startActiveWorkoutNotification(context)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val notificationHelper: NotificationHelper by inject()
            notificationHelper.stopActiveNotifications(context)
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