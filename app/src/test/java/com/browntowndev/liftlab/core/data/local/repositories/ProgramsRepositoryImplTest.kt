package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.dtos.ProgramMetadataDto
import com.browntowndev.liftlab.core.data.local.dtos.ProgramWithRelationshipsDto
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration

/**
 * Comprehensive test suite for ProgramsRepositoryImpl using the UPDATED ProgramDelta DSL.
 * Includes helper builders to ensure entity/DTO construction matches the latest constructors.
 */
class ProgramsRepositoryImplTest {

    // ---------- Test helpers (builders) ----------

    private fun buildProgramEntity(
        id: Long = 1L,
        name: String = "Program",
        deloadWeek: Int = 4,
        isActive: Boolean = true,
        currentMicrocycle: Int = 0,
        currentMicrocyclePosition: Int = 0,
        currentMesocycle: Int = 0,
    ) = ProgramEntity(
        id = id,
        name = name,
        deloadWeek = deloadWeek,
        isActive = isActive,
        currentMicrocycle = currentMicrocycle,
        currentMicrocyclePosition = currentMicrocyclePosition,
        currentMesocycle = currentMesocycle,
    )

    private fun buildWorkoutEntity(
        id: Long = 10L,
        programId: Long = 1L,
        name: String = "Workout",
        position: Int = 0,
    ) = WorkoutEntity(
        id = id,
        programId = programId,
        name = name,
        position = position,
    )

    private fun buildLiftEntity(
        id: Long = 200L,
        name: String = "Lift",
        movementPattern: MovementPattern = MovementPattern.HORIZONTAL_PULL,
        volumeTypesBitmask: Int = 0,
        secondaryVolumeTypesBitmask: Int? = null,
        restTime: Duration? = null,
        restTimerEnabled: Boolean = true,
        incrementOverride: Float? = null,
        isBodyweight: Boolean = false,
        note: String? = null,
    ) = LiftEntity(
        id = id,
        name = name,
        movementPattern = movementPattern,
        volumeTypesBitmask = volumeTypesBitmask,
        secondaryVolumeTypesBitmask = secondaryVolumeTypesBitmask,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        incrementOverride = incrementOverride,
        isBodyweight = isBodyweight,
        note = note,
    )

    private fun buildWorkoutLiftEntity(
        id: Long = 5L,
        workoutId: Long = 10L,
        liftId: Long = 200L,
        progressionScheme: ProgressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        position: Int = 0,
        setCount: Int = 3,
        deloadWeek: Int? = null,
        rpeTarget: Float? = null,
        repRangeBottom: Int? = 8,
        repRangeTop: Int? = 10,
        stepSize: Int? = null,
    ) = WorkoutLiftEntity(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        progressionScheme = progressionScheme,
        position = position,
        setCount = setCount,
        deloadWeek = deloadWeek,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        stepSize = stepSize,
    )

    private fun buildCustomLiftSetEntity(
        id: Long = 2L,
        workoutLiftId: Long = 5L,
        type: SetType = SetType.STANDARD,
        position: Int = 1,
        rpeTarget: Float = 8f,
        repRangeBottom: Int = 8,
        repRangeTop: Int = 10,
        setGoal: Int? = null,
        repFloor: Int? = null,
        dropPercentage: Float? = null,
        maxSets: Int? = null,
        setMatching: Boolean = false,
    ) = CustomLiftSetEntity(
        id = id,
        workoutLiftId = workoutLiftId,
        type = type,
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        setGoal = setGoal,
        repFloor = repFloor,
        dropPercentage = dropPercentage,
        maxSets = maxSets,
        setMatching = setMatching,
    )

    private fun buildWorkoutLiftWithRelationships(
        wl: WorkoutLiftEntity = buildWorkoutLiftEntity(),
        lift: LiftEntity = buildLiftEntity(id = wl.liftId),
        sets: List<CustomLiftSetEntity> = emptyList(),
    ): WorkoutLiftWithRelationships =
        WorkoutLiftWithRelationships(
            workoutLiftEntity = wl,
            liftEntity = lift,
            customLiftSetEntities = sets,
        )

