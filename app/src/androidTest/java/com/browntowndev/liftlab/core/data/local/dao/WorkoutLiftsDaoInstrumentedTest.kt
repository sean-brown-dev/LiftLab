package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class WorkoutLiftsDaoInstrumentedTest {

    private lateinit var db: LiftLabDatabase
    private lateinit var dao: WorkoutLiftsDao
    private lateinit var programsDao: ProgramsDao
    private lateinit var workoutsDao: WorkoutsDao
    private lateinit var liftsDao: LiftsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutLiftsDao()
        programsDao = db.programsDao()
        workoutsDao = db.workoutsDao()
        liftsDao = db.liftsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    private fun workoutLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        progression: ProgressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        position: Int = 0,
        remoteId: String? = "RID-WL-$id",
        deleted: Boolean = false,
        synced: Boolean = true
    ): WorkoutLiftEntity = WorkoutLiftEntity(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        progressionScheme = progression,
        position = position,
        setCount = 3,
    ).apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }

    private suspend fun seedChain(pid: Long, wid: Long, lid: Long) {
        programsDao.upsertMany(listOf(program(pid)))
        workoutsDao.upsertMany(listOf(workout(wid, pid)))
        liftsDao.upsertMany(listOf(lift(lid)))
    }

    // ---- Tests ----
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        seedChain(1, 10, 100)
        val a = workoutLift(1, 10, 100, synced = false, deleted = false)
        val b = workoutLift(2, 10, 100, synced = false, deleted = true)
        val c = workoutLift(3, 10, 100, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    @Test fun remoteId_gettersReturnDeletedToo() = runBlocking {
        seedChain(1, 10, 100)
        val a = workoutLift(10, 10, 100, remoteId = "RID-WL-10", deleted = false)
        val b = workoutLift(11, 10, 100, remoteId = "RID-WL-11", deleted = true)
        dao.upsertMany(listOf(a,b))
        assertNotNull(dao.getByRemoteId("RID-WL-10"))
        assertNotNull(dao.getByRemoteId("RID-WL-11"))
        assertEquals(setOf(10L,11L), dao.getManyByRemoteId(listOf("RID-WL-10","RID-WL-11")).map { it.id }.toSet())
    }

    @Test fun get_getMany_getAll_andFlow_excludeDeleted() = runBlocking {
        seedChain(1, 10, 100)
        val a = workoutLift(20, 10, 100, deleted = false)
        val b = workoutLift(21, 10, 100, deleted = true)
        val c = workoutLift(22, 10, 100, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertNotNull(dao.getWithoutRelationships(20))
        assertNull(dao.getWithoutRelationships(21))
        assertNotNull(dao.getWithRelationships(20))
        assertNull(dao.getWithRelationships(21))
        assertEquals(setOf(20L,22L), dao.getManyWithoutRelationships(listOf(20,21,22)).map { it.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getMany(listOf(20,21,22)).map { it.workoutLiftEntity.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAll().map { it.workoutLiftEntity.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAllFlow().first().map { it.workoutLiftEntity.id }.toSet())
    }

    @Test fun getForWorkout_and_getLiftIds() = runBlocking {
        seedChain(1, 10, 100)
        val keep = workoutLift(30, 10, 100, deleted = false)
        val hide = workoutLift(31, 10, 100, deleted = true)
        val other = workoutLift(32, 11, 100, deleted = false).also {
            // ensure parent workout exists for ID 11
            programsDao.upsertMany(listOf(program(2)))
            workoutsDao.upsertMany(listOf(workout(11, 2)))
        }
        dao.upsertMany(listOf(keep, hide, other))
        assertEquals(setOf(30L), dao.getForWorkout(10).map { it.workoutLiftEntity.id }.toSet())
        assertEquals(listOf(100L), dao.getLiftIdsForWorkout(10))
    }

    @Test fun deleteAll_and_softDelete_ops() = runBlocking {
        seedChain(1, 10, 100)
        dao.upsertMany(listOf(workoutLift(40, 10, 100), workoutLift(41, 10, 100)))
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())

        dao.upsertMany(listOf(workoutLift(50, 10, 100), workoutLift(51, 10, 100)))
        dao.softDelete(51)
        assertEquals(setOf(50L), dao.getAll().map { it.workoutLiftEntity.id }.toSet())
        dao.softDeleteMany(listOf(50))
        assertTrue(dao.getAll().isEmpty())

        // soft deletes by scopes
        seedChain(2, 20, 200)
        dao.upsertMany(listOf(workoutLift(60, 20, 200), workoutLift(61, 20, 200)))
        dao.softDeleteByProgramId(2)
        assertTrue(dao.getAll().isEmpty())

        seedChain(3, 30, 300)
        dao.upsertMany(listOf(workoutLift(70, 30, 300), workoutLift(71, 30, 300)))
        dao.softDeleteByWorkoutId(30)
        assertTrue(dao.getAll().isEmpty())

        seedChain(4, 40, 400)
        dao.upsertMany(listOf(workoutLift(80, 40, 400), workoutLift(81, 40, 400)))
        dao.softDeleteByWorkoutIds(listOf(40))
        assertTrue(dao.getAll().isEmpty())
    }
}
