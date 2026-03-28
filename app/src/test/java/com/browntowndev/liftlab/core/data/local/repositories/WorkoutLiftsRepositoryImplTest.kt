@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit; no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkoutLiftsRepositoryImplTest {

    @MockK lateinit var workoutLiftsDao: WorkoutLiftsDao

    private lateinit var repo: WorkoutLiftsRepositoryImpl

    // Minimal concrete set model to avoid depending on your sealed set types
    private data class TestSet(
        override val id: Long,
        override val workoutLiftId: Long,
        override val position: Int,
        override val rpeTarget: Float = 8f,
        override val repRangeBottom: Int = 5,
        override val repRangeTop: Int = 8,
    ) : GenericLiftSet

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repo = WorkoutLiftsRepositoryImpl(workoutLiftsDao)

        // Static mapping + metadata extensions
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensionsKt")
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensionsKt")
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntityCopyWithMetadataKt")
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntityCopyWithMetadataKt")

        // Domain <-> entity stubs
        every { any<WorkoutLiftWithRelationships>().toDomainModel() } answers {
            val d = firstArg<WorkoutLiftWithRelationships>()
            CustomWorkoutLift(
                id = d.workoutLiftEntity.id,
                workoutId = d.workoutLiftEntity.workoutId,
                liftId = d.workoutLiftEntity.liftId,
                liftName = "Lift${d.workoutLiftEntity.liftId}",
                liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                liftVolumeTypes = 0,
                liftSecondaryVolumeTypes = null,
                liftNote = null,
                position = d.workoutLiftEntity.position,
                progressionScheme = d.workoutLiftEntity.progressionScheme,
                deloadWeek = null,
                incrementOverride = null,
                restTime = null,
                restTimerEnabled = false,
                customLiftSets = emptyList()
            )
        }
        every { any<GenericWorkoutLift>().toEntity() } answers {
            val l = firstArg<GenericWorkoutLift>()
            WorkoutLiftEntity(
                id = l.id,
                workoutId = l.workoutId,
                liftId = l.liftId,
                progressionScheme = l.progressionScheme,
                position = l.position,
                setCount = l.setCount
            )
        }
        every { any<GenericLiftSet>().toEntity() } answers {
            val s = firstArg<GenericLiftSet>()
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

        // Metadata: return same instance for assertions
        every { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), any()) } answers { firstArg() }
        every { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), any()) } answers { firstArg() }
    }

    @AfterEach fun tearDown() = unmockkAll()

    // ---------- Reads ----------

    @Test fun `getAll maps`() = runTest {
        coEvery { workoutLiftsDao.getAll() } returns listOf(
            mockLiftDto(id = 1, workoutId = 10, liftId = 100),
            mockLiftDto(id = 2, workoutId = 10, liftId = 101),
        )
        val r = repo.getAll()
        assertEquals(listOf(1L, 2L), r.map { it.id })
    }

    @Test fun `getAllFlow maps with turbine`() = runTest {
        coEvery { workoutLiftsDao.getAllFlow() } returns flowOf(listOf(
            mockLiftDto(id = 9, workoutId = 1, liftId = 99)
        ))
        repo.getAllFlow().test {
            assertEquals(9L, awaitItem().single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `getById maps or null`() = runTest {
        coEvery { workoutLiftsDao.get(7) } returns mockLiftDto(7, 1, 100)
        assertEquals(7L, repo.getById(7)!!.id)

        coEvery { workoutLiftsDao.get(8) } returns null
        assertNull(repo.getById(8))
    }

    @Test fun `getMany maps`() = runTest {
        coEvery { workoutLiftsDao.getMany(listOf(1L, 2L)) } returns listOf(
            mockLiftDto(1, 1, 1),
            mockLiftDto(2, 1, 2),
        )
        val r = repo.getMany(listOf(1L, 2L))
        assertEquals(listOf(1L, 2L), r.map { it.id })
    }

    @Test fun `getLiftIdsForWorkout returns ids`() = runTest {
        coEvery { workoutLiftsDao.getLiftIdsForWorkout(42L) } returns listOf(3L, 4L)
        assertEquals(listOf(3L, 4L), repo.getLiftIdsForWorkout(42L))
    }

    @Test fun `getForWorkout maps`() = runTest {
        coEvery { workoutLiftsDao.getForWorkout(5L) } returns listOf(
            mockLiftDto(11, 5, 111)
        )
        val r = repo.getForWorkout(5L)
        assertEquals(listOf(11L), r.map { it.id })
    }

    // ---------- helpers ----------

    private fun mockLiftDto(id: Long, workoutId: Long, liftId: Long): WorkoutLiftWithRelationships {
        return mockk(relaxed = true) {
            every { workoutLiftEntity } returns WorkoutLiftEntity(
                id = id, workoutId = workoutId, liftId = liftId,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 0
            )
            every { customLiftSetEntities } returns emptyList()
        }
    }
}
