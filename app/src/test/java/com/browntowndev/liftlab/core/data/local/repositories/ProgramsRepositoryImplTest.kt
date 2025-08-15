package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.common.Patch
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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration

/**
 * ProgramsRepositoryImpl test suite aligned with the latest repository semantics:
 * - Robust set position reindexing after deletions (no getMaxPosition / syncPositions)
 * - maybeUpsertWorkout: insert uses insertWorkoutAndChildren (inserts lifts + sets); update uses workoutsDao.update
 * - maybeUpsertWorkoutLifts: inserts via insertMany; updates via updateMany
 *
 * NOTE: Helper builders are preserved with the user's naming scheme (buildX).
 */
class ProgramsRepositoryImplTest {

    // ---------- Helper builders (preserve names) ----------

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
        workoutLiftEntity: WorkoutLiftEntity = buildWorkoutLiftEntity(),
        liftEntity: LiftEntity = buildLiftEntity(id = workoutLiftEntity.liftId),
        sets: List<CustomLiftSetEntity> = emptyList(),
    ) = WorkoutLiftWithRelationships(
        workoutLiftEntity = workoutLiftEntity,
        liftEntity = liftEntity,
        customLiftSetEntities = sets,
    )

    private fun buildWorkoutWithRelationships(
        workoutEntity: WorkoutEntity = buildWorkoutEntity(),
        lifts: List<WorkoutLiftWithRelationships> = emptyList(),
    ) = WorkoutWithRelationships(
        workoutEntity = workoutEntity,
        lifts = lifts,
    )

    private fun buildProgramWithRelationships(
        programEntity: ProgramEntity = buildProgramEntity(),
        workouts: List<WorkoutWithRelationships> = emptyList(),
    ) = ProgramWithRelationshipsDto(
        programEntity = programEntity,
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
    private lateinit var tx: TransactionScope
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
        tx = mockk(relaxed = true)
        coEvery { tx.execute(any<suspend () -> Unit>()) } coAnswers {
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
            tx
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
        coVerify(exactly = 1) { tx.execute(any<suspend () -> Unit>()) }
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
            updateProgram(name = Patch.Set("New"), deloadWeek = Patch.Set(4), currentMicrocycle = Patch.Set(5))
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
    fun `applyDelta - insert workout inserts only workout when no children present`() = runTest {
        // Insert-only: no children
        coEvery { workoutsDao.insert(any()) } returns 99L

        val newWorkoutDomain = buildWorkoutEntity(id = 0L, programId = 1L, name = "W", position = 0).toDomainModel()

        val delta = programDelta { workout(newWorkoutDomain) }

        repo.applyDelta(1L, delta)

        coVerify { workoutsDao.insert(match { it.programId == 1L && it.name == "W" && it.id == 0L }) }
        coVerify(exactly = 0) { workoutLiftsDao.insertMany(any()) }
        coVerify(exactly = 0) { customSetsDao.insertMany(any()) }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - insert workout inserts children (lifts and sets) when present`() = runTest {
        // Build a domain workout with children using DTO-to-domain mapping
        val w = buildWorkoutEntity(id = 0L, programId = 1L, name = "WithChildren", position = 0)
        val wl = buildWorkoutLiftEntity(id = 0L, workoutId = 0L, liftId = 300L, position = 0, setCount = 3)
        val c1 = buildCustomLiftSetEntity(id = 0L, workoutLiftId = 0L, position = 0)
        val c2 = buildCustomLiftSetEntity(id = 0L, workoutLiftId = 0L, position = 1)
        val wDto = buildWorkoutWithRelationships(
            workoutEntity = w,
            lifts = listOf(
                buildWorkoutLiftWithRelationships(wl, buildLiftEntity(id = 300L), sets = listOf(c1, c2))
            )
        )
        val newWorkoutDomain = wDto.toDomainModel() // includes lifts + sets

        coEvery { workoutsDao.insert(any()) } returns 77L
        coEvery { workoutLiftsDao.insertMany(any()) } returns listOf(700L) // returned id for wl
        coEvery { customSetsDao.insertMany(any()) } returns listOf(800L, 801L)

        val delta = programDelta { workout(newWorkoutDomain) }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            workoutsDao.insert(match { it.programId == 1L && it.name == "WithChildren" })
            workoutLiftsDao.insertMany(match { it.size == 1 && it[0].workoutId == 77L && it[0].liftId == 300L })
            customSetsDao.insertMany(match {
                it.size == 2 &&
                it.all { s -> s.workoutLiftId == 700L } &&
                it.map { s -> s.position } == listOf(0,1)
            })
            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - update workout missing throws`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(77L, 1L) } returns null

        val delta = programDelta { workout(77L, name = Patch.Set("X")) { } }

        assertThrows(IllegalStateException::class.java) {
            runTest { repo.applyDelta(1L, delta) }
        }
        coVerify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - update workout merges fields and calls update`() = runTest {
        val existing = buildWorkoutEntity(id = 77L, programId = 1L, name = "Old", position = 5)
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(77L, 1L) } returns existing
        coJustRun { workoutsDao.update(any()) }
        coEvery { workoutLiftsDao.getForWorkout(77L) } returns emptyList()

        val delta = programDelta { workout(77L, name = Patch.Set("New"), position = Patch.Set(9)) { } }

        repo.applyDelta(1L, delta)

        coVerify {
            workoutsDao.update(match { it.id == 77L && it.name == "New" && it.position == 9 })
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
                insertLift(lift1Domain)
                insertLift(lift2Domain)
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
                updateLift(5L, liftId = Patch.Set(202L), position = Patch.Set(2), setCount = Patch.Set(4), deloadWeek = Patch.Set(2), repRangeTop = Patch.Set(12), repRangeBottom = Patch.Set(9), rpeTarget = Patch.Set(9f), stepSize = Patch.Set(5)) { }
                updateLift(6L, position = Patch.Set(0)) { }
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

        val delta = programDelta { workout(10L) { updateLift(999L, position = Patch.Set(1)) { } } }

        assertThrows(IllegalStateException::class.java) {
            runTest { repo.applyDelta(1L, delta) }
        }
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
                updateSets(5L) {
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
    fun `applyDelta - delete sets by ids (single lift) deletes and reindexes remaining sets for that lift`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L))
        )
        coEvery { customSetsDao.getMany(any()) } returns emptyList()
        coEvery { customSetsDao.softDeleteMany(listOf(1L, 2L, 3L)) } returns 3

        // After deletions, remaining sets under lift 5 should be reindexed to 0..n-1
        val remaining = listOf(
            buildCustomLiftSetEntity(id = 10L, workoutLiftId = 5L, position = 5),
            buildCustomLiftSetEntity(id = 12L, workoutLiftId = 5L, position = 7),
        )
        coEvery { customSetsDao.getByWorkoutLiftId(5L) } returns remaining
        coJustRun { customSetsDao.updateMany(any()) }

        val delta = programDelta {
            workout(10L) {
                updateSets(5L) { removeSets(1L, 2L, 3L) }
            }
        }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            customSetsDao.softDeleteMany(listOf(1L, 2L, 3L))
            customSetsDao.getByWorkoutLiftId(5L)
            customSetsDao.updateMany(match { updated ->
                updated.size == 2 &&
                updated[0].id == 10L && updated[0].position == 0 &&
                updated[1].id == 12L && updated[1].position == 1
            })
            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - delete sets by ids (two lifts) deletes and reindexes remaining sets per group`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L)),
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 6L, workoutId = 10L, liftId = 201L), buildLiftEntity(id = 201L))
        )
        // Group A (lift 5L)
        coEvery { customSetsDao.softDeleteMany(listOf(1L, 2L)) } returns 2
        val remainingA = listOf(
            buildCustomLiftSetEntity(id = 10L, workoutLiftId = 5L, position = 9),
            buildCustomLiftSetEntity(id = 11L, workoutLiftId = 5L, position = 10),
        )
        coEvery { customSetsDao.getByWorkoutLiftId(5L) } returns remainingA
        // Group B (lift 6L)
        coEvery { customSetsDao.softDeleteMany(listOf(7L, 8L, 9L)) } returns 3
        val remainingB = listOf(
            buildCustomLiftSetEntity(id = 20L, workoutLiftId = 6L, position = 4),
            buildCustomLiftSetEntity(id = 21L, workoutLiftId = 6L, position = 5),
        )
        coEvery { customSetsDao.getByWorkoutLiftId(6L) } returns remainingB
        coJustRun { customSetsDao.updateMany(any()) }

        val delta = programDelta {
            workout(10L) {
                updateSets(5L) { removeSets(1L, 2L) }
                updateSets(6L) { removeSets(7L, 8L, 9L) }
            }
        }

        repo.applyDelta(1L, delta)

        coVerifyOrder {
            customSetsDao.softDeleteMany(listOf(1L, 2L))
            customSetsDao.getByWorkoutLiftId(5L)
            customSetsDao.updateMany(match { updated ->
                updated.size == 2 &&
                updated[0].id == 10L && updated[0].position == 0 &&
                updated[1].id == 11L && updated[1].position == 1
            })

            customSetsDao.softDeleteMany(listOf(7L, 8L, 9L))
            customSetsDao.getByWorkoutLiftId(6L)
            customSetsDao.updateMany(match { updated ->
                updated.size == 2 &&
                updated[0].id == 20L && updated[0].position == 0 &&
                updated[1].id == 21L && updated[1].position == 1
            })

            syncScheduler.scheduleSync()
        }
    }

    @Test
    fun `applyDelta - delete sets by ids with no remaining sets skips reindex`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L))
        )
        coEvery { customSetsDao.getMany(any()) } returns emptyList()
        coEvery { customSetsDao.softDeleteMany(listOf(1L, 2L)) } returns 2
        coEvery { customSetsDao.getByWorkoutLiftId(5L) } returns emptyList()

        val delta = programDelta {
            workout(10L) {
                updateSets(5L) { removeSets(1L, 2L) }
            }
        }

        repo.applyDelta(1L, delta)

        // No updateMany called when nothing remains
        coVerify(exactly = 0) { customSetsDao.updateMany(any()) }
        coVerify { syncScheduler.scheduleSync() }
    }

    @Test
    fun `applyDelta - removeAllSets purges by workoutLiftId and schedules sync`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(buildWorkoutLiftEntity(id = 5L, workoutId = 10L, liftId = 200L), buildLiftEntity(id = 200L))
        )
        coJustRun { customSetsDao.softDeleteByWorkoutLiftIds(listOf(5L)) }

        val delta = programDelta { workout(10L) { updateSets(5L) { removeAllSets() } } }

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
        coEvery { workoutsDao.update(any()) } returns Unit
        coEvery { workoutsDao.insert(any()) } returns 99L // new workout

        // Existing lifts for workout 20L
        val wl5 = buildWorkoutLiftEntity(id = 5L, workoutId = 20L, liftId = 200L, position = 0, setCount = 3)
        val wl6 = buildWorkoutLiftEntity(id = 6L, workoutId = 20L, liftId = 201L, position = 1, setCount = 3)
        coEvery { workoutLiftsDao.getForWorkout(20L) } returns listOf(
            buildWorkoutLiftWithRelationships(wl5, buildLiftEntity(id = 200L)),
            buildWorkoutLiftWithRelationships(wl6, buildLiftEntity(id = 201L))
        )

        // Lifts ops on existing workout
        coEvery { workoutLiftsDao.updateMany(any()) } returns Unit
        coEvery { workoutLiftsDao.softDeleteMany(listOf(6L)) } returns 1

        // Set deletions grouping under existing lift 5L - robust flow
        coEvery { customSetsDao.softDeleteMany(listOf(21L, 22L)) } returns 2
        val remainingAfterDelete = listOf(
            buildCustomLiftSetEntity(id = 30L, workoutLiftId = 5L, position = 3),
            buildCustomLiftSetEntity(id = 31L, workoutLiftId = 5L, position = 4),
        )
        coEvery { customSetsDao.getByWorkoutLiftId(5L) } returns remainingAfterDelete
        coJustRun { customSetsDao.updateMany(any()) }
        // Purge sets for removed lift 6L
        coJustRun { customSetsDao.softDeleteByWorkoutLiftIds(listOf(6L)) }

        // Build complex delta: patch program + update existing workout + insert a brand-new workout (no nested ops on the inserted one)
        val newWorkoutDomain = buildWorkoutEntity(id = 0L, programId = 1L, name = "NewW", position = 1).toDomainModel()
        val existingSetDomain = buildCustomLiftSetEntity(id = 2L, workoutLiftId = 5L).toDomainModel()

        val delta = programDelta {
            updateProgram(name = Patch.Set("Patched"), deloadWeek = Patch.Set(1))

            workout(20L, name = Patch.Set("Renamed"), position = Patch.Set(0)) {
                updateLift(5L, position = Patch.Set(1)) {
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
        coVerify { workoutsDao.update(match { it.id == 20L && it.name == "Renamed" }) }
        coVerify { workoutsDao.insert(match { it.id == 0L && it.name == "NewW" }) }
        coVerify { workoutLiftsDao.updateMany(match { it.any { e -> e.id == 5L && e.position == 1 } }) }
        coVerify { workoutLiftsDao.softDeleteMany(listOf(6L)) }
        // grouped deletions on existing lift 5L using new flow
        coVerifyOrder {
            customSetsDao.softDeleteMany(listOf(21L, 22L))
            customSetsDao.getByWorkoutLiftId(5L)
            customSetsDao.updateMany(match { updated ->
                updated.size == 2 &&
                updated[0].id == 30L && updated[0].position == 0 &&
                updated[1].id == 31L && updated[1].position == 1
            })
        }
        // purge path for removed lift
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

    @Test
    fun `applyDelta - update lifts with Patch_Set_null clears nullable scalar fields`() = runTest {
        coEvery { workoutsDao.getWithoutRelationshipsWithProgramValidation(10L, 1L) } returns buildWorkoutEntity(id = 10L, programId = 1L)
        val existing = buildWorkoutLiftEntity(
            id = 5L, workoutId = 10L, liftId = 200L,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            position = 0, setCount = 3,
            deloadWeek = 1, repRangeTop = 10, repRangeBottom = 8, rpeTarget = 8f, stepSize = 2
        )
        coEvery { workoutLiftsDao.getForWorkout(10L) } returns listOf(
            buildWorkoutLiftWithRelationships(existing, buildLiftEntity(id = 200L))
        )
        coEvery { workoutLiftsDao.updateMany(any()) } returns Unit

        val delta = programDelta {
            workout(10L) {
                // Explicitly CLEAR several fields
                updateLift(
                    workoutLiftId = 5L,
                    repRangeTop = Patch.Set(null),
                    repRangeBottom = Patch.Set(null),
                    rpeTarget = Patch.Set(null),
                    stepSize = Patch.Set(null)
                ) { }
            }
        }

        repo.applyDelta(1L, delta)

        coVerify {
            workoutLiftsDao.updateMany(match { list ->
                list.size == 1 && list[0].let { e ->
                    e.id == 5L &&
                            e.repRangeTop == null &&
                            e.repRangeBottom == null &&
                            e.rpeTarget == null &&
                            e.stepSize == null &&
                            // unchanged fields are preserved
                            e.deloadWeek == 1 &&
                            e.liftId == 200L &&
                            e.setCount == 3
                }
            })
        }
        coVerify { syncScheduler.scheduleSync() }
    }
}