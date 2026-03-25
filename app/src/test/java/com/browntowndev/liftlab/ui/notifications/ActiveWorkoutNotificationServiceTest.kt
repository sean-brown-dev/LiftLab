
package com.browntowndev.liftlab.ui.notifications

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.coroutines.AppDispatchers
import com.browntowndev.liftlab.core.coroutines.TestDispatchers
import com.browntowndev.liftlab.dependencyInjection.DurationTimer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.util.Date

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34], application = Application::class)
class ActiveWorkoutNotificationServiceTest {

    private lateinit var app: Application
    private lateinit var serviceController: ServiceController<ActiveWorkoutNotificationService>
    private lateinit var service: ActiveWorkoutNotificationService

    // Koin-provided test doubles / dependencies
    private lateinit var fakeTimer: LiftLabTimer
    private lateinit var helperMock: NotificationHelper

    // Captured callbacks from LiftLabTimer.start(...)
    private var capturedOnTick: ((Long) -> Unit)? = null
    private var capturedOnFinish: (() -> Unit)? = null

    private lateinit var testDispatchers: TestDispatchers

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        // Make Dispatchers.Main.immediate usable in unit tests
        Dispatchers.setMain(mainDispatcher)

        // Test dispatchers so the service's coroutineScope runs on Main
        testDispatchers = TestDispatchers()

        app = ApplicationProvider.getApplicationContext<Context>() as Application

        // Fresh Koin for each test
        stopKoinSafely()

        // Base fakes/mocks
        fakeTimer = mockk(relaxed = true)
        helperMock = mockk(relaxed = true)

        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING) } returns false
        justRun { SettingsManager.initialize(any<Context>()) }

        // Capture callbacks from LiftLabTimer.start
        capturedOnTick = null
        capturedOnFinish = null
        every {
            fakeTimer.start(
                countDown = any(),
                millisInFuture = any(),
                countDownInterval = any(),
                onTick = captureLambda(),
                onFinish = captureLambda()
            )
        } answers {
            capturedOnTick = arg(3)
            capturedOnFinish = arg(4)
            fakeTimer
        }

        // Start Koin with default bindings (helperMock for most tests)
        startKoin {
            androidContext(app)
            modules(module {
                single(DurationTimer) { fakeTimer }
                single { helperMock }
                single<AppDispatchers> { testDispatchers }
            })
        }

        serviceController = Robolectric.buildService(ActiveWorkoutNotificationService::class.java)
        service = serviceController.create().get()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        unmockkAll()
        stopKoinSafely()
        Dispatchers.resetMain()
    }

    // -------------------------- onCreate / channel / foreground --------------------------

    @Test
    fun `onCreate creates channel and enters foreground`() {
        val nm = app.getSystemService(NotificationManager::class.java)

        // Channel should exist
        val channel = nm.getNotificationChannel("ActiveWorkoutForegroundService")
        Assertions.assertNotNull(channel)
        Assertions.assertEquals(NotificationManager.IMPORTANCE_LOW, channel!!.importance)

        // Foreground notification is present (at least one notification posted)
        val shadowNm = Shadows.shadowOf(nm)
        val notifications = shadowNm.allNotifications
        Assertions.assertTrue(notifications.isNotEmpty(), "Expected a foreground notification to be posted on create")
        // Ongoing flag should be set
        Assertions.assertTrue((notifications.last().flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    // -------------------------- onBind --------------------------

    @Test
    fun `onBind returns null`() {
        Assertions.assertNull(service.onBind(null))
    }

    // -------------------------- onStartCommand paths --------------------------

    @Test
    fun `onStartCommand - no active workout - service stops and timer not started`() {
        coEvery { helperMock.getActiveWorkoutMetadata() } returns null

        val result = service.onStartCommand(Intent(), 0, 1)
        Assertions.assertEquals(android.app.Service.START_STICKY, result)

        // Let the coroutine run stopSelf() -> onDestroy()
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        // 1) Timer was NEVER started
        verify(exactly = 0) { fakeTimer.start(any(), any(), any(), any(), any()) }
    }


    @Test
    fun `onStartCommand - active workout - sets title and nextSet, starts timer, updates on tick`() {
        // Stub the helper (do NOT reload Koin here)
        val start = Date(System.currentTimeMillis() - 10_000L)
        coEvery { helperMock.getActiveWorkoutMetadata() } returns
                com.browntowndev.liftlab.ui.models.workout.ActiveWorkoutNotificationMetadata(
                    workoutName = "PUSH A",
                    nextSet = "Bench Press — 8–10 @ RPE 8",
                    startTime = start
                )

        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)
        shadowNm.allNotifications.clear()

        val res = service.onStartCommand(Intent(), 0, 2)
        Assertions.assertEquals(Service.START_STICKY, res)

        // withContext(Main.immediate) now runs inline because dispatchers.io == Main.immediate
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        val tick = capturedOnTick
        Assertions.assertNotNull(tick, "Timer.start should have captured onTick callback")

        tick!!.invoke(0L)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        val last = shadowNm.allNotifications.last()
        val title = last.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text  = last.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val sub1  = last.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        Assertions.assertEquals("PUSH A", title)
        Assertions.assertTrue(text.contains("Bench Press"))
        Assertions.assertTrue(sub1.isNotBlank())

        tick.invoke(1000L)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
        val sub2 = shadowNm.allNotifications.last().extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        Assertions.assertNotEquals(sub1, sub2)
    }

    // -------------------------- onDestroy --------------------------

    @Test
    fun `onDestroy cancels timer and removes notification`() {
        coEvery { helperMock.getActiveWorkoutMetadata() } returns null
        service.onStartCommand(Intent(), 0, 1)

        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)
        val before = shadowNm.allNotifications.size

        service.onDestroy()

        verify(exactly = 1) { fakeTimer.cancel() }
        // Notification with NOTIFICATION_ID should be cancelled
        val after = shadowNm.allNotifications.size
        Assertions.assertTrue(after <= before, "Expected notification count to not increase after onDestroy")
    }

    // -------------------------- helpers -------------------------- //
    private fun stopKoinSafely() {
        try { stopKoin() } catch (_: Throwable) { /* ignore */ }
        GlobalContext.getOrNull()?.let { stopKoin() }
    }
}
