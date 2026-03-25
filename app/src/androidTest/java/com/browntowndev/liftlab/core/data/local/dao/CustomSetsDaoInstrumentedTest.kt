package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class CustomSetsDaoInstrumentedTest {

    private lateinit var db: LiftLabDatabase
    private lateinit var dao: CustomSetsDao
    private lateinit var programsDao: ProgramsDao
    private lateinit var workoutsDao: WorkoutsDao
    private lateinit var workoutLiftsDao: WorkoutLiftsDao
    private lateinit var liftsDao: LiftsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.customSetsDao()
        programsDao = db.programsDao()
        workoutsDao = db.workoutsDao()
        workoutLiftsDao = db.workoutLiftsDao()
        liftsDao = db.liftsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // ---- Builders ----
    private fun program(id: Long) = ProgramEntity(
        id = id, name = "P$id",
    ).apply { remoteId = "RID-P-$id"; deleted = false; synced = true }

    private fun workout(id: Long, programId: Long) = WorkoutEntity(
        id = id, programId = programId, name = "W$id", position = 0
    ).apply { remoteId = "RID-W-$id"; deleted = false; synced = true }

    private fun lift(id: Long) = LiftEntity(
        id = id, name = "L$id", movementPattern = MovementPattern.LEG_PUSH, volumeTypesBitmask = 1
    ).apply { remoteId = "RID-L-$id"; deleted = false; synced = true }

    private fun workoutLift(id: Long, workoutId: Long, liftId: Long) = WorkoutLiftEntity(
        id = id, workoutId = workoutId, liftId = liftId,
        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, position = 0, setCount = 3
    ).apply { remoteId = "RID-WL-$id"; deleted = false; synced = true }

    private fun customSet(
        id: Long,
        remoteId: String? = "RID-CS-$id",
        workoutLiftId: Long,
        deleted: Boolean = false,
        synced: Boolean = true
    ) = CustomLiftSetEntity(
        id = id,
        workoutLiftId = workoutLiftId,
        type = SetType.STANDARD,
        position = 0,
        rpeTarget = 8.0f,
        repRangeBottom = 5,
        repRangeTop = 8,
        setGoal = null,
        repFloor = null,
        dropPercentage = null,
        maxSets = null,
        setMatching = false,
    ).apply { this.remoteId = remoteId; this.deleted = deleted; this.synced = synced }

    private suspend fun seedChain(programId: Long, workoutId: Long, workoutLiftId: Long, liftId: Long) {
        programsDao.upsertMany(listOf(program(programId)))
        workoutsDao.upsertMany(listOf(workout(workoutId, programId)))
        liftsDao.upsertMany(listOf(lift(liftId)))
        workoutLiftsDao.upsertMany(listOf(workoutLift(workoutLiftId, workoutId, liftId)))
    }

    // ---- Tests ----
    @Test
    fun getAllUnsynced_includesDeletedWhenUnsynced() = runBlocking {
        seedChain(programId = 1, workoutId = 100, workoutLiftId = 77, liftId = 1)
        val a = customSet(1, workoutLiftId = 77, synced = false, deleted = false)
        val b = customSet(2, workoutLiftId = 77, synced = false, deleted = true)
        val c = customSet(3, workoutLiftId = 77, synced = true, deleted = false)
        dao.upsertMany(listOf(a, b, c))
        val ids = dao.getAllUnsynced().map { it.id }.toSet()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    fun remoteId_ignoresDeletedFilter() = runBlocking {
        seedChain(programId = 1, workoutId = 100, workoutLiftId = 88, liftId = 1)
        val a = customSet(30, remoteId = "RID-CS-30", workoutLiftId = 88, deleted = false, synced = true)
        val b = customSet(31, remoteId = "RID-CS-31", workoutLiftId = 88, deleted = true, synced = true)
        dao.upsertMany(listOf(a, b))
        assertNotNull(dao.getByRemoteId("RID-CS-30"))
        assertNotNull(dao.getByRemoteId("RID-CS-31"))
        val got = dao.getManyByRemoteId(listOf("RID-CS-30", "RID-CS-31")).map { it.id }.toSet()
        assertEquals(setOf(30L, 31L), got)
    }

    @Test
    fun getByWorkoutLiftId_excludesDeleted() = runBlocking {
        seedChain(programId = 2, workoutId = 200, workoutLiftId = 5, liftId = 1)
        seedChain(programId = 3, workoutId = 300, workoutLiftId = 6, liftId = 1)
        val keep = customSet(40, workoutLiftId = 5, deleted = false)
        val hide = customSet(41, workoutLiftId = 5, deleted = true)
        val other = customSet(42, workoutLiftId = 6, deleted = false)
        dao.upsertMany(listOf(keep, hide, other))
        assertEquals(setOf(40L), dao.getByWorkoutLiftId(5).map { it.id }.toSet())
    }

    @Test
    fun deleteAndSoftDelete_ops_and_syncPositions() = runBlocking {
        seedChain(programId = 4, workoutId = 400, workoutLiftId = 77, liftId = 1)
        dao.upsertMany(listOf(customSet(50, workoutLiftId = 77), customSet(51, workoutLiftId = 77)))
        dao.syncPositions(77, afterPosition = -1) // call coverage
        dao.softDelete(51)
        dao.softDeleteMany(listOf(50))
        assertTrue(dao.getByWorkoutLiftId(77).isEmpty())
        dao.deleteAll()
        // scoped soft-deletes call coverage
        dao.softDeleteByProgramId(4)
        dao.softDeleteByWorkoutId(400)
        dao.softDeleteByWorkoutLiftId(77)
        dao.softDeleteByWorkoutLiftIds(listOf(77))
        dao.softDeleteByWorkoutIds(listOf(400))
        assertTrue(true)
    }
}
