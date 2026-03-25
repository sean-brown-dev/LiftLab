package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class WorkoutsDaoInstrumentedTest {

    private lateinit var db: LiftLabDatabase
    private lateinit var dao: WorkoutsDao
    private lateinit var programsDao: ProgramsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutsDao()
        programsDao = db.programsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // ---- Tests ----
    @Test
    fun getAllUnsynced_includesDeleted() = runBlocking {
        programsDao.upsertMany(listOf(program(1)))
        val a = workout(1, programId = 1, synced = false, deleted = false)
        val b = workout(2, programId = 1, synced = false, deleted = true)
        val c = workout(3, programId = 1, synced = true, deleted = false)
        dao.upsertMany(listOf(a, b, c))
        val ids = dao.getAllUnsynced().map { it.id }.toSet()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    fun remoteId_gettersReturnDeletedToo() = runBlocking {
        programsDao.upsertMany(listOf(program(1)))
        val a = workout(10, programId = 1, remoteId = "RID-W-10", deleted = false)
        val b = workout(11, programId = 1, remoteId = "RID-W-11", deleted = true)
        dao.upsertMany(listOf(a, b))
        assertNotNull(dao.getByRemoteId("RID-W-10"))
        assertNotNull(dao.getByRemoteId("RID-W-11"))
        assertEquals(setOf(10L, 11L), dao.getManyByRemoteId(listOf("RID-W-10", "RID-W-11")).map { it.id }.toSet())
    }

    @Test
    fun standardGetters_excludeDeleted() = runBlocking {
        programsDao.upsertMany(listOf(program(1)))

        // Explicit positions so we can validate microcycle lookups:
        // pos 0 -> non-deleted, pos 1 -> deleted, pos 2 -> non-deleted
        val a = workout(20, programId = 1, position = 0, deleted = false)
        val b = workout(21, programId = 1, position = 1, deleted = true)
        val c = workout(22, programId = 1, position = 2, deleted = false)
        dao.upsertMany(listOf(a, b, c))

        // Standard “exclude deleted” checks
        assertNotNull(dao.getWithoutRelationships(20))
        assertNull(dao.getWithoutRelationships(21))
        assertEquals(setOf(20L, 22L), dao.getManyWithoutRelationships(listOf(20, 21, 22)).map { it.id }.toSet())
        assertEquals(setOf(20L, 22L), dao.getAllForProgramWithoutRelationships(1).map { it.id }.toSet())
        assertEquals(setOf(20L, 22L), dao.getAllForProgram(1).map { it.workoutEntity.id }.toSet())
        assertEquals(setOf(20L, 22L), dao.getAll().map { it.workoutEntity.id }.toSet())
        assertEquals(setOf(20L, 22L), dao.getAllFlow().first().map { it.workoutEntity.id }.toSet())
        assertEquals(setOf(20L, 22L), dao.getMany(listOf(20, 21, 22)).map { it.workoutEntity.id }.toSet())
        assertNotNull(dao.get(20))
        assertNull(dao.get(21))
        assertNotNull(dao.getByIdFlow(20).first())
        assertNull(dao.getByIdFlow(21).first())
        assertNotNull(dao.getWithoutRelationshipsWithProgramValidation(20, 1))
        assertNull(dao.getWithoutRelationshipsWithProgramValidation(20, 999))

        // Final position should reflect the highest non-deleted position (2 here).
        assertEquals(2, dao.getFinalPosition(1))

        // Microcycle position lookups should ignore deleted rows:
        assertNotNull(dao.getByMicrocyclePosition(1, 0).first()) // pos 0 exists & is not deleted
        assertNull(dao.getByMicrocyclePosition(1, 1).first())    // pos 1 exists but is deleted -> filtered
        assertNotNull(dao.getByMicrocyclePosition(1, 2).first()) // pos 2 exists & is not deleted
    }

    @Test
    fun deleteAll_and_softDeletes() = runBlocking {
        programsDao.upsertMany(listOf(program(1)))
        dao.upsertMany(listOf(workout(30, 1), workout(31, 1)))
        assertTrue(dao.getAll().isNotEmpty())
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())

        dao.upsertMany(listOf(workout(40, 1), workout(41, 1)))
        dao.softDelete(41)
        assertEquals(setOf(40L), dao.getAll().map { it.workoutEntity.id }.toSet())
        dao.softDeleteMany(listOf(40))
        assertTrue(dao.getAll().isEmpty())

        dao.upsertMany(listOf(workout(50, 1)))
        dao.softDeleteByProgramId(1)
        assertTrue(dao.getAll().isEmpty())
    }
}
