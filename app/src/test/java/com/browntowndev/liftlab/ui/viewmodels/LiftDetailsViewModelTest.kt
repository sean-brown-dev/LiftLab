
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.AddVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.CreateLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetLiftWithHistoryStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.RemoveVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateLiftNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateMovementPatternUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateVolumeTypeUseCase
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.LiftUiModel
import com.browntowndev.liftlab.ui.viewmodels.liftDetails.LiftDetailsState
import com.browntowndev.liftlab.ui.viewmodels.liftDetails.LiftDetailsViewModel
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
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.browntowndev.liftlab.core.domain.models.workout.Lift as DomainLift

@OptIn(ExperimentalCoroutinesApi::class)
class LiftDetailsViewModelTest {

    // Use cases
    @RelaxedMockK
    lateinit var updateLiftNameUseCase: UpdateLiftNameUseCase
    @RelaxedMockK lateinit var updateMovementPatternUseCase: UpdateMovementPatternUseCase
    @RelaxedMockK lateinit var updateVolumeTypeUseCase: UpdateVolumeTypeUseCase
    @RelaxedMockK lateinit var addVolumeTypeUseCase: AddVolumeTypeUseCase
    @RelaxedMockK lateinit var removeVolumeTypeUseCase: RemoveVolumeTypeUseCase
    @RelaxedMockK lateinit var createLiftUseCase: CreateLiftUseCase
    @RelaxedMockK lateinit var getLiftWithHistoryStateFlowUseCase: GetLiftWithHistoryStateFlowUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: LiftDetailsViewModel
    private val mainDispatcher = StandardTestDispatcher()

    private lateinit var crashlytics: FirebaseCrashlytics

    private var navigatedBack = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Crashlytics static
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics

        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        // Default: no emissions from init flow
        every { getLiftWithHistoryStateFlowUseCase.invoke(any()) } returns emptyFlow()

        navigatedBack = false

        viewModel = LiftDetailsViewModel(
            liftId = 42L,
            updateLiftNameUseCase = updateLiftNameUseCase,
            updateMovementPatternUseCase = updateMovementPatternUseCase,
            updateVolumeTypeUseCase = updateVolumeTypeUseCase,
            addVolumeTypeUseCase = addVolumeTypeUseCase,
            removeVolumeTypeUseCase = removeVolumeTypeUseCase,
            createLiftUseCase = createLiftUseCase,
            getLiftWithHistoryStateFlowUseCase = getLiftWithHistoryStateFlowUseCase,
            onNavigateBack = { navigatedBack = true },
            eventBus = eventBus
        )
        mainDispatcher.scheduler.advanceUntilIdle()

