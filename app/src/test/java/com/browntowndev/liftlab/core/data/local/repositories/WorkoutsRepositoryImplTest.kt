@file:OptIn(ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit; no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutMetadataDto
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WorkoutsRepositoryImplTest {

    @MockK lateinit var workoutsDao: WorkoutsDao
    @MockK lateinit var workoutLiftsDao: WorkoutLiftsDao
    @MockK lateinit var customSetsDao: CustomSetsDao
    @MockK lateinit var liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao
    @MockK lateinit var workoutInProgressDao: WorkoutInProgressDao
    @MockK lateinit var syncScheduler: SyncScheduler

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
            workoutLiftsDao = workoutLiftsDao,
            customSetsDao = customSetsDao,
            liveWorkoutCompletedSetsDao = liveWorkoutCompletedSetsDao,
            workoutInProgressDao = workoutInProgressDao,
            syncScheduler = syncScheduler
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
        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.getForWorkout(any()) } returns emptyList()
        coEvery { customSetsDao.getByWorkoutLiftId(any()) } returns emptyList()
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

    // -------- Single-field update --------

    @Test
    fun `updateName applies metadata, updates, schedules sync`() = runTest {
        val current = WorkoutEntity(id = 9, programId = 2, name = "Old", position = 1)
        coEvery { workoutsDao.getWithoutRelationships(9L) } returns current
        coEvery { workoutsDao.update(any()) } just Runs

        repo.updateName(9L, "New")

        coVerify { workoutsDao.update(match { it.id == 9L && it.name == "New" }) }
        verify(exactly = 1) { any<WorkoutEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    // -------- Update (with children) --------

    @Test
    @DisplayName("UPDATE filters children: only id != 0 are updated, metadata applied, workoutId copied")
    fun update_filtersChildrenAndAppliesMetadata() = runTest {
        val workoutId = 100L

        val existingLiftId = 20L
        val newLiftId = 0L

        val existingLift = customLift(id = existingLiftId, workoutId = workoutId, position = 1, setPositions = listOf(1))
        val newLift = customLift(id = newLiftId, workoutId = workoutId, position = 2, setPositions = listOf(1, 2))

        val model = Workout(
            id = workoutId,
            programId = 7L,
            name = "W",
            position = 1,
            lifts = listOf(existingLift, newLift)
        )

        // current workout (so update path is taken)
        coEvery { workoutsDao.getWithoutRelationships(workoutId) } returns WorkoutEntity(id = workoutId, programId = 7L, name = "W", position = 1)
        coEvery { workoutsDao.update(any()) } just Runs

        // existing lift and existing set (for id != 0 filter)
        val existingLiftRel = mockk<WorkoutLiftWithRelationships> {
            every { workoutLiftEntity } returns WorkoutLiftEntity(
                id = existingLiftId,
                workoutId = workoutId,
                liftId = 501L,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 1,
                setCount = 1
            )
            every { customLiftSetEntities } returns listOf(
                CustomLiftSetEntity(
                    id = 30L, workoutLiftId = existingLiftId, type = SetType.STANDARD,
                    position = 1, rpeTarget = 8f, repRangeBottom = 5, repRangeTop = 8
                )
            )
        }
        coEvery { workoutLiftsDao.getMany(listOf(existingLiftId, newLiftId)) } returns listOf(existingLiftRel)
        coEvery { workoutLiftsDao.updateMany(any()) } just Runs
        coEvery { customSetsDao.updateMany(any()) } just Runs

        repo.update(model)

        // verify lifts: only existing id gets updated, and workoutId is correctly set to the parent
        coVerify {
            workoutLiftsDao.updateMany(match { list ->
                list.all { it.id != 0L } && list.any { it.id == existingLiftId } &&
                        list.all { it.workoutId == workoutId }
            })
        }
        // verify sets: only existing set id != 0 updated
        coVerify {
            customSetsDao.updateMany(match { list -> list.all { it.id != 0L } })
        }

        // metadata applied (workout + lifts + sets)
        verify(atLeast = 1) { any<WorkoutEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }

        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    @DisplayName("UPDATE: switching a lift to Standard deletes its custom sets")
    fun update_switchToStandard_deletesCustomSets() = runTest {
        val testWorkoutId = 200L

        val standard = standardLift(
            id = 20L,
            liftId = 501L,
            position = 2,
            setPositions = emptyList(),
            workoutId = testWorkoutId,
        )
        val custom = customLift(id = 10L, workoutId = testWorkoutId, position = 1, setPositions = listOf(1))

        val model = Workout(id = testWorkoutId, programId = 9L, name = "W", position = 1, lifts = listOf(custom, standard))

        coEvery { workoutsDao.getWithoutRelationships(testWorkoutId) } returns
                WorkoutEntity(id = testWorkoutId, programId = 9L, name = "W", position = 1)
        coEvery { workoutsDao.update(any()) } just Runs
        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.updateMany(any()) } just Runs
        coEvery { customSetsDao.updateMany(any()) } just Runs

        // NEW: repo fetches existing sets under both lifts for diff/purge
        coEvery { customSetsDao.getByWorkoutLiftId(10L) } returns listOf(
            CustomLiftSetEntity(
                id = 30L, workoutLiftId = 10L, type = SetType.STANDARD,
                position = 1, rpeTarget = 8f, repRangeBottom = 5, repRangeTop = 8
            )
        )
        // IMPORTANT: return some existing sets for the STANDARD lift so the repo will delete them
        coEvery { customSetsDao.getByWorkoutLiftId(20L) } returns listOf(
            CustomLiftSetEntity(
                id = 40L, workoutLiftId = 20L, type = SetType.STANDARD,
                position = 1, rpeTarget = 8f, repRangeBottom = 5, repRangeTop = 8
            )
        )

        // Repo now calls softDeleteMany with the collected IDs (not softDeleteByWorkoutLiftId)
        coEvery { customSetsDao.softDeleteMany(listOf(40L)) } returns 1

        // (Optional but future-proof) repo may read other diffs for this workout; keep them empty
        coEvery { workoutLiftsDao.getForWorkout(testWorkoutId) } returns emptyList()
        coEvery { workoutLiftsDao.getLiftIdsForWorkout(testWorkoutId) } returns emptyList()

        repo.update(model)

        // Verify purge occurred using softDeleteMany on the returned set ids
        coVerify { customSetsDao.softDeleteMany(listOf(40L)) }
        verify { syncScheduler.scheduleSync() }
    }


    @Test
    fun `updateMany updates only existing workouts, performs UPDATE on children, schedules sync`() = runTest {
        val w1 = Workout(id = 1, programId = 10, name = "W1", position = 1, lifts = emptyList())
        val w2 = Workout(id = 2, programId = 10, name = "W2", position = 2, lifts = emptyList())
        val wNew = Workout(id = 0, programId = 10, name = "WNew", position = 3, lifts = emptyList())

        // only 1 and 2 exist “without relationships”
        coEvery { workoutsDao.getManyWithoutRelationships(listOf(1L, 2L, 0L)) } returns listOf(
            WorkoutEntity(id = 1, programId = 10, name = "W1", position = 1),
            WorkoutEntity(id = 2, programId = 10, name = "W2", position = 2),
        )
        coEvery { workoutsDao.updateMany(any()) } just Runs

        repo.updateMany(listOf(w1, w2, wNew))

        // ensure only existing (1,2) are updated
        coVerify {
            workoutsDao.updateMany(match { list -> list.map { it.id }.sorted() == listOf(1L, 2L) })
        }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    // -------- Upsert --------

    @Test
    fun `upsert maps, applies metadata, cascades UPSERT to lifts and sets, schedules sync`() = runTest {
        val w = Workout(id = 0L, programId = 2L, name = "W", position = 1,
            lifts = listOf(customLift(id = 0L, workoutId = 0L, position = 1, setPositions = listOf(1, 2), setWorkoutLiftId = 0))
        )

        coEvery { workoutsDao.getWithoutRelationships(0L) } returns null
        coEvery { workoutsDao.upsert(any()) } returns 123L

        // no existing lifts/sets yet
        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.upsertMany(any()) } returns listOf(555L) // new lift id returned

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns emptyList()

        // After upserting the workout, repo diffs existing lifts for that workoutId (123)
        coEvery { workoutLiftsDao.getForWorkout(123L) } returns emptyList()

        // Repo also diff-deletes sets after persisting, stub this too for the new lift id (555)
        coEvery { customSetsDao.getByWorkoutLiftId(555L) } returns emptyList()

        val id = repo.upsert(w)

        assertEquals(123L, id)
        // EXPECTATION (TDD) – child sets must use the *returned* lift id (555)
        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 555L }, "Sets must copy parent lift id returned from DAO")

        // metadata applied (workout + lifts + sets)
        verify(atLeast = 1) { any<WorkoutEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `upsert - DAO returns -1L uses entity id, still cascades UPSERT`() = runTest {
        val w = Workout(id = 55L, programId = 2L, name = "W", position = 1,
            lifts = listOf(customLift(id = 55L, workoutId = 0L, position = 1, setPositions = listOf(1), setWorkoutLiftId = 0))
        )

        coEvery { workoutsDao.getWithoutRelationships(55L) } returns WorkoutEntity(id = 55L, programId = 2L, name = "W", position = 1)
        coEvery { workoutsDao.upsert(any()) } returns -1L

        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.upsertMany(any()) } returns listOf(-1L) // fallback to 55

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns emptyList()

        val upsertedId = repo.upsert(w)

        assertEquals(55L, upsertedId) // falls back to entity id
        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 55L }, "Sets must copy the existing entity id when DAO returns -1L")
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `upsertMany applies metadata for each, cascades UPSERT children, returns ids`() = runTest {
        val w1 = Workout(id = 1L, programId = 2L, name = "W1", position = 1,
            lifts = listOf(customLift(id = 1L, workoutId = 1L, position = 1, setPositions = listOf(1), setWorkoutLiftId = 0))
        )
        val w2 = Workout(id = 0L, programId = 2L, name = "W2", position = 2,
            lifts = listOf(customLift(id = 0L, workoutId = 0L, position = 1, setPositions = listOf(1, 2), setWorkoutLiftId = 0))
        )

        coEvery { workoutsDao.getManyWithoutRelationships(listOf(1L, 0L)) } returns listOf(
            WorkoutEntity(id = 1L, programId = 2L, name = "W1", position = 1),
        )
        coEvery { workoutsDao.upsertMany(any()) } returns listOf(-1L, 222L)

        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.upsertMany(any()) } returnsMany listOf(
            listOf(-1L),
            listOf(333L)
        ) // first keeps 1, second is 333

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.upsertMany(capture(captured)) } returns emptyList()

        val ids = repo.upsertMany(listOf(w1, w2))

        assertEquals(listOf(1L, 222L), ids)
        // EXPECTATION (TDD): two child batches; first uses 1, second uses 333
        assertEquals(2, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 1L }, "First batch sets must use lift id 1")
        assertTrue(captured[1].all { it.workoutLiftId == 333L }, "Second batch sets must use lift id 333")
        verify { syncScheduler.scheduleSync() }
    }

    // -------- Insert --------

    @Test
    fun `insert persists workout, cascades INSERT for lifts and sets, schedules sync`() = runTest {
        val w = Workout(id = 0L, programId = 3L, name = "W", position = 1,
            lifts = listOf(customLift(id = 0L, workoutId = 0L, position = 1, setPositions = listOf(1, 2), setWorkoutLiftId = 0))
        )

        coEvery { workoutsDao.insert(any()) } returns 777L

        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.insertMany(any()) } returns listOf(888L) // inserted lift id

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.insertMany(capture(captured)) } returns emptyList()

        val id = repo.insert(w)

        assertEquals(777L, id)
        // EXPECTATION (TDD): sets use 888 as workoutLiftId
        assertEquals(1, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 888L }, "Sets must copy inserted parent lift id")

        // even on INSERT, child entities get metadata applied by helper
        verify(atLeast = 1) { any<WorkoutLiftEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(atLeast = 1) { any<CustomLiftSetEntity>().applyRemoteStorageMetadata(any(), any(), false) }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `insertMany persists workouts, cascades INSERT for each, schedules sync`() = runTest {
        val w1 = Workout(id = 0L, programId = 3L, name = "W1", position = 1,
            lifts = listOf(customLift(id = 0L, workoutId = 0L, position = 1, setPositions = listOf(1), setWorkoutLiftId = 0))
        )
        val w2 = Workout(id = 0L, programId = 3L, name = "W2", position = 2,
            lifts = listOf(customLift(id = 0L, workoutId = 0L, position = 1, setPositions = listOf(1,2), setWorkoutLiftId = 0))
        )

        coEvery { workoutsDao.insertMany(any()) } returns listOf(11L, 12L)
        coEvery { workoutLiftsDao.getMany(any()) } returns emptyList()
        coEvery { workoutLiftsDao.insertMany(any()) } returnsMany listOf(
            listOf(901L), // for w1’s single lift
            listOf(902L)  // for w2’s single lift
        )

        val captured = mutableListOf<List<CustomLiftSetEntity>>()
        coEvery { customSetsDao.insertMany(capture(captured)) } returns emptyList()

        val ids = repo.insertMany(listOf(w1, w2))

        assertEquals(listOf(11L, 12L), ids)
        // EXPECTATION (TDD): two child batches; first uses 901, second uses 902
        assertEquals(2, captured.size)
        assertTrue(captured[0].all { it.workoutLiftId == 901L }, "First batch sets must use lift id 901")
        assertTrue(captured[1].all { it.workoutLiftId == 902L }, "Second batch sets must use lift id 902")
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    // -------- Deletes --------

    @Test
    fun `delete deletes workout, cascades children only when rows affected, schedules sync`() = runTest {
        val w = Workout(id = 9L, programId = 2L, name = "W", position = 1, lifts = emptyList())

        coEvery { workoutsDao.softDelete(9L) } returns 1
        coEvery { workoutLiftsDao.softDeleteByWorkoutId(9L) } just Runs
        coEvery { customSetsDao.softDeleteByWorkoutId(9L) } just Runs
        coEvery { workoutInProgressDao.softDeleteByWorkoutId(9L) } just Runs
        coEvery { liveWorkoutCompletedSetsDao.softDeleteAllByWorkoutId(9L) } just Runs

        val count = repo.delete(w)

        assertEquals(1, count)
        coVerify { workoutLiftsDao.softDeleteByWorkoutId(9L) }
        coVerify { customSetsDao.softDeleteByWorkoutId(9L) }
        coVerify { workoutInProgressDao.softDeleteByWorkoutId(9L) }
        coVerify { liveWorkoutCompletedSetsDao.softDeleteAllByWorkoutId(9L) }
        verify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `deleteMany 0 input returns 0 and no sync`() = runTest {
        val c = repo.deleteMany(emptyList())
        assertEquals(0, c)
        verify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `deleteMany cascades for each id and schedules sync when rows affected`() = runTest {
        val w1 = Workout(id = 1, programId = 2, name = "W1", position = 1, lifts = emptyList())
        val w2 = Workout(id = 2, programId = 2, name = "W2", position = 2, lifts = emptyList())

        coEvery { workoutsDao.softDeleteMany(listOf(1L, 2L)) } returns 2

        // UPDATED: bulk cascade stubs
        coEvery { workoutLiftsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { customSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { workoutInProgressDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs

        val c = repo.deleteMany(listOf(w1, w2))

        assertEquals(2, c)

        // UPDATED: verify bulk calls (not per-id)
        coVerify { workoutLiftsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { customSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { workoutInProgressDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }

        verify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `deleteById loads model and delegates`() = runTest {
        coEvery { workoutsDao.get(5L) } returns mockk {
            every { workoutEntity } returns WorkoutEntity(id = 5L, programId = 1L, name = "W", position = 1)
        }
        coEvery { workoutsDao.softDelete(5L) } returns 1
        coEvery { workoutLiftsDao.softDeleteByWorkoutId(5L) } just Runs
        coEvery { customSetsDao.softDeleteByWorkoutId(5L) } just Runs
        coEvery { workoutInProgressDao.softDeleteByWorkoutId(5L) } just Runs
        coEvery { liveWorkoutCompletedSetsDao.softDeleteAllByWorkoutId(5L) } just Runs

        val c = repo.deleteById(5L)
        assertEquals(1, c)
    }

    @Test
    fun `deleteById returns 0 when not found`() = runTest {
        coEvery { workoutsDao.get(99L) } returns null
        assertEquals(0, repo.deleteById(99L))
    }

    @Test
    fun `delete does nothing when no row affected (no cascade, no sync)`() = runTest {
        val w = Workout(id = 9L, programId = 2L, name = "W", position = 1, lifts = emptyList())
        coEvery { workoutsDao.softDelete(9L) } returns 0

        val count = repo.delete(w)

        assertEquals(0, count)
        verify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `deleteMany cascades using bulk dao calls and schedules sync when rows affected`() = runTest {
        val w1 = Workout(id = 1, programId = 2, name = "W1", position = 1, lifts = emptyList())
        val w2 = Workout(id = 2, programId = 2, name = "W2", position = 2, lifts = emptyList())

        coEvery { workoutsDao.softDeleteMany(listOf(1L, 2L)) } returns 2
        coEvery { workoutLiftsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { customSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { workoutInProgressDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs
        coEvery { liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) } just Runs

        val c = repo.deleteMany(listOf(w1, w2))

        assertEquals(2, c)
        coVerify { workoutLiftsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { customSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { workoutInProgressDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        coVerify { liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(listOf(1L, 2L)) }
        verify { syncScheduler.scheduleSync() }
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

    private fun standardLift(
        id: Long,
        workoutId: Long,
        position: Int,
        setPositions: List<Int>,
        liftId: Long = 500L,
    ): StandardWorkoutLift {
        return StandardWorkoutLift(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftName = "Lift$liftId",
            liftMovementPattern = MovementPattern.BICEP_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            liftNote = null,
            position = position,
            setCount = setPositions.size,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            rpeTarget = 8f,
            repRangeBottom = 8,
            repRangeTop = 10,
            stepSize = 2,
        )
    }
}
