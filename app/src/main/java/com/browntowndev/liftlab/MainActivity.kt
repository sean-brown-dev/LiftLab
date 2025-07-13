package com.browntowndev.liftlab

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.notifications.NotificationHelper
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.persistence.sync.FirebaseSyncManager
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !LiftLabDatabase.initialized.value
            }
        }

        requestNotificationPermission(this)
        SettingsManager.initialize(this@MainActivity)

        val mutableSyncState = MutableStateFlow(false)
        val syncState = mutableSyncState.asStateFlow()
        val syncManager: FirebaseSyncManager by inject()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                //syncManager.syncAll()
            }
            mutableSyncState.value = true
        }

        setContent {
            val isInitialized by LiftLabDatabase.initialized.collectAsState()
            val isSyncComplete by syncState.collectAsState()

            if(isInitialized && isSyncComplete) {
                val donationViewModel: DonationViewModel = remember {
                    getViewModel(parameters = { parametersOf(BillingClient.newBuilder(this)) })
                }
                val donationState by donationViewModel.state.collectAsState()
                LiftLab(
                    donationState = donationState,
                    onClearBillingError = donationViewModel::clearBillingError,
                    onUpdateDonationProduct = donationViewModel::setNewDonationOption,
                    onProcessDonation = {
                        donationViewModel.processDonation(this)
                    },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(.5f),
                        )
                        Text(
                            text = "Syncing Data...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
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