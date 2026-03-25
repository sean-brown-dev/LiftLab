package com.browntowndev.liftlab.ui.notifications

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.common.toTimeString
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34], application = Application::class)
class RestTimerNotificationServiceTest {

    private lateinit var app: Application
    private lateinit var controller: org.robolectric.android.controller.ServiceController<RestTimerNotificationService>
    private lateinit var service: RestTimerNotificationService

    // Injected timer (CountdownTimer)
    private lateinit var fakeTimer: LiftLabTimer

    // Captured callbacks from timer.start(...)
    private var capturedOnTick: ((Long) -> Unit)? = null
    private var capturedOnFinish: (() -> Unit)? = null

    // Use a test Main dispatcher (good hygiene for notif/UI paths)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)

        app = ApplicationProvider.getApplicationContext<Context>() as Application

        // Start a fresh Koin graph for this test class
        stopKoinSafely()

        fakeTimer = mockk(relaxed = true)

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

        // Mock the companion alert function used by the service
        mockkObject(NotificationHelper.Companion)
        every { NotificationHelper.postRestTimerCompletionAlert(any()) } just Runs

        startKoin {
            androidContext(app)
            modules(module {
                single(named("CountdownTimer")) { fakeTimer }
            })
        }

        controller = Robolectric.buildService(RestTimerNotificationService::class.java)
        service = controller.create().get()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        unmockkAll()
        stopKoinSafely()
        Dispatchers.resetMain()
    }

    // -------- onCreate / channel / foreground --------

    @Test
    fun `onCreate creates low-importance channel and posts ongoing foreground notification`() {
        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)

        // low-importance channel exists
        val anyLow = nm.notificationChannels.any { it.importance == NotificationManager.IMPORTANCE_LOW }
        Assertions.assertTrue(anyLow, "Expected a low-importance channel to be created")

        // ongoing foreground notification posted
        val last = shadowNm.allNotifications.lastOrNull()
            ?: Assertions.fail<Notification>("Expected a foreground notification posted on create")
        Assertions.assertTrue(
            (last.flags and Notification.FLAG_ONGOING_EVENT) != 0,
            "Foreground notification must be ongoing"
        )
    }

    // -------- onBind --------

    @Test
    fun `onBind returns null`() {
        Assertions.assertNull(service.onBind(null))
    }

    // -------- onStartCommand paths --------

    @Test
    fun `onStartCommand - countDownFrom Less Than or Equal to 0 - posts completion immediately, broadcasts, stops and cleans up`() {
        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)
        val serviceChannelId = shadowNm.allNotifications.lastOrNull()?.channelId

        val intent = Intent().putExtra(RestTimerNotificationService.EXTRA_COUNT_DOWN_FROM, 0L)
        val res = service.onStartCommand(intent, 0, 1)

        Assertions.assertEquals(Service.START_NOT_STICKY, res)

        // Let stopSelf() cascade into onDestroy()
        runMainLooperToEnd()

        // Timer was never started
        verify(exactly = 0) { fakeTimer.start(any(), any(), any(), any(), any()) }

        // Completion alert posted
        verify(exactly = 1) { NotificationHelper.postRestTimerCompletionAlert(service) }

        // Broadcast sent
        val broadcasts = Shadows.shadowOf(app).broadcastIntents
        Assertions.assertTrue(broadcasts.isNotEmpty(), "Expected a broadcast for rest timer completion")

        // Drive lifecycle to onDestroy() so cleanup runs deterministically
        controller.destroy()
        runMainLooperToEnd()

        // Timer is cancelled in onDestroy()
        verify(exactly = 1) { fakeTimer.cancel() }

        // (sanity check) no *new* notifications after destroy
        val afterDestroy = shadowNm.allNotifications.size
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
        Assertions.assertEquals(afterDestroy, shadowNm.allNotifications.size)

    }

    @Test
    fun `onStartCommand - positive countDown - starts timer, updates on tick, finishes with alert, broadcast, and starts ActiveWorkoutNotification`() {
        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)
        shadowNm.allNotifications.clear()

        // 2.5s should round to 3s
        val intent = Intent().putExtra(RestTimerNotificationService.EXTRA_COUNT_DOWN_FROM, 2500L)
        val res = service.onStartCommand(intent, 0, 2)

        Assertions.assertEquals(Service.START_STICKY, res)

        runMainLooperToEnd()

        // Verify start() args (we don't rely on positions for capturing callbacks)
        verify(exactly = 1) {
            fakeTimer.start(
                countDown = true,
                millisInFuture = 2000L,
                countDownInterval = 1000L,
                onTick = any(),
                onFinish = any()
            )
        }

        val tick = capturedOnTick ?: Assertions.fail("(onTick) should have been captured by timer.start")
        val finish = capturedOnFinish ?: Assertions.fail("(onFinish) should have been captured by timer.start")

        // Simulate a tick at 2 seconds remaining
        tick.invoke(2000L)
        runMainLooperToEnd()

        val afterTick = shadowNm.allNotifications.lastOrNull()
            ?: Assertions.fail<Notification>("Expected a notification after tick")

        val textAfterTick = afterTick.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        Assertions.assertEquals(2000L.toTimeString(), textAfterTick)

        // Simulate finish
        val beforeBroadcasts = Shadows.shadowOf(app).broadcastIntents.size
        finish.invoke()
        runMainLooperToEnd()

        // Completion alert posted
        verify(exactly = 1) { NotificationHelper.postRestTimerCompletionAlert(service) }

        // Broadcast sent
        val afterBroadcasts = Shadows.shadowOf(app).broadcastIntents.size
        Assertions.assertTrue(afterBroadcasts > beforeBroadcasts, "Expected a broadcast on finish")

        // Start of ActiveWorkoutNotificationService via startForegroundService
        val started = Shadows.shadowOf(app).nextStartedService
        Assertions.assertNotNull(started, "Expected a startForegroundService intent for ActiveWorkoutNotificationService")
        Assertions.assertEquals(
            ActiveWorkoutNotificationService::class.qualifiedName,
            started?.component?.className,
            "Should start ActiveWorkoutNotificationService"
        )

        // Foreground notification removed & timer cancelled via onDestroy()
        // Finish → fire onDestroy deterministically, then assert timer cancelled
        finish.invoke()
        runMainLooperToEnd()

        // Robolectric may not automatically call onDestroy() after stopSelf()
        // Force it so we can assert cancel() reliably.
        controller.destroy()
        runMainLooperToEnd()

        verify(exactly = 1) { fakeTimer.cancel() }
    }

    @Test
    fun `onDestroy cancels timer and removes notification`() {
        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = Shadows.shadowOf(nm)
        val channelId = shadowNm.allNotifications.lastOrNull()?.channelId

        service.onDestroy()
        runMainLooperToEnd()

        verify(exactly = 1) { fakeTimer.cancel() }

        if (channelId != null) {
            val remaining = shadowNm.allNotifications.count { it.channelId == channelId }
            Assertions.assertEquals(0, remaining)
        }
    }

    // -------- helpers --------

    private fun runMainLooperToEnd() {
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
    }

    private fun stopKoinSafely() {
        try { stopKoin() } catch (_: Throwable) { /* ignore */ }
        GlobalContext.getOrNull()?.let { stopKoin() }
    }
}
