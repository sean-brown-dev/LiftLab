package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class WorkoutLogEntryDaoInstrumentedTest {
    private lateinit var db: LiftLabDatabase
    private lateinit var dao: WorkoutLogEntryDao
    private lateinit var histWorkoutNamesDao: HistoricalWorkoutNamesDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutLogEntriesDao()
        histWorkoutNamesDao = db.historicalWorkoutNamesDao()

        runBlocking {
            val histWorkoutName = histWorkoutName(1L)
            histWorkoutNamesDao.upsert(histWorkoutName)
        }
    }

    @AfterEach fun tearDown() { db.close() }

    // Unsynced
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        val a = workoutLogEntry(1, synced = false, deleted = false)
        val b = workoutLogEntry(2, synced = false, deleted = true)
        val c = workoutLogEntry(3, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    // Remote ID
    @Test fun remoteId_gettersReturnDeletedToo() = runBlocking {
        val a = workoutLogEntry(10, remoteId = "RID-A", deleted = false)
        val b = workoutLogEntry(11, remoteId = "RID-B", deleted = true)
        dao.upsertMany(listOf(a,b))
        assertNotNull(dao.getByRemoteId("RID-A"))
        assertNotNull(dao.getByRemoteId("RID-B"))
        assertEquals(setOf(10L,11L), dao.getManyByRemoteId(listOf("RID-A","RID-B")).map { it.id }.toSet())
    }

    // Standard getters
    @Test fun get_getMany_getAll_excludeDeleted() = runBlocking {
        val a = workoutLogEntry(20, deleted = false); val b = workoutLogEntry(21, deleted = true); val c = workoutLogEntry(22, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertNotNull(dao.get(20)); assertNull(dao.get(21))
        assertEquals(setOf(20L,22L), dao.getMany(listOf(20,21,22)).map { it.id }.toSet())
        assertEquals(2, dao.getAll().size)
    }

    // Mutations
    @Test fun softDelete_ops_and_deleteAll() = runBlocking {
        dao.upsertMany(listOf(workoutLogEntry(30), workoutLogEntry(31), workoutLogEntry(32)))
        dao.softDelete(31)
        dao.softDeleteMany(listOf(30,32))
        assertTrue(dao.getAll().isEmpty())
        assertTrue(dao.getAllUnsynced().isNotEmpty())
    }
}
