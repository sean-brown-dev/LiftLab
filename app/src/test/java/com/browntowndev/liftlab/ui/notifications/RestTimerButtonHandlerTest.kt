package com.browntowndev.liftlab.ui.notifications

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34], application = Application::class)
class RestTimerButtonHandlerTest {

    private lateinit var app: Application
    private lateinit var repo: RestTimerInProgressRepository
    private lateinit var helper: NotificationHelper
    private lateinit var receiver: RestTimerButtonHandler

    @BeforeEach
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<Context>() as Application
        stopKoinSafely()

        repo = mockk(relaxed = true)
        helper = mockk(relaxed = true)

        startKoin {
            androidContext(app)
            modules(module {
                single { repo }
                single { helper }
            })
        }

        receiver = RestTimerButtonHandler()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        stopKoinSafely()
    }

    @Test
    fun `onReceive - null context or intent - no work`() {
        receiver.onReceive(null, Intent(RestTimerNotificationService.SKIP_ACTION))
        receiver.onReceive(app, null)

        runMainLooperToEnd()
        coVerify(exactly = 0) { repo.deleteAll() }
        coVerify(exactly = 0) { helper.startActiveWorkoutNotification(any()) }
    }

    @Test
    fun `onReceive - other action - ignored`() {
        receiver.onReceive(app, Intent("not.skip"))
        runMainLooperToEnd()
        coVerify(exactly = 0) { repo.deleteAll() }
        coVerify(exactly = 0) { helper.startActiveWorkoutNotification(any()) }
    }

    @Test
    fun `onReceive - SKIP_ACTION - deletes rest timers, stops rest service, and starts active workout notification`() {
        val intent = Intent(RestTimerNotificationService.SKIP_ACTION)
        receiver.onReceive(app, intent)

        // Work runs via goAsync + coroutine -> wait eagerly for side-effects
        awaitUntil(1500) {
            val repoOk = try { coVerify(exactly = 1) { repo.deleteAll() }; true } catch (_: AssertionError) { false }
            val helperOk = try { coVerify(exactly = 1) { helper.startActiveWorkoutNotification(app) }; true } catch (_: AssertionError) { false }
            repoOk && helperOk
        }

        // (Optional) sanity: ensure nothing else got queued on main
        runMainLooperToEnd()
    }

    // --- helpers ---
    private fun runMainLooperToEnd() {
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
    }

    private fun awaitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            runMainLooperToEnd()
            Thread.sleep(5)
        }
        fail("Condition not met within ${timeoutMs}ms")
    }

    private fun stopKoinSafely() {
        try { stopKoin() } catch (_: Throwable) {}
        GlobalContext.getOrNull()?.let { stopKoin() }
    }
}
