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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.data.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.FirestoreSyncViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {
    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firestoreSyncViewModel: FirestoreSyncViewModel by inject()
        firestoreSyncViewModel.syncAll()

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !firestoreSyncViewModel.syncState.value.syncing
            }
        }

        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)

        setContent {
            val syncState by firestoreSyncViewModel.syncState.collectAsState()

            val donationViewModel: DonationViewModel = remember {
                getViewModel(parameters = { parametersOf(BillingClient.newBuilder(this)) })
            }
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
                onCloseSyncFailedDialog = firestoreSyncViewModel::toggleSyncErrorDialog,
                onBeginSync = firestoreSyncViewModel::syncAll,
            )

            enableEdgeToEdge(getStatusBarStyle())
        }
    }

    override fun onStop() {
        super.onStop()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val programRepository: ProgramsRepository by inject()
            val workoutsRepositoryImpl: WorkoutsRepositoryImpl by inject()
            val workoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl by inject()
            val restTimerInProgressRepository: RestTimerInProgressRepository by inject()

            // TODO: make this injectable via Koin
            val notificationHelper = NotificationHelper(
                programRepository = programRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
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
            val workoutsRepositoryImpl: WorkoutsRepositoryImpl by inject()
            val workoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl by inject()
            val restTimerInProgressRepository: RestTimerInProgressRepository by inject()

            NotificationHelper(
                programRepository = programRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
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