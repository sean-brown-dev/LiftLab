@file:OptIn(ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit; no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutMetadataDto
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkoutsRepositoryImplTest {

    @MockK lateinit var workoutsDao: WorkoutsDao

    private lateinit var repo: WorkoutsRepositoryImpl

    // minimal concrete set model we’ll map via mocked extension (so we don’t depend on StandardSet, etc.)
    private data class TestSet(
        override val id: Long,
        override val workoutLiftId: Long,
        override val position: Int,
        override val rpeTarget: Float = 8f,
        override val repRangeBottom: Int = 5,
        override val repRangeTop: Int = 8,
    ) : GenericLiftSet

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repo = WorkoutsRepositoryImpl(
            workoutsDao = workoutsDao,
        )

        // Static mapping extensions
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensionsKt")
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensionsKt")
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensionsKt")

        // Firestore metadata KSP extensions
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.WorkoutEntityCopyWithMetadataKt")
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntityCopyWithMetadataKt")
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntityCopyWithMetadataKt")

        // Workout<->domain mapping defaults
        every { any<WorkoutWithRelationships>().toDomainModel() } answers { wkr ->
            val w = wkr.invocation.args[0] as WorkoutWithRelationships
            Workout(
                id = w.workoutEntity.id,
                programId = w.workoutEntity.programId,
                name = w.workoutEntity.name,
                position = w.workoutEntity.position,
                lifts = emptyList()
            )
        }
        every { any<WorkoutEntity>().toDomainModel() } answers { we ->
            val e = we.invocation.args[0] as WorkoutEntity
            Workout(id = e.id, programId = e.programId, name = e.name, position = e.position, lifts = emptyList())
        }
        every { any<Workout>().toEntity() } answers {
            val w = firstArg<Workout>()
            WorkoutEntity(id = w.id, programId = w.programId, name = w.name, position = w.position)
        }

        // Lifts & sets mapping (we’ll verify workoutId copy in the repo, not inside mapping)
        every { any<GenericWorkoutLift>().toEntity() } answers {
            val l = firstArg<GenericWorkoutLift>()
            WorkoutLiftEntity(
                id = l.id,
                workoutId = l.workoutId,
                liftId = l.liftId,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = l.position,
                setCount = l.setCount
            )
        }
        every { any<GenericLiftSet>().toEntity() } answers {
            val s = firstArg<GenericLiftSet>() as TestSet
            CustomLiftSetEntity(
                id = s.id,
                workoutLiftId = s.workoutLiftId,
                type = SetType.STANDARD,
                position = s.position,
                rpeTarget = s.rpeTarget,
                repRangeBottom = s.repRangeBottom,
                repRangeTop = s.repRangeTop
            )
        }

        // Calculation + Metadata mapping
        every { any<WorkoutWithRelationships>().toCalculationDomainModel() } returns mockk<CalculationWorkout>(relaxed = true)
        every { any<WorkoutMetadataDto>().toDomainModel() } returns WorkoutMetadata(id = 99L, name = "Meta")

        // Firestore metadata copy calls return receiver
        every { any<WorkoutEntity>().applyRemoteStorageMetadata(any(), any(), any()) } answers { firstArg() }
        every { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), any()) } answers { firstArg() }
        every { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), any()) } answers { firstArg() }

        coEvery { workoutsDao.getMany(any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -------- Reads --------

    @Test
    fun `getAll maps list`() = runTest {
        coEvery { workoutsDao.getAll() } returns listOf(
            mockk<WorkoutWithRelationships> { every { workoutEntity } returns WorkoutEntity(id = 1, programId = 10, name = "A", position = 1) },
            mockk<WorkoutWithRelationships> { every { workoutEntity } returns WorkoutEntity(id = 2, programId = 10, name = "B", position = 2) },
        )

        val r = repo.getAll()

        assertEquals(listOf(1L, 2L), r.map { it.id })  // maps via toDomainModel()
        coVerify(exactly = 1) { workoutsDao.getAll() }
    }

    @Test
    fun `getAllFlow maps with turbine`() = runTest {
        coEvery { workoutsDao.getAllFlow() } returns flowOf(
            listOf(mockk<WorkoutWithRelationships> {
                every { workoutEntity } returns WorkoutEntity(id = 5, programId = 1, name = "X", position = 1)
            })
        )

        repo.getAllFlow().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(5L, list[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped workout or null`() = runTest {
        coEvery { workoutsDao.get(7L) } returns mockk {
            every { workoutEntity } returns WorkoutEntity(id = 7, programId = 2, name = "W", position = 1)
        }
        assertEquals(7L, repo.getById(7L)!!.id)

        coEvery { workoutsDao.get(8L) } returns null
        assertNull(repo.getById(8L))
    }

    @Test
    fun `getMany maps by ids`() = runTest {
        coEvery { workoutsDao.getMany(listOf(1L, 2L)) } returns listOf(
            mockk { every { workoutEntity } returns WorkoutEntity(id = 1, programId = 1, name = "A", position = 1) },
            mockk { every { workoutEntity } returns WorkoutEntity(id = 2, programId = 1, name = "B", position = 2) },
        )

        val r = repo.getMany(listOf(1L, 2L))
        assertEquals(listOf(1L, 2L), r.map { it.id })
    }

    @Test
    fun `getFlow maps nullable with turbine`() = runTest {
        coEvery { workoutsDao.getByIdFlow(10L) } returns flowOf(
            mockk {
                every { workoutEntity } returns WorkoutEntity(id = 10, programId = 1, name = "W", position = 1)
            }
        )

        repo.getFlow(10L).test {
            val item = awaitItem()
            assertNotNull(item)
            assertEquals(10L, item!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getByMicrocyclePositionForCalculation maps to CalculationWorkout`() = runTest {
        val dto = mockk<WorkoutWithRelationships>(relaxed = true)
        coEvery { workoutsDao.getByMicrocyclePosition(programId = 1L, microcyclePosition = 3) } returns flowOf(dto)

        repo.getByMicrocyclePositionForCalculation(1L, 3).test {
            val cw = awaitItem()
            assertNotNull(cw)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllForProgramWithoutLiftsPopulated maps entities`() = runTest {
        coEvery { workoutsDao.getAllForProgramWithoutRelationships(5L) } returns listOf(
            WorkoutEntity(id = 1, programId = 5, name = "W1", position = 1),
            WorkoutEntity(id = 2, programId = 5, name = "W2", position = 2),
        )

        val r = repo.getAllForProgramWithoutLiftsPopulated(5L)
        assertEquals(listOf(1L, 2L), r.map { it.id })
    }

    // -------- helpers --------

    private fun customLift(
        id: Long,
        workoutId: Long,
        position: Int,
        setPositions: List<Int>,
        liftId: Long = 500L,
        setWorkoutLiftId: Long = id // allow forcing 0 for TDD FK propagation checks
    ): CustomWorkoutLift {
        return CustomWorkoutLift(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftName = "Lift$liftId",
            liftMovementPattern = MovementPattern.BICEP_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            liftNote = null,
            position = position,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            customLiftSets = setPositions.mapIndexed { idx, pos ->
                // make first set “existing” when the parent lift is existing (id!=0)
                val setId = if (idx == 0 && id != 0L) 30L else 0L
                TestSet(
                    id = setId,
                    workoutLiftId = setWorkoutLiftId,
                    position = pos
                )
            }
        )
    }
}
