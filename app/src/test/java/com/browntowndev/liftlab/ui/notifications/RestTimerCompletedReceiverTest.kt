package com.browntowndev.liftlab.ui.notifications

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import io.mockk.coVerify
import io.mockk.mockk
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
class RestTimerCompletedReceiverTest {

    private lateinit var app: Application
    private lateinit var repo: RestTimerInProgressRepository
    private lateinit var receiver: RestTimerCompletedReceiver

    @BeforeEach
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<Context>() as Application
        stopKoinSafely()

        repo = mockk(relaxed = true)

        startKoin {
            androidContext(app)
            modules(module {
                single { repo }
            })
        }

        receiver = RestTimerCompletedReceiver()
    }

    @AfterEach
    fun tearDown() {
        stopKoinSafely()
    }

    @Test
    fun `onReceive - ignores non-matching action`() {
        receiver.onReceive(app, Intent("not.the.action"))
        // idle main, though work should not be scheduled
        runMainLooperToEnd()
        coVerify(exactly = 0) { repo.delete() }
    }

    @Test
    fun `onReceive - matching action deletes all rest timers`() {
        val intent = Intent(RestTimerCompletedReceiver.ACTION)
        receiver.onReceive(app, intent)

        // The work is launched via goAsync + coroutine -> wait briefly/eagerly
        awaitUntil(1000) {
            try { coVerify(exactly = 1) { repo.delete() }; true } catch (_: AssertionError) { false }
        }
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