        // Seed state.lift so guarded actions proceed
        injectLift(
            LiftUiModel(
                id = 1L,
                name = "Bench Press",
                movementPattern = MovementPattern.entries.first(),
                volumeTypes = emptyList(),
                secondaryVolumeTypes = emptyList(),
                incrementOverride = null,
                restTime = null,
                restTimerEnabled = false,
                isBodyweight = false,
                note = null
            )
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    private fun injectLift(lift: LiftUiModel?) {
        val field = LiftDetailsViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<LiftDetailsState>
        flow.update { it.copy(lift = lift) }
    }

    // ---------- Top bar events ----------

    @Test
    fun navigatedBack_callsCallback() = runTest {
        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(navigatedBack)
    }

    @Test
    fun confirmCreateNewLift_callsUseCase_withMappedDomain_andNavigatesBack() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()

        val slot = slot<DomainLift>()
        coEvery { createLiftUseCase(capture(slot)) } returns 1

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.ConfirmCreateNewLift))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { createLiftUseCase(any()) }
        assertTrue(navigatedBack)
    }

    // ---------- Guard: when lift is null ----------

    @Test
    fun guardedActions_whenLiftNull_doNotCallUseCases_andLogCrash() = runTest {
        // Remove the lift
        injectLift(null)

        viewModel.updateName("New Name")
        viewModel.addVolumeType(VolumeType.entries.first())
        viewModel.removeVolumeType(VolumeType.entries.first())
        viewModel.updateMovementPattern(MovementPattern.entries.first())
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateLiftNameUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { addVolumeTypeUseCase.invoke(any(), any(), any()) }
        coVerify(exactly = 0) { removeVolumeTypeUseCase.invoke(any(), any(), any()) }
        coVerify(exactly = 0) { updateMovementPatternUseCase.invoke(any(), any()) }

        verify(atLeast = 1) { crashlytics.recordException(any()) }
    }

    // ---------- Delegations (verify correct domain is passed) ----------

    @Test
    fun updateName_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        coEvery { updateLiftNameUseCase(capture(slot), any()) } returns expectedDomain

        viewModel.updateName("New Name")
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { updateLiftNameUseCase(any(), "New Name") }
    }

    @Test
    fun updateMovementPattern_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val mp = MovementPattern.entries.last()
        coEvery { updateMovementPatternUseCase(capture(slot), mp) } returns expectedDomain

        viewModel.updateMovementPattern(mp)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { updateMovementPatternUseCase(any(), mp) }
    }

    @Test
    fun addVolumeType_primary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { addVolumeTypeUseCase(capture(slot), vt, VolumeTypeCategory.PRIMARY) } returns expectedDomain

        viewModel.addVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { addVolumeTypeUseCase(any(), vt, VolumeTypeCategory.PRIMARY) }
    }

    @Test
    fun addVolumeType_secondary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { addVolumeTypeUseCase(capture(slot), vt, VolumeTypeCategory.SECONDARY) } returns expectedDomain

        viewModel.addSecondaryVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { addVolumeTypeUseCase(any(), vt, VolumeTypeCategory.SECONDARY) }
    }

    @Test
    fun removeVolumeType_primary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { removeVolumeTypeUseCase(capture(slot), vt, VolumeTypeCategory.PRIMARY) } returns expectedDomain

        viewModel.removeVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { removeVolumeTypeUseCase(any(), vt, VolumeTypeCategory.PRIMARY) }
    }

    @Test
    fun removeVolumeType_secondary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { removeVolumeTypeUseCase(capture(slot), vt, VolumeTypeCategory.SECONDARY) } returns expectedDomain

        viewModel.removeSecondaryVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { removeVolumeTypeUseCase(any(), vt, VolumeTypeCategory.SECONDARY) }
    }

    @Test
    fun updateVolumeType_primary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { updateVolumeTypeUseCase(capture(slot), 0, vt, VolumeTypeCategory.PRIMARY) } returns expectedDomain

        viewModel.updateVolumeType(index = 0, newVolumeType = vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { updateVolumeTypeUseCase(any(), 0, vt, VolumeTypeCategory.PRIMARY) }
    }

    @Test
    fun updateVolumeType_secondary_delegatesToUseCase_withMappedDomain() = runTest {
        val ui = viewModel.state.value.lift!!
        val expectedDomain = ui.toDomainModel()
        val slot = slot<DomainLift>()
        val vt = VolumeType.entries.first()
        coEvery { updateVolumeTypeUseCase(capture(slot), 1, vt, VolumeTypeCategory.SECONDARY) } returns expectedDomain

        viewModel.updateSecondaryVolumeType(index = 1, newVolumeType = vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedDomain, slot.captured)
        coVerify(exactly = 1) { updateVolumeTypeUseCase(any(), 1, vt, VolumeTypeCategory.SECONDARY) }
    }

    // ---------- State-mutation tests: use real mapping by returning real DomainLift from use cases ----------

    @Test
    fun updateName_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = "NEW",
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = baseDomain.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = baseDomain.secondaryVolumeTypesBitmask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { updateLiftNameUseCase(any(), "NEW") } returns domainAfter

        viewModel.updateName("NEW")
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("NEW", viewModel.state.value.lift?.name)
    }

    @Test
    fun updateMovementPattern_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val newMp = MovementPattern.entries.last()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = newMp,
            volumeTypesBitmask = baseDomain.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = baseDomain.secondaryVolumeTypesBitmask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { updateMovementPatternUseCase(any(), newMp) } returns domainAfter

        viewModel.updateMovementPattern(newMp)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newMp, viewModel.state.value.lift?.movementPattern)
    }

    @Test
    fun addVolumeType_primary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val vt = VolumeType.entries.first()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = vt.bitMask,
            secondaryVolumeTypesBitmask = baseDomain.secondaryVolumeTypesBitmask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { addVolumeTypeUseCase(any(), vt, VolumeTypeCategory.PRIMARY) } returns domainAfter

        viewModel.addVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(vt), viewModel.state.value.lift?.volumeTypes)
    }

    @Test
    fun addVolumeType_secondary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val vt = VolumeType.entries.first()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = baseDomain.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = vt.bitMask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { addVolumeTypeUseCase(any(), vt, VolumeTypeCategory.SECONDARY) } returns domainAfter

        viewModel.addSecondaryVolumeType(vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(vt), viewModel.state.value.lift?.secondaryVolumeTypes)
    }

    @Test
    fun removeVolumeType_primary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = 0,
            secondaryVolumeTypesBitmask = baseDomain.secondaryVolumeTypesBitmask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { removeVolumeTypeUseCase(any(), any(), VolumeTypeCategory.PRIMARY) } returns domainAfter

        viewModel.removeVolumeType(VolumeType.entries.first())
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.lift?.volumeTypes?.isEmpty() == true)
    }

    @Test
    fun removeVolumeType_secondary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = baseDomain.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = 0,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { removeVolumeTypeUseCase(any(), any(), VolumeTypeCategory.SECONDARY) } returns domainAfter

        viewModel.removeSecondaryVolumeType(VolumeType.entries.first())
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.lift?.secondaryVolumeTypes?.isEmpty() == true)
    }

    @Test
    fun updateVolumeType_primary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val vt = VolumeType.entries.last()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = vt.bitMask,
            secondaryVolumeTypesBitmask = baseDomain.secondaryVolumeTypesBitmask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { updateVolumeTypeUseCase(any(), 0, vt, VolumeTypeCategory.PRIMARY) } returns domainAfter

        viewModel.updateVolumeType(index = 0, newVolumeType = vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(vt), viewModel.state.value.lift?.volumeTypes)
    }

    @Test
    fun updateVolumeType_secondary_updatesStateWithDomainReturnMappedToUi() = runTest {
        val ui = viewModel.state.value.lift!!
        val baseDomain = ui.toDomainModel()
        val vt = VolumeType.entries.last()
        val domainAfter = DomainLift(
            id = baseDomain.id,
            name = baseDomain.name,
            movementPattern = baseDomain.movementPattern,
            volumeTypesBitmask = baseDomain.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = vt.bitMask,
            incrementOverride = baseDomain.incrementOverride,
            restTime = baseDomain.restTime,
            restTimerEnabled = baseDomain.restTimerEnabled,
            isBodyweight = baseDomain.isBodyweight,
            note = baseDomain.note
        )
        coEvery { updateVolumeTypeUseCase(any(), 1, vt, VolumeTypeCategory.SECONDARY) } returns domainAfter

        viewModel.updateSecondaryVolumeType(index = 1, newVolumeType = vt)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(vt), viewModel.state.value.lift?.secondaryVolumeTypes)
    }
}