    private fun buildWorkoutWithRelationships(
        w: WorkoutEntity = buildWorkoutEntity(),
        lifts: List<WorkoutLiftWithRelationships> = emptyList(),
    ): WorkoutWithRelationships =
        WorkoutWithRelationships(
            workoutEntity = w,
            lifts = lifts,
        )

    private fun buildProgramWithRelationships(
        p: ProgramEntity = buildProgramEntity(),
        workouts: List<WorkoutWithRelationships> = emptyList(),
    ) = ProgramWithRelationshipsDto(
        programEntity = p,
        workouts = workouts,
    )

    // ---------- Mocks & SUT ----------

    private lateinit var programsDao: ProgramsDao
    private lateinit var workoutsDao: WorkoutsDao
    private lateinit var workoutLiftsDao: WorkoutLiftsDao
    private lateinit var customSetsDao: CustomSetsDao
    private lateinit var liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao
    private lateinit var workoutInProgressDao: WorkoutInProgressDao
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var transactionScope: TransactionScope
    private lateinit var repo: ProgramsRepositoryImpl

    @BeforeEach
    fun setUp() {
        programsDao = mockk(relaxed = true)
        workoutsDao = mockk(relaxed = true)
        workoutLiftsDao = mockk(relaxed = true)
        customSetsDao = mockk(relaxed = true)
        liveWorkoutCompletedSetsDao = mockk(relaxed = true)
        workoutInProgressDao = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        repo = ProgramsRepositoryImpl(
            programsDao,
            workoutsDao,
            workoutLiftsDao,
            customSetsDao,
            liveWorkoutCompletedSetsDao,
            workoutInProgressDao,
            syncScheduler,
            transactionScope
        )
    }

    // ------------------------ applyDelta: Delete program ------------------------

