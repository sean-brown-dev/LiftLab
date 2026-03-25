package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class LiftMetricChartsDaoInstrumentedTest {
    private lateinit var db: LiftLabDatabase
    private lateinit var dao: LiftMetricChartsDao
    private lateinit var liftsDao: LiftsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.liftMetricChartsDao()
        liftsDao = db.liftsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // Unsynced
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        val a = liftMetricChart(1, synced = false, deleted = false)
        val b = liftMetricChart(2, synced = false, deleted = true)
        val c = liftMetricChart(3, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    // Remote ID
    @Test fun remoteId_gettersReturnDeletedToo() = runBlocking {
        val a = liftMetricChart(10, remoteId = "RID-LMC-10", deleted = false)
        val b = liftMetricChart(11, remoteId = "RID-LMC-11", deleted = true)
        dao.upsertMany(listOf(a, b))

        assertNotNull(dao.getByRemoteId("RID-LMC-10"))
        assertNotNull(dao.getByRemoteId("RID-LMC-11"))
        assertEquals(setOf(10L, 11L),
            dao.getManyByRemoteId(listOf("RID-LMC-10", "RID-LMC-11")).map { it.id }.toSet()
        )
    }


    // Standard getters
    @Test fun get_getAll_getAllWithNoLift_excludeDeleted() = runBlocking {
        val lift = lift(5)
        liftsDao.upsert(lift)

        val a = liftMetricChart(20, liftId = null, deleted = false)
        val b = liftMetricChart(21, liftId = null, deleted = true)
        val c = liftMetricChart(22, liftId = 5L, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertNotNull(dao.get(20)); assertNull(dao.get(21))
        val noLift = dao.getAllWithNoLift().map { it.id }.toSet()
        assertTrue(noLift.contains(20L))
        assertFalse(noLift.contains(21L))
        assertTrue(noLift.none { it == 22L })
    }

    // Mutations
    @Test fun deleteAll_and_softDelete_ops() = runBlocking {
        dao.upsertMany(listOf(liftMetricChart(40), liftMetricChart(41), liftMetricChart(42)))
        dao.softDelete(41)
        dao.softDeleteMany(listOf(40,42))
        assertTrue(dao.getAll().isEmpty())
        assertTrue(dao.getAllUnsynced().isNotEmpty())
    }
}
