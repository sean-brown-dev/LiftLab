package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.settings.SettingsConfigurationState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.common.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
class GetSettingConfigurationStateFlowUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetSettingConfigurationStateFlowUseCase

    // Backing flows we control in tests
    private lateinit var activeProgramFlow: MutableStateFlow<Program?>
    private lateinit var defaultIncrementFlow: MutableStateFlow<Float>
    private lateinit var defaultRestTimeMillisFlow: MutableStateFlow<Long>
    private lateinit var useAllLiftDataFlow: MutableStateFlow<Boolean>
    private lateinit var useOnlySamePositionFlow: MutableStateFlow<Boolean>
    private lateinit var liftSpecificDeloadFlow: MutableStateFlow<Boolean>
    private lateinit var promptOnDeloadStartFlow: MutableStateFlow<Boolean>

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Initialize flows with deterministic starting values
        activeProgramFlow = MutableStateFlow(null)
        defaultIncrementFlow = MutableStateFlow(2.5f)
        defaultRestTimeMillisFlow = MutableStateFlow(90_000L) // 90s
        useAllLiftDataFlow = MutableStateFlow(true)
        useOnlySamePositionFlow = MutableStateFlow(false)
        liftSpecificDeloadFlow = MutableStateFlow(false)
        promptOnDeloadStartFlow = MutableStateFlow(false)

        // Wire repositories to these flows
        every { programsRepository.getActiveProgramFlow() } returns activeProgramFlow

        // Important: specify generic type arguments when stubbing getSettingFlow
        every { settingsRepository.getSettingFlow<Float>(SettingKey.Increment) } returns defaultIncrementFlow
        every { settingsRepository.getSettingFlow<Long>(SettingKey.RestTime) } returns defaultRestTimeMillisFlow
        every { settingsRepository.getSettingFlow<Boolean>(SettingKey.UseAllLiftDataForRecommendations) } returns useAllLiftDataFlow
        every { settingsRepository.getSettingFlow<Boolean>(SettingKey.UseOnlyResultsFromLiftInSamePosition) } returns useOnlySamePositionFlow
        every { settingsRepository.getSettingFlow<Boolean>(SettingKey.LiftSpecificDeload) } returns liftSpecificDeloadFlow
        every { settingsRepository.getSettingFlow<Boolean>(SettingKey.PromptForDeloadWeek) } returns promptOnDeloadStartFlow

        useCase = GetSettingConfigurationStateFlowUseCase(
            programsRepository = programsRepository,
            settingsRepository = settingsRepository
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `initial emission maps all fields and converts rest time from millis to Duration`() = runTest {
        val state: SettingsConfigurationState = useCase().first()

        assertNull(state.activeProgram)
        assertEquals(2.5f, state.defaultIncrement)

        val expectedRest: Duration = 90_000L.toDuration(DurationUnit.MILLISECONDS)
        assertEquals(expectedRest, state.defaultRestTime)

        assertTrue(state.useAllLiftDataForRecommendations)
        assertEquals(false, state.useOnlyResultsFromLiftInSamePosition)
        assertEquals(false, state.liftSpecificDeloading)
        assertEquals(false, state.promptOnDeloadStart)
    }

    @Test
    fun `changing rest time millis updates converted Duration, other fields remain`() = runTest {
        val emissions = mutableListOf<SettingsConfigurationState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().take(2).toList(emissions)
        }

        // Trigger a change
        defaultRestTimeMillisFlow.value = 120_000L // 2nd emission

        collectJob.join()

        // Initial
        val s0 = emissions[0]
        assertEquals(90_000L.toDuration(DurationUnit.MILLISECONDS), s0.defaultRestTime)

        // Updated
        val s1 = emissions[1]
        assertEquals(120_000L.toDuration(DurationUnit.MILLISECONDS), s1.defaultRestTime)
        assertEquals(2.5f, s1.defaultIncrement)
        assertEquals(true, s1.useAllLiftDataForRecommendations)
        assertEquals(false, s1.useOnlyResultsFromLiftInSamePosition)
        assertEquals(false, s1.liftSpecificDeloading)
        assertEquals(false, s1.promptOnDeloadStart)
    }

    @Test
    fun `program change from null to non-null is reflected in state`() = runTest {
        val program = mockk<Program>(relaxed = true)

        val emissions = mutableListOf<SettingsConfigurationState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().take(2).toList(emissions)
        }

        activeProgramFlow.value = program

        collectJob.join()

        assertNull(emissions[0].activeProgram)
        assertNotNull(emissions[1].activeProgram)
        assertSame(program, emissions[1].activeProgram)
    }

    @Test
    fun `boolean toggles propagate correctly (useOnlySamePosition and promptOnDeloadStart)`() = runTest {
        val emissions = mutableListOf<SettingsConfigurationState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().take(3).toList(emissions)
        }

        useOnlySamePositionFlow.value = true
        promptOnDeloadStartFlow.value = true

        collectJob.join()

        // Initial
        val s0 = emissions[0]
        assertEquals(false, s0.useOnlyResultsFromLiftInSamePosition)
        assertEquals(false, s0.promptOnDeloadStart)

        // After first toggle
        val s1 = emissions[1]
        assertEquals(true, s1.useOnlyResultsFromLiftInSamePosition)
        assertEquals(false, s1.promptOnDeloadStart)

        // After second toggle
        val s2 = emissions[2]
        assertEquals(true, s2.useOnlyResultsFromLiftInSamePosition)
        assertEquals(true, s2.promptOnDeloadStart)
    }

    @Test
    fun `increment changes are reflected without affecting others`() = runTest {
        val emissions = mutableListOf<SettingsConfigurationState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().take(2).toList(emissions)
        }

        defaultIncrementFlow.value = 5.0f

        collectJob.join()

        assertEquals(2.5f, emissions[0].defaultIncrement)
        assertEquals(5.0f, emissions[1].defaultIncrement)
        // Spot-check unrelated fields unchanged
        assertEquals(
            90_000L.toDuration(DurationUnit.MILLISECONDS),
            emissions[1].defaultRestTime
        )
        assertEquals(true, emissions[1].useAllLiftDataForRecommendations)
    }
}
