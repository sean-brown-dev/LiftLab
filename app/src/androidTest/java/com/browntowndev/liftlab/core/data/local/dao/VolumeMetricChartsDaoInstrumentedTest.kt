package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class VolumeMetricChartsDaoInstrumentedTest {
// ==== Local factory lives in your project ====
private fun volumeChart(
    id: Long,
    remoteId: String = "RID-VMC-$id",
    liftId: Long? = null,
    deleted: Boolean = false,
    synced: Boolean = true
): VolumeMetricChartEntity {
    val e = VolumeMetricChartEntity(
        id = id,
        volumeType = VolumeType.POSTERIOR_DELTOID,
        volumeTypeImpact = VolumeTypeImpactSelection.PRIMARY,
    )
    return e.apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }
}


    private lateinit var db: LiftLabDatabase
    private lateinit var dao: VolumeMetricChartsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.volumeMetricChartsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // Unsynced
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        val a = volumeChart(1, synced = false, deleted = false)
        val b = volumeChart(2, synced = false, deleted = true)
        val c = volumeChart(3, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    // Remote ID
    @Test fun remoteId_gettersReturnDeletedToo() = runBlocking {
        val a = volumeChart(10, remoteId = "RID-VMC-10", deleted = false)
        val b = volumeChart(11, remoteId = "RID-VMC-11", deleted = true)
        dao.upsertMany(listOf(a, b))

        assertNotNull(dao.getByRemoteId("RID-VMC-10"))
        assertNotNull(dao.getByRemoteId("RID-VMC-11"))
        assertEquals(setOf(10L, 11L),
            dao.getManyByRemoteId(listOf("RID-VMC-10", "RID-VMC-11")).map { it.id }.toSet()
        )
    }

    // Standard getters
    @Test fun get_getMany_getAll_andFlow_excludeDeleted() = runBlocking {
        val a = volumeChart(20, deleted = false); val b = volumeChart(21, deleted = true); val c = volumeChart(22, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertNotNull(dao.get(20)); assertNull(dao.get(21))
        assertEquals(setOf(20L,22L), dao.getMany(listOf(20,21,22)).map { it.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAll().map { it.id }.toSet())
        assertEquals(setOf(20L,22L), dao.getAllFlow().first().map { it.id }.toSet())
    }

    // Mutations
    @Test fun deleteAll_and_softDelete_ops() = runBlocking {
        dao.upsertMany(listOf(volumeChart(40), volumeChart(41), volumeChart(42)))
        dao.softDelete(41)
        dao.softDeleteMany(listOf(40,42))
        assertTrue(dao.getAll().isEmpty())
        assertTrue(dao.getAllUnsynced().isNotEmpty())
    }
}