    @Test
    fun `applyDelta - deleteProgram cascades and schedules sync when rows deleted`() = runTest {
        coEvery { programsDao.softDelete(1L) } returns 1

        val delta = programDelta { deleteProgram() }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            programsDao.softDelete(1L)
            workoutsDao.softDeleteByProgramId(1L)
            workoutLiftsDao.softDeleteByProgramId(1L)
            customSetsDao.softDeleteByProgramId(1L)
            workoutInProgressDao.softDeleteByProgramId(1L)
            liveWorkoutCompletedSetsDao.softDeleteByProgramId(1L)
            syncScheduler.scheduleSync()
        }
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }

    @Test
    fun `applyDelta - deleteProgram with no rows deleted does not cascade or sync`() = runTest {
        coEvery { programsDao.softDelete(1L) } returns 0

        val delta = programDelta { deleteProgram() }

        repo.applyDelta(1L, delta)

        coVerify(exactly = 1) { programsDao.softDelete(1L) }
        coVerify(exactly = 0) { workoutsDao.softDeleteByProgramId(any()) }
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    // ------------------------ applyDelta: Update program ------------------------

    @Test
    fun `applyDelta - updateProgram patches provided fields and schedules once`() = runTest {
        coEvery { programsDao.get(1L) } returns buildProgramWithRelationships(buildProgramEntity(id = 1L, name = "Old", deloadWeek = 1, isActive = true, currentMesocycle = 2, currentMicrocycle = 3, currentMicrocyclePosition = 0))

        val delta = programDelta {
            updateProgram(name = "New", deloadWeek = 4, currentMicrocycle = 5)
        }

        repo.applyDelta(1L, delta)

        coVerify(exactly = 1) {
            programsDao.update(match {
                it.id == 1L && it.name == "New" && it.deloadWeek == 4 && it.currentMicrocycle == 5 && it.currentMesocycle == 2 && it.isActive
            })
        }
        coVerify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    // ------------------------ applyDelta: Remove workouts ------------------------

    @Test
    fun `applyDelta - removeWorkouts cascades and schedules once when any removed`() = runTest {
        coEvery { workoutsDao.softDeleteMany(listOf(10L, 11L)) } returns 2

        val delta = programDelta { removeWorkouts(10L, 11L) }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            workoutsDao.softDeleteMany(listOf(10L, 11L))
            workoutLiftsDao.softDeleteByWorkoutIds(listOf(10L, 11L))
            customSetsDao.softDeleteByWorkoutIds(listOf(10L, 11L))
            workoutInProgressDao.softDeleteByWorkoutIds(listOf(10L, 11L))
            liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(listOf(10L, 11L))
            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - removeWorkouts no rows removed does not cascade`() = runTest {
        coEvery { workoutsDao.softDeleteMany(listOf(10L)) } returns 0

        repo.applyDelta(1L, programDelta { removeWorkouts(10L) })

        coVerify(exactly = 0) { workoutLiftsDao.softDeleteByWorkoutIds(any()) }
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    // ------------------------ applyDelta: Insert & Update workout ------------------------

    @Test
    fun `applyDelta - insert workout uses returned id downstream and schedules`() = runTest {
        // upsert of insert returns generated id
        coEvery { workoutsDao.upsert(any()) } returns 99L
        coEvery { workoutLiftsDao.getForWorkout(99L) } returns emptyList()

        val newWorkoutDomain = buildWorkoutEntity(id = 0L, programId = 1L, name = "W", position = 0).toDomainModel()

        val delta = programDelta { workout(newWorkoutDomain) }

        repo.applyDelta(1L, delta)

        coVerify { workoutsDao.upsert(match { it.programId == 1L && it.name == "W" && it.id == 0L }) }
        coVerify { workoutLiftsDao.getForWorkout(99L) }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - update workout missing throws`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(77L, 1L) } returns null

        val delta = programDelta { workout(77L, name = "X") { } }

        try {
            repo.applyDelta(1L, delta)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) { /* expected */ }
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - update workout merges fields and marks unsynced`() = runTest {
        val existing = buildWorkoutEntity(id = 77L, programId = 1L, name = "Old", position = 5)
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(77L, 1L) } returns existing
        coEvery { workoutsDao.upsert(any()) } returns -1L // emulate update
        coEvery { workoutLiftsDao.getForWorkout(77L) } returns emptyList()

        val delta = programDelta { workout(77L, name = "New", position = 9) { } }

        repo.applyDelta(1L, delta)

        coVerify {
            workoutsDao.upsert(match { it.id == 77L && it.name == "New" && it.position == 9 })
        }
        coVerify { syncScheduler.scheduleSync() }
    }

    // ------------------------ applyDelta: Insert, Update, Delete lifts ------------------------

    @Test
    fun `applyDelta - insert lifts uses insertMany and schedules`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns emptyList()
        coEvery { workoutLiftsDao.insertMany(any()) } returns listOf(100L, 101L)

        val lift1Domain = buildWorkoutLiftEntity(id = 0L, workoutId = 10L, liftId = 200L, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 0, setCount = 3).toDomainModel()
        val lift2Domain = buildWorkoutLiftEntity(id = 0L, workoutId = 10L, liftId = 201L, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 1, setCount = 3).toDomainModel()

        val delta = programDelta {
            workout(10L) {
                lift(lift1Domain)
                lift(lift2Domain)
            }
        }

        repo.applyDelta(1L, delta)

        coVerify { workoutLiftsDao.insertMany(match { it.size == 2 && it.all { e -> e.workoutId == 10L } }) }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - update lifts merges fields and calls updateMany`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        val existing1 = buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 0, setCount = 3, deloadWeek = 1, repRangeTop = 10, repRangeBottom = 8, rpeTarget = 8f, stepSize = 2)
        val existing2 = buildWorkoutLiftEntity(id = 6L, workoutId = 10L, liftId = 201L, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 1, setCount = 3)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(existing1, buildLiftEntity(id = 200L)),
            buildWorkoutLiftWithRelationships(existing2, buildLiftEntity(id = 201L))
        )
        coEvery { workoutLiftsDao.updateMany(any()) } returns Unit

        val delta = programDelta {
            workout(10L) {
                // Update many scalar fields
                lift(5L, liftId = 202L, position = 2, setCount = 4, deloadWeek = 2, repRangeTop = 12, repRangeBottom = 9, rpeTarget = 9f, stepSize = 5) { }
                lift(6L, position = 0) { }
            }
        }

        repo.applyDelta(1L, delta)

        coVerify {
            workoutLiftsDao.updateMany(match {
                it.size == 2 &&
                        it.any { e -> e.id == 5L && e.position == 2 && e.liftId == 202L && e.setCount == 4 && e.deloadWeek == 2 && e.repRangeTop == 12 && e.repRangeBottom == 9 && e.rpeTarget == 9f && e.stepSize == 5 } &&
                        it.any { e -> e.id == 6L && e.position == 0 }
            })
        }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - update lifts referencing missing id throws`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns emptyList()

        val delta = programDelta { workout(10L) { lift(999L, position = 1) { } } }

        try {
            repo.applyDelta(1L, delta)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) { /* expected */ }
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - removeWorkoutLifts soft-deletes and cascades sets`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns emptyList()
        coEvery { workoutLiftsDao.softDeleteMany(listOf(5L, 6L)) } returns 2

        val delta = programDelta { workout(10L) { removeWorkoutLifts(5L, 6L) } }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            workoutLiftsDao.softDeleteMany(listOf(5L, 6L))
            customSetsDao.softDeleteByWorkoutLiftIds(listOf(5L, 6L))
            syncScheduler.scheduleSync()
        }
    }

    // ------------------------ applyDelta: Sets upsert + delete-by-ids + purge ------------------------

    @Test
    fun `applyDelta - upsert sets carries remote metadata and upserts many`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        val wl = buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 0, setCount = 3)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(buildWorkoutLiftWithRelationships(wl, buildLiftEntity(id = 200L)))
        val existing = buildCustomLiftSetEntity(id = 2L, workoutLiftId = 5L)
        coEvery { customSetsDao.getMany(listOf(2L)) } returns listOf(existing)
        coEvery { customSetsDao.upsertMany(any()) } returns listOf(-1L, 10L)

        val delta = programDelta {
            workout(10L) {
                lift(5L) {
                    set(existing.toDomainModel())
                    set(buildCustomLiftSetEntity(id = 0L, workoutLiftId = 5L, position = 2).toDomainModel())
                }
            }
        }

        repo.applyDelta(1L, delta)

        coVerify {
            customSetsDao.upsertMany(match {
                it.size == 2 &&
                        it.any { s -> s.id == 2L && s.workoutLiftId == 5L } &&
                        it.any { s -> s.id == 0L && s.workoutLiftId == 5L && s.position == 2 }
            })
        }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - delete sets by ids (single lift) groups by workoutLift and syncs positions for that lift`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L))
        )
        coEvery { customSetsDao.getMany(any()) } returns emptyList()
        coEvery { customSetsDao.getMaxPosition(listOf(1L, 2L, 3L)) } returns 3
        coEvery { customSetsDao.softDeleteMany(listOf(1L, 2L, 3L)) } returns 3
        coJustRun { customSetsDao.syncPositions(5L, 3) }

        val delta = programDelta {
            workout(10L) {
                lift(5L) { removeSets(1L, 2L, 3L) }
            }
        }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            customSetsDao.getMaxPosition(listOf(1L, 2L, 3L))
            customSetsDao.softDeleteMany(listOf(1L, 2L, 3L))
            customSetsDao.syncPositions(5L, 3)
            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - delete sets by ids (two lifts) calls getMaxPosition, delete, then syncPositions per group`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L)),
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 6L, workoutId = 10L, liftId = 201L), buildLiftEntity(id = 201L))
        )
        // Group A
        coEvery { customSetsDao.getMaxPosition(listOf(1L, 2L)) } returns 2
        coEvery { customSetsDao.softDeleteMany(listOf(1L, 2L)) } returns 2
        coJustRun { customSetsDao.syncPositions(5L, 2) }
        // Group B
        coEvery { customSetsDao.getMaxPosition(listOf(7L, 8L, 9L)) } returns 9
        coEvery { customSetsDao.softDeleteMany(listOf(7L, 8L, 9L)) } returns 3
        coJustRun { customSetsDao.syncPositions(6L, 9) }

        val delta = programDelta {
            workout(10L) {
                lift(5L) { removeSets(1L, 2L) }
                lift(6L) { removeSets(7L, 8L, 9L) }
            }
        }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            customSetsDao.getMaxPosition(listOf(1L, 2L))
            customSetsDao.softDeleteMany(listOf(1L, 2L))
            customSetsDao.syncPositions(5L, 2)

            customSetsDao.getMaxPosition(listOf(7L, 8L, 9L))
            customSetsDao.softDeleteMany(listOf(7L, 8L, 9L))
            customSetsDao.syncPositions(6L, 9)

            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - removeAllSets purges by workoutLiftId and schedules sync`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L))
        )
        coJustRun { customSetsDao.softDeleteByWorkoutLiftIds(listOf(5L)) }

        val delta = programDelta { workout(10L) { lift(5L) { removeAllSets() } } }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            customSetsDao.softDeleteByWorkoutLiftIds(listOf(5L))
            syncScheduler.scheduleSync()
        }
    }

    // ------------------------ applyDelta: Mixed combination scenario ------------------------
    @Test
    fun `applyDelta - mixed multi-lift multi-set delta results in single sync and correct grouped operations`() = runTest {
        // Program patch prerequisites
        coEvery { programsDao.get(1L) } returns buildProgramWithRelationships(buildProgramEntity(id = 1L, name = "Old", isActive = false, deloadWeek = 0, currentMesocycle = 1, currentMicrocycle = 1))

        // Existing workout (20L) and new workout (insert → 99L)
        val existingWorkout = buildWorkoutEntity(id = 20L, programId = 1L, name = "Existing", position = 0)
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(20L, 1L) } returns existingWorkout
        coEvery { workoutsDao.upsert(any()) } returnsMany listOf(-1L, 99L) // first updates 20L, second inserts new id 99L

        // Existing lifts for workout 20L
        val wl5 = buildWorkoutLiftEntity(id = 5L, workoutId = 20L, liftId = 200L, position = 0, setCount = 3)
        val wl6 = buildWorkoutLiftEntity(id = 6L, workoutId = 20L, liftId = 201L, position = 1, setCount = 3)
        coEvery { workoutLiftsDao.getForWorkout(20L) } returns listOf(buildWorkoutLiftWithRelationships(wl5, buildLiftEntity(id = 200L)), buildWorkoutLiftWithRelationships(wl6, buildLiftEntity(id = 201L)))

        // Lifts ops on existing workout
        coEvery { workoutLiftsDao.updateMany(any()) } returns Unit
        coEvery { workoutLiftsDao.softDeleteMany(listOf(6L)) } returns 1

        // Set deletions grouping under existing lift 5L
        coEvery { customSetsDao.getMaxPosition(listOf(21L, 22L)) } returns 22
        coEvery { customSetsDao.softDeleteMany(listOf(21L, 22L)) } returns 2
        coJustRun { customSetsDao.syncPositions(5L, 22) }
        // Purge sets for removed lift 6L
        coJustRun { customSetsDao.softDeleteByWorkoutLiftIds(listOf(6L)) }

        // Build complex delta: patch program + update existing workout + insert a brand-new workout (no nested ops on the inserted one)
        val newWorkoutDomain = buildWorkoutEntity(id = 0L, programId = 1L, name = "NewW", position = 1).toDomainModel()
        val existingSetDomain = buildCustomLiftSetEntity(id = 2L, workoutLiftId = 5L).toDomainModel()

        val delta = programDelta {
            updateProgram(name = "Patched", deloadWeek = 1)

            workout(20L, name = "Renamed", position = 0) {
                lift(5L, position = 1) {
                    set(existingSetDomain)
                    removeSets(21L, 22L)
                }
                removeWorkoutLifts(6L) // purge sets path
            }

            // Insert a new workout in the same delta — but no nested lift/set ops alongside the insert
            workout(newWorkoutDomain)
        }

        repo.applyDelta(1L, delta)

        // Verifications
        coVerify { programsDao.update(match { it.name == "Patched" && it.deloadWeek == 1 }) }
        coVerify { workoutsDao.upsert(match { it.id == 20L && it.name == "Renamed" }) }
        coVerify { workoutsDao.upsert(match { it.id == 0L && it.name == "NewW" }) }
        coVerify { workoutLiftsDao.updateMany(match { it.any { e -> e.id == 5L && e.position == 1 } }) }
        coVerify { workoutLiftsDao.softDeleteMany(listOf(6L)) }
        // grouped deletions on existing lift 5L
        coVerify { customSetsDao.getMaxPosition(listOf(21L, 22L)) }
        coVerify { customSetsDao.softDeleteMany(listOf(21L, 22L)) }
        coVerify { customSetsDao.syncPositions(5L, 22) }
        // purge path
        coVerify { customSetsDao.softDeleteByWorkoutLiftIds(listOf(6L)) }
        // one sync overall
        coVerify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - no-ops do not schedule sync`() = runTest {
        repo.applyDelta(1L, programDelta { })
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    // ------------------------ Other public functions ------------------------

    @Test
    fun `insert delegates to dao and maps domain to entity`() = runTest {
        val program = Program(id = 0L, name = "X", isActive = true, deloadWeek = 0, workouts = emptyList(), currentMesocycle = 0, currentMicrocycle = 0, currentMicrocyclePosition = 0)
        coEvery { programsDao.insert(any()) } returns 42L

        val id = repo.insert(program)

        assertEquals(42L, id)
        coVerify { programsDao.insert(match { it.name == "X" && it.id == 0L }) }
    }

    @Test
    fun `getNewest returns mapped domain or null`() = runTest {
        every { programsDao.getNewest() } returns buildProgramWithRelationships(buildProgramEntity(id = 3L, name = "Newest"))
        val p = repo.getNewest()
        assertNotNull(p)
        assertEquals("Newest", p!!.name)

        every { programsDao.getNewest() } returns null
        assertNull(repo.getNewest())
    }

    @Test
    fun `getForWorkout returns mapped domain or null`() = runTest {
        every { programsDao.getForWorkout(99L) } returns buildProgramWithRelationships(buildProgramEntity(id = 9L, name = "P"))
        val p = repo.getForWorkout(99L)
        assertNotNull(p)
        assertEquals(9L, p!!.id)

        every { programsDao.getForWorkout(99L) } returns null
        assertNull(repo.getForWorkout(99L))
    }

    @Test
    fun `getActive returns sorted program copy when present`() = runTest {
        coEvery { programsDao.getActive() } returns buildProgramWithRelationships(buildProgramEntity(id = 1L, name = "A"))
        val p = repo.getActive()
        assertNotNull(p)
        assertEquals(1L, p!!.id)
    }

    @Test
    fun `getActiveProgramFlow maps and sorts`() = runTest {
        val flow = MutableStateFlow<ProgramWithRelationshipsDto?>(buildProgramWithRelationships(buildProgramEntity(id = 1L, name = "A")))
        every { programsDao.getActiveFlow() } returns flow

        val first = repo.getActiveProgramFlow().first()
        assertNotNull(first)
        assertEquals(1L, first!!.id)
    }

    @Test
    fun `getDeloadWeek delegates to dao`() = runTest {
        coEvery { programsDao.getDeloadWeek(7L) } returns 2
        assertEquals(2, repo.getDeloadWeek(7L))
    }

    @Test
    fun `getActiveProgramMetadataFlow maps dto to domain`() = runTest {
        val flow = MutableStateFlow(
            ProgramMetadataDto(
                programId = 1L, name = "A", deloadWeek = 1, currentMesocycle = 2, currentMicrocycle = 3, currentMicrocyclePosition = 4, workoutCount = 5
            )
        )
        every { programsDao.getActiveProgramMetadata() } returns flow

        val meta = repo.getActiveProgramMetadataFlow().first()
        assertNotNull(meta)
        assertEquals(1L, meta!!.programId)
        assertEquals("A", meta.name)
        assertEquals(5, meta.workoutCount)
    }
}
