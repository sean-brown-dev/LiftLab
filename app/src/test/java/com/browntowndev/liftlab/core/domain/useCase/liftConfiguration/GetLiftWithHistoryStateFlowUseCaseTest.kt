package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

// ---- Explicit JUnit Jupiter assertion imports (no wildcards) ----
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.LiftWithHistoryState
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class GetLiftWithHistoryStateFlowUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var useCase: GetLiftWithHistoryStateFlowUseCase

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk(relaxed = true)
        workoutLogRepository = mockk(relaxed = true)
        useCase = GetLiftWithHistoryStateFlowUseCase(liftsRepository, workoutLogRepository)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `when liftId is null, emits default new-lift state and does not query repositories`() = runTest {
        val state = useCase(liftId = null).first()

        // Verify repositories were not queried
        coVerify(exactly = 0) {
            @Suppress("UnusedFlow")
            liftsRepository.getByIdFlow(any())
        }
        coVerify(exactly = 0) {
            @Suppress("UnusedFlow")
            workoutLogRepository.getWorkoutLogsForLiftFlow(any())
        }

        // Default Lift values (as constructed in the use case)
        val l: Lift = state.lift
        assertEquals(0L, l.id)
        assertEquals("", l.name)
        assertEquals(MovementPattern.AB_ISO, l.movementPattern)
        assertEquals(VolumeType.AB.bitMask, l.volumeTypesBitmask)
        assertNull(l.secondaryVolumeTypesBitmask)
        assertNull(l.incrementOverride)
        assertNull(l.restTime)
        assertTrue(l.restTimerEnabled)
        assertEquals(false, l.isBodyweight)
        assertNull(l.note)
    }

    @Test
    fun `when lift exists and logs are empty, metrics are null or zero`() = runTest {
        val liftId = 123L
        val lift = mockk<Lift>(relaxed = true)

        val liftFlow = MutableStateFlow<Lift?>(lift)
        val logsFlow = MutableStateFlow<List<WorkoutLogEntry>>(emptyList())

        coEvery { liftsRepository.getByIdFlow(liftId) } returns liftFlow
        coEvery { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns logsFlow

        val s: LiftWithHistoryState = useCase(liftId).first()

        assertSame(lift, s.lift)
        assertTrue(s.workoutLogEntries.isEmpty())

        // Metrics
        assertNull(s.maxVolume)
        assertNull(s.maxWeight)
        assertTrue(s.topTenPerformances.isEmpty())
        assertEquals(0, s.totalReps)
        assertEquals(0f, s.totalVolume)
    }

    @Test
    fun `computes metrics correctly with non-empty logs`() = runTest {
        val liftId = 10L
        val lift = mockk<Lift>(relaxed = true)

        val d1 = Date(1700000000000) // arbitrary stable dates
        val d2 = Date(1700100000000)

        // Sets for day 1: volumes 1000, 675; weights 200, 225
        val s1 = set(reps = 5, weight = 200f, oneRepMax = 250)
        val s2 = set(reps = 3, weight = 225f, oneRepMax = 275)

        // Sets for day 2: volumes 800, 315; weights 100, 315
        val s3 = set(reps = 8, weight = 100f, oneRepMax = 180)
        val s4 = set(reps = 1, weight = 315f, oneRepMax = 350)

        val log1 = log(d1, listOf(s1, s2))
        val log2 = log(d2, listOf(s3, s4))

        val liftFlow = MutableStateFlow<Lift?>(lift)
        val logsFlow = MutableStateFlow(listOf(log1, log2))

        coEvery { liftsRepository.getByIdFlow(liftId) } returns liftFlow
        coEvery { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns logsFlow

        val s: LiftWithHistoryState = useCase(liftId).first()

        // Max volume = max(max(1000,675)=1000 on d1, max(800,315)=800 on d2) -> (d1, 1000)
        assertNotNull(s.maxVolume)
        assertEquals(d1, s.maxVolume!!.first)
        assertEquals(1000f, s.maxVolume.second)

        // Max weight = max(max(200,225)=225 on d1, max(100,315)=315 on d2) -> (d2, 315)
        assertNotNull(s.maxWeight)
        assertEquals(d2, s.maxWeight!!.first)
        assertEquals(315f, s.maxWeight.second)

        // Top ten performances sorted by descending oneRepMax
        val expectedOrder = listOf(s4, s2, s1, s3)
        assertIterableEquals(expectedOrder, s.topTenPerformances.map { it.second })

        // Totals across all sets
        assertEquals(5 + 3 + 8 + 1, s.totalReps)
        assertEquals(1000f + 675f + 800f + 315f, s.totalVolume)
    }

    @Test
    fun `top ten performances are limited to 10 and sorted descending by oneRepMax`() = runTest {
        val liftId = 55L
        val lift = mockk<Lift>(relaxed = true)

        // Build 12 sets with distinct oneRepMax values 1..12
        val d = Date(1700200000000)
        val sets1to6 = (1..6).map { set(reps = 1, weight = 1f, oneRepMax = it) }
        val sets7to12 = (7..12).map { set(reps = 1, weight = 1f, oneRepMax = it) }

        val logA = log(d, sets1to6)
        val logB = log(d, sets7to12)

        val liftFlow = MutableStateFlow<Lift?>(lift)
        val logsFlow = MutableStateFlow(listOf(logA, logB))

        coEvery { liftsRepository.getByIdFlow(liftId) } returns liftFlow
        coEvery { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns logsFlow

        val s: LiftWithHistoryState = useCase(liftId).first()

        // Expect 12..3 (top 10)
        val topTen = s.topTenPerformances.map { it.second.oneRepMax }
        assertEquals(10, topTen.size)
        assertIterableEquals((12 downTo 3).toList(), topTen)
    }

    @Test
    fun `throws when lift flow emits null (lift not found)`() = runTest {
        val liftId = 777L
        val liftFlow = MutableStateFlow<Lift?>(null)
        val logsFlow = MutableStateFlow<List<WorkoutLogEntry>>(emptyList())

        coEvery { liftsRepository.getByIdFlow(liftId) } returns liftFlow
        coEvery { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns logsFlow

        // Because the current value is null, the first() should throw
        assertThrows<IllegalArgumentException> {
            useCase(liftId).first()
        }
    }

    @Test
    fun `subsequent snapshots reflect updates to logs`() = runTest {
        val liftId = 202L
        val lift = mockk<Lift>(relaxed = true)

        val d = Date(1700300000000)
        val logEmpty = emptyList<WorkoutLogEntry>()
        val s1 = set(5, 100f, 200)
        val logFull = listOf(log(d, listOf(s1)))

        val liftFlow = MutableStateFlow<Lift?>(lift)
        val logsFlow = MutableStateFlow(logEmpty)

        coEvery { liftsRepository.getByIdFlow(liftId) } returns liftFlow
        coEvery { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns logsFlow

        // Snapshot #1: no logs
        val s0 = useCase(liftId).first()
        assertTrue(s0.workoutLogEntries.isEmpty())
        assertEquals(0, s0.totalReps)
        assertEquals(0f, s0.totalVolume)

        // Update logs, take another snapshot (fresh collector)
        logsFlow.value = logFull
        val s1snap = useCase(liftId).first()
        assertEquals(listOf(d), s1snap.workoutLogEntries.map { it.date })
        assertEquals(5, s1snap.totalReps)
        assertEquals(500f, s1snap.totalVolume)
    }

    // -------- Helpers --------

    private fun set(reps: Int, weight: Float, oneRepMax: Int): SetLogEntry =
        mockk(relaxed = true) {
            every { this@mockk.reps } returns reps
            every { this@mockk.weight } returns weight
            every { this@mockk.oneRepMax } returns oneRepMax
        }

    private fun log(date: Date, sets: List<SetLogEntry>): WorkoutLogEntry =
        mockk(relaxed = true) {
            every { this@mockk.date } returns date
            every { this@mockk.setLogEntries } returns sets
        }
}
