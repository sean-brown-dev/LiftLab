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

class HistoricalWorkoutNamesDaoInstrumentedTest {
    private lateinit var db: LiftLabDatabase
    private lateinit var dao: HistoricalWorkoutNamesDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historicalWorkoutNamesDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // Unsynced
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        val a = histWorkoutName(1, synced = false, deleted = false)
        val b = histWorkoutName(2, synced = false, deleted = true)
        val c = histWorkoutName(3, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    // Remote ID
    @Test fun remoteId_gettersReturnDeletedToo() = runBlocking {
        val a = histWorkoutName(10, deleted = false).apply { remoteId = "RID-HWN-10" }
        val b = histWorkoutName(11, deleted = true ).apply { remoteId = "RID-HWN-11" }
        dao.upsertMany(listOf(a, b))

        assertNotNull(dao.getByRemoteId("RID-HWN-10"))
        assertNotNull(dao.getByRemoteId("RID-HWN-11"))
        assertEquals(setOf(10L, 11L),
            dao.getManyByRemoteId(listOf("RID-HWN-10", "RID-HWN-11")).map { it.id }.toSet()
        )
    }


    // Standard getters
    @Test fun get_getMany_getAll_excludeDeleted() = runBlocking {
        val a = histWorkoutName(20, deleted = false); val b = histWorkoutName(21, deleted = true); val c = histWorkoutName(22, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertNotNull(dao.get(20)); assertNull(dao.get(21))
        assertEquals(setOf(20L,22L), dao.getMany(listOf(20,21,22)).map { it.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAll().map { it.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAllFlow().first().map { it.id }.toSet())
    }

    @Test fun getByProgramAndWorkoutId_excludesDeleted() = runBlocking {
        val keep = histWorkoutName(30, programId = 99, workoutId = 199, deleted = false)
        val hidden = histWorkoutName(31, programId = 99, workoutId = 199, deleted = true)
        dao.upsertMany(listOf(keep, hidden))
        assertNotNull(dao.getByProgramAndWorkoutId(99, 199))
    }

    // Mutations
    @Test fun deleteAll_and_softDelete_ops() = runBlocking {
        dao.upsertMany(listOf(histWorkoutName(40), histWorkoutName(41), histWorkoutName(42)))
        dao.softDelete(41)
        dao.softDeleteMany(listOf(40,42))
        assertTrue(dao.getAll().isEmpty())
        assertTrue(dao.getAllUnsynced().isNotEmpty())
    }
}
