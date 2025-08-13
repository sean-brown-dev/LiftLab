@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit; no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WorkoutLiftsRepositoryImplTest {

    @MockK lateinit var workoutLiftsDao: WorkoutLiftsDao
    @MockK lateinit var customSetsDao: CustomSetsDao
    @MockK lateinit var syncScheduler: SyncScheduler

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
        repo = WorkoutLiftsRepositoryImpl(workoutLiftsDao, customSetsDao, syncScheduler)

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

    // ---------- Single-field update ----------

    @Test fun `updateLiftId applies metadata, updates, schedules sync`() = runTest {
        coEvery { workoutLiftsDao.getWithoutRelationships(9) } returns WorkoutLiftEntity(
            id = 9, workoutId = 1, liftId = 10, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 0
        )
        coEvery { workoutLiftsDao.update(any()) } just Runs

        repo.updateLiftId(9, 55)

        coVerify { workoutLiftsDao.update(match { it.id == 9L && it.liftId == 55L }) }
        verify { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify { syncScheduler.scheduleSync() }
    }

    // ---------- UPDATE (with children) ----------

    @Test
    fun `update updates lift, updates only existing sets (id!=0), metadata applied, schedules sync`() = runTest {
        val model = customLift(id = 100, workoutId = 1, setIds = listOf(30L, 0L))
        coEvery { workoutLiftsDao.getWithoutRelationships(100) } returns WorkoutLiftEntity(
            id = 100, workoutId = 1, liftId = 200, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 2
        )
        coEvery { workoutLiftsDao.update(any()) } just Runs
        coEvery { customSetsDao.getByWorkoutLiftId(100) } returns listOf(
            CustomLiftSetEntity(id = 30, workoutLiftId = 100, type = SetType.STANDARD, position = 1, rpeTarget = 8f, repRangeBottom = 5, repRangeTop = 8)
        )
        coEvery { customSetsDao.updateMany(any()) } just Runs

        repo.update(model)

        coVerify { workoutLiftsDao.update(any()) }
        coVerify {
            customSetsDao.updateMany(match { sets -> sets.all { it.id != 0L } && sets.any { it.id == 30L } && sets.none { it.id == 0L } })
        }
        verify(atLeast = 1) { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify { syncScheduler.scheduleSync() }
    }

    // TODO (needs StandardWorkoutLift class):
    // @Test
    // fun `update switching to Standard deletes existing custom sets and schedules sync`() = runTest {
    //     // Given a current custom lift with existing sets, when updating with a StandardWorkoutLift
    //     // then customSetsDao.softDeleteByWorkoutLiftId(id) is called.
    // }

    @Test
    fun `updateMany updates only existing lifts and schedules sync`() = runTest {
        val m1 = customLift(id = 1, workoutId = 10, setIds = listOf(11L))
        val m2 = customLift(id = 2, workoutId = 10, setIds = listOf(0L))  // new set skipped in UPDATE

        coEvery { workoutLiftsDao.getManyWithoutRelationships(listOf(1L, 2L)) } returns listOf(
            WorkoutLiftEntity(id = 1, workoutId = 10, liftId = 100, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 1)
        )
        coEvery { workoutLiftsDao.updateMany(any()) } just Runs
        coEvery { customSetsDao.getByWorkoutLiftId(any()) } returns emptyList() // child helper fetch
        coEvery { customSetsDao.updateMany(any()) } just Runs

        repo.updateMany(listOf(m1, m2))

        // Only id=1 updated at lift level
        coVerify { workoutLiftsDao.updateMany(match { it.map { e -> e.id } == listOf(1L) }) }
        verify { syncScheduler.scheduleSync() }
    }

    // ---------- INSERT (children inherit *new* parent id) ----------

    @Test
    @DisplayName("INSERT: child sets use newly inserted workoutLiftId")
    fun insert_childrenCopyNewParentId() = runTest {
        val model = customLift(id = 0, workoutId = 1, setIds = listOf(0L, 0L), setWorkoutLiftId = 0)
        coEvery { workoutLiftsDao.insert(any()) } returns 777L
        coEvery { customSetsDao.getByWorkoutLiftId(777L) } returns emptyList()
        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.insertMany(capture(captured)) } returns listOf(1L, 2L)

        repo.insert(model)

        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 777L })
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `insertMany children copy the corresponding returned parent ids`() = runTest {
        val m1 = customLift(id = 0, workoutId = 10, setIds = listOf(0L), setWorkoutLiftId = 0)
        val m2 = customLift(id = 0, workoutId = 10, setIds = listOf(0L, 0L), setWorkoutLiftId = 0)

        coEvery { workoutLiftsDao.insertMany(any()) } returns listOf(101L, 102L)
        coEvery { customSetsDao.getByWorkoutLiftId(101L) } returns emptyList()
        coEvery { customSetsDao.getByWorkoutLiftId(102L) } returns emptyList()

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.insertMany(capture(captured)) } returns listOf(1L)

        repo.insertMany(listOf(m1, m2))

        assertEquals(2, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 101L }, "First batch must use parent id 101")
        assertTrue(captured[1].all { it.workoutLiftId == 102L }, "Second batch must use parent id 102")
        verify { syncScheduler.scheduleSync() }
    }

    // ---------- UPSERT (children inherit returned id) ----------

    @Test
    @DisplayName("UPSERT (new): child sets use returned id from DAO")
    fun upsert_new_childrenUseReturnedId() = runTest {
        val model = customLift(id = 0, workoutId = 1, setIds = listOf(0L, 0L), setWorkoutLiftId = 0)
        coEvery { workoutLiftsDao.getWithoutRelationships(0) } returns null
        coEvery { workoutLiftsDao.upsert(any()) } returns 200L
        coEvery { customSetsDao.getByWorkoutLiftId(200L) } returns emptyList()
        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns listOf(1L, 2L)

        val id = repo.upsert(model)

        assertEquals(200L, id)
        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 200L })
        verify(atLeast = 1) { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `upsert existing (-1L fallback) child sets use entity id`() = runTest {
        val model = customLift(id = 55, workoutId = 1, setIds = listOf(0L), setWorkoutLiftId = 0)
        coEvery { workoutLiftsDao.getWithoutRelationships(55) } returns WorkoutLiftEntity(
            id = 55, workoutId = 1, liftId = 2, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 1
        )
        coEvery { workoutLiftsDao.upsert(any()) } returns -1L
        coEvery { customSetsDao.getByWorkoutLiftId(55L) } returns emptyList()
        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns listOf(1L)

        val id = repo.upsert(model)

        assertEquals(55L, id)
        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 55L })
        verify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `upsertMany uses returned ids per element for child batches`() = runTest {
        val m1 = customLift(id = 1, workoutId = 10, setIds = listOf(0L), setWorkoutLiftId = 0)
        val m2 = customLift(id = 0, workoutId = 10, setIds = listOf(0L, 0L), setWorkoutLiftId = 0)

        coEvery { workoutLiftsDao.getManyWithoutRelationships(listOf(1L, 0L)) } returns listOf(
            WorkoutLiftEntity(id = 1, workoutId = 10, liftId = 100, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION, position = 1, setCount = 1)
        )
        coEvery { workoutLiftsDao.upsertMany(any()) } returns listOf(-1L, 333L) // m1 keeps 1, m2 becomes 333
        coEvery { customSetsDao.getByWorkoutLiftId(1L) } returns emptyList()
        coEvery { customSetsDao.getByWorkoutLiftId(333L) } returns emptyList()

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns listOf(1L)

        val ids = repo.upsertMany(listOf(m1, m2))

        assertEquals(listOf(1L, 333L), ids)
        assertEquals(2, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 1L })
        assertTrue(captured[1].all { it.workoutLiftId == 333L })
        verify { syncScheduler.scheduleSync() }
    }

    // ---------- Deletes ----------

    @Test fun `delete cascades sets and schedules when count gt 0`() = runTest {
        coEvery { workoutLiftsDao.softDelete(9L) } returns 1
        coEvery { customSetsDao.softDeleteByWorkoutLiftId(9L) } just Runs

        val c = repo.delete(customLift(id = 9, workoutId = 1, setIds = emptyList()))
        assertEquals(1, c)
        coVerify { customSetsDao.softDeleteByWorkoutLiftId(9L) }
        verify { syncScheduler.scheduleSync() }
    }

    @Test fun `deleteMany empty returns 0 and no sync`() = runTest {
        val c = repo.deleteMany(emptyList())
        assertEquals(0, c)
        verify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test fun `deleteMany cascades per id and schedules when gt 0`() = runTest {
        val m1 = customLift(id = 1, workoutId = 1, setIds = emptyList())
        val m2 = customLift(id = 2, workoutId = 1, setIds = emptyList())
        coEvery { workoutLiftsDao.softDeleteMany(listOf(1L, 2L)) } returns 2
        coEvery { customSetsDao.softDeleteByWorkoutLiftId(any()) } just Runs

        val c = repo.deleteMany(listOf(m1, m2))

        assertEquals(2, c)
        coVerify { customSetsDao.softDeleteByWorkoutLiftId(1L) }
        coVerify { customSetsDao.softDeleteByWorkoutLiftId(2L) }
        verify { syncScheduler.scheduleSync() }
    }

    @Test fun `deleteById delegates and schedules when gt 0`() = runTest {
        coEvery { workoutLiftsDao.softDelete(5L) } returns 1
        coEvery { customSetsDao.softDeleteByWorkoutLiftId(5L) } just Runs

        val c = repo.deleteById(5L)
        assertEquals(1, c)
        coVerify { customSetsDao.softDeleteByWorkoutLiftId(5L) }
        verify { syncScheduler.scheduleSync() }
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

    private fun customLift(
        id: Long,
        workoutId: Long,
        setIds: List<Long>,
        setWorkoutLiftId: Long = id, // default: sets use parent id unless forcing 0 for tests
        position: Int = 1,
        liftId: Long = 100L,
    ): CustomWorkoutLift {
        return CustomWorkoutLift(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftName = "Lift$liftId",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            liftNote = null,
            position = position,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            customLiftSets = setIds.mapIndexed { idx, setId ->
                TestSet(
                    id = setId,
                    workoutLiftId = setWorkoutLiftId,
                    position = idx + 1
                )
            }
        )
    }
}
