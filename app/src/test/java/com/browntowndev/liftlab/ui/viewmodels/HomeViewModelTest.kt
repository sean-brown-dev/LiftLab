
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.models.metrics.ConfiguredMetricsState
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteLiftMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteVolumeMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetConfiguredMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.InsertManyLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.UpsertManyVolumeMetricChartsUseCase
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.viewmodels.home.HomeViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import dev.gitlive.firebase.auth.FirebaseUser as KmpFirebaseUser

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @RelaxedMockK lateinit var getConfiguredMetricsStateFlowUseCase: GetConfiguredMetricsStateFlowUseCase
    @RelaxedMockK lateinit var upsertManyVolumeMetricChartsUseCase: UpsertManyVolumeMetricChartsUseCase
    @RelaxedMockK lateinit var deleteVolumeMetricChartByIdUseCase: DeleteVolumeMetricChartByIdUseCase
    @RelaxedMockK lateinit var deleteLiftMetricChartByIdUseCase: DeleteLiftMetricChartByIdUseCase
    @RelaxedMockK lateinit var insertManyLiftMetricChartsUseCase: InsertManyLiftMetricChartsUseCase
    @RelaxedMockK lateinit var firebaseAuth: FirebaseAuth
    @RelaxedMockK lateinit var eventBus: EventBus

    private val mainDispatcher = StandardTestDispatcher()

    private var settingsNavigated = false
    private var liftLibraryNavigatedIds: List<Long>? = null
    private var userLoggedIn = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Crashlytics static
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        // Keep init simple; we don't rely on the flow mapping in these tests
        every { getConfiguredMetricsStateFlowUseCase.invoke() } returns flowOf(ConfiguredMetricsState())
        // We intentionally do NOT mock the Firebase authStateFlow extension; if it doesn't emit,
        // combine() won't produce, but our tests don't depend on that emission.

        settingsNavigated = false
        liftLibraryNavigatedIds = null
        userLoggedIn = false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    private fun newViewModel(): HomeViewModel {
        return HomeViewModel(
            getConfiguredMetricsStateFlowUseCase = getConfiguredMetricsStateFlowUseCase,
            upsertManyVolumeMetricChartsUseCase = upsertManyVolumeMetricChartsUseCase,
            deleteVolumeMetricChartByIdUseCase = deleteVolumeMetricChartByIdUseCase,
            deleteLiftMetricChartByIdUseCase = deleteLiftMetricChartByIdUseCase,
            insertManyLiftMetricChartsUseCase = insertManyLiftMetricChartsUseCase,
            onNavigateToSettingsMenu = { settingsNavigated = true },
            onNavigateToLiftLibrary = { ids -> liftLibraryNavigatedIds = ids },
            onUserLoggedIn = { userLoggedIn = true },
            firebaseAuth = firebaseAuth,
            eventBus = eventBus
        ).also {
            mainDispatcher.scheduler.advanceUntilIdle()
        }
    }

    // ---------- Top bar actions ----------

    @Test
    fun openSettingsMenu_callsCallback() = runTest {
        val vm = newViewModel()
        assertFalse(settingsNavigated)
        vm.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenSettingsMenu))
        assertTrue(settingsNavigated)
    }

    @Test
    fun openProfileMenu_togglesLoginModal() = runTest {
        val vm = newViewModel()
        assertFalse(vm.state.value.loginModalVisible)
        vm.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenProfileMenu))
        assertTrue(vm.state.value.loginModalVisible)
        vm.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenProfileMenu))
        assertFalse(vm.state.value.loginModalVisible)
    }

    // ---------- Simple toggles ----------

    @Test
    fun toggleLoginModal_flipsFlag() = runTest {
        val vm = newViewModel()
        val initial = vm.state.value.loginModalVisible
        vm.toggleLoginModal()
        assertEquals(!initial, vm.state.value.loginModalVisible)
    }

    @Test
    fun toggleLiftChartPicker_togglesAndResetsSelections() = runTest {
        val vm = newViewModel()

        // First toggle: show picker and clear selections
        vm.toggleLiftChartPicker()
        assertTrue(vm.state.value.showLiftChartPicker)
        assertTrue(vm.state.value.volumeTypeSelections.isEmpty())
        assertTrue(vm.state.value.liftChartTypeSelections.isEmpty())
        assertNull(vm.state.value.volumeImpactSelection)

        // Second toggle: hide picker and keep cleared
        vm.toggleLiftChartPicker()
        assertFalse(vm.state.value.showLiftChartPicker)
        assertTrue(vm.state.value.volumeTypeSelections.isEmpty())
        assertTrue(vm.state.value.liftChartTypeSelections.isEmpty())
        assertNull(vm.state.value.volumeImpactSelection)
    }

    // ---------- Delete actions ----------

    @Test
    fun deleteLiftMetricChart_delegatesToUseCase() = runTest {
        val vm = newViewModel()
        coEvery { deleteLiftMetricChartByIdUseCase(55L) } returns 1

        vm.deleteLiftMetricChart(55L)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deleteLiftMetricChartByIdUseCase(55L) }
    }

    @Test
    fun deleteVolumeMetricChart_delegatesToUseCase() = runTest {
        val vm = newViewModel()
        coEvery { deleteVolumeMetricChartByIdUseCase(66L) } returns 1

        vm.deleteVolumeMetricChart(66L)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deleteVolumeMetricChartByIdUseCase(66L) }
    }

    // ---------- Auth: create account ----------

    @Test
    fun createAccount_success_sendsVerification_andCallsHandleCompletion() = runTest {
        val vm = newViewModel()

        val firebaseUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
        every { firebaseUser.sendEmailVerification() } returns mockk(relaxed = true)

        val authResult = mockk<AuthResult>(relaxed = true)
        every { authResult.user } returns firebaseUser

        val task = mockk<Task<AuthResult>>(relaxed = true)
        every { task.isSuccessful } returns true
        every { task.result } returns authResult
        every { task.exception } returns null
        every { task.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
            firstArg<OnCompleteListener<AuthResult>>().onComplete(task)
            task
        }

        every { firebaseAuth.createUserWithEmailAndPassword("a@b.com", "pw") } returns task

        vm.createAccount("a@b.com", "pw")
        mainDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { firebaseUser.sendEmailVerification() }
        assertTrue(userLoggedIn) // handleFirebaseTaskCompletion calls onUserLoggedIn when user != null
    }

    @Test
    fun createAccount_failure_setsError() = runTest {
        val vm = newViewModel()

        val task = mockk<Task<AuthResult>>(relaxed = true)
        every { task.isSuccessful } returns false
        every { task.exception } returns RuntimeException("boom")
        every { task.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
            firstArg<OnCompleteListener<AuthResult>>().onComplete(task)
            task
        }

        every { firebaseAuth.createUserWithEmailAndPassword("a@b.com", "pw") } returns task

        vm.createAccount("a@b.com", "pw")
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to authenticate user.", vm.state.value.firebaseError)
    }

    // ---------- Auth: login/logout ----------

    @Test
    fun login_success_callsHandleCompletion() = runTest {
        val vm = newViewModel()

        val authResult = mockk<AuthResult>(relaxed = true)
        every { authResult.user } returns mockk(relaxed = true)

        val task = mockk<Task<AuthResult>>(relaxed = true)
        every { task.isSuccessful } returns true
        every { task.result } returns authResult
        every { task.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
            firstArg<OnCompleteListener<AuthResult>>().onComplete(task)
            task
        }

        every { firebaseAuth.signInWithEmailAndPassword("a@b.com", "pw") } returns task

        vm.login("a@b.com", "pw")
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(userLoggedIn)
    }

    @Test
    fun login_failure_setsError() = runTest {
        val vm = newViewModel()

        val task = mockk<Task<AuthResult>>(relaxed = true)
        every { task.isSuccessful } returns false
        every { task.exception } returns RuntimeException("nope")
        every { task.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
            firstArg<OnCompleteListener<AuthResult>>().onComplete(task)
            task
        }

        every { firebaseAuth.signInWithEmailAndPassword("a@b.com", "pw") } returns task

        vm.login("a@b.com", "pw")
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to authenticate user.", vm.state.value.firebaseError)
    }

    @Test
    fun logout_callsFirebaseSignOut() = runTest {
        val vm = newViewModel()
        every { firebaseAuth.signOut() } just Runs

        vm.logout()
        mainDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    // ---------- Auth: Google sign-in ----------

    @Test
    fun signInWithGoogle_success_nonNullUser_callsOnUserLoggedIn() = runTest {
        val vm = newViewModel()
        val kmpUser = mockk<KmpFirebaseUser>(relaxed = true)

        vm.signInWithGoogle(Result.success(kmpUser))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(userLoggedIn)
        assertNull(vm.state.value.firebaseError)
    }

    @Test
    fun signInWithGoogle_success_nullUser_setsErrorAndClearsFields() = runTest {
        val vm = newViewModel()

        vm.signInWithGoogle(Result.success(null))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Authentication successful, but user data is unavailable.", vm.state.value.firebaseError)
        assertNull(vm.state.value.firebaseUsername)
        assertFalse(vm.state.value.emailVerified)
    }

    @Test
    fun signInWithGoogle_failure_setsErrorAndClearsFields() = runTest {
        val vm = newViewModel()

        vm.signInWithGoogle(Result.failure(RuntimeException("bad")))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to authenticate user.", vm.state.value.firebaseError)
        assertNull(vm.state.value.firebaseUsername)
        assertFalse(vm.state.value.emailVerified)
    }
}
