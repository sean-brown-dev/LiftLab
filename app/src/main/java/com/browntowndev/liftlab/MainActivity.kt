package com.browntowndev.liftlab

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.ui.infrastructure.WalCaretaker
import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import com.browntowndev.liftlab.ui.viewmodels.donation.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.remoteSync.RemoteSyncViewModel
import com.browntowndev.liftlab.ui.viewmodels.startup.StartupViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {
    val startupViewModel: StartupViewModel by inject()
    val remoteSyncViewModel: RemoteSyncViewModel by inject()
    val donationViewModel: DonationViewModel by inject()
    val notificationHelper: NotificationHelper by inject()
    val db: LiftLabDatabase by inject()

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            getKoin().get<WalCaretaker>()
        )

        installSplashScreen().setKeepOnScreenCondition {
            !startupViewModel.initialized.value ||
                remoteSyncViewModel.syncState.value.syncing
        }

        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)
        startupViewModel.beginInitializationCheck {
            remoteSyncViewModel.syncAllSuspending()
        }

        setContent {
            val initialized by startupViewModel.initialized.collectAsState()
            val syncState by remoteSyncViewModel.syncState.collectAsState()
            val donationState by donationViewModel.state.collectAsState()

            LiftLab(
                initializing = syncState.syncing || !initialized,
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
        if (isChangingConfigurations) return  // don't run during rotation, split-screen toggles, etc.

        val appCtx = applicationContext

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (!notificationHelper.startRestTimerNotification(appCtx)) {
                    notificationHelper.startActiveWorkoutNotification(appCtx)
                }
            }.onFailure { e ->
                Log.w("MainActivity", "Failed to start workout/rest notifications", e)
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
        if (ContextCompat.checkSelfPermission(
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