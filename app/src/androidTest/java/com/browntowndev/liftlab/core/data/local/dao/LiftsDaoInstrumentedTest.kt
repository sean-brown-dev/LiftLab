package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class LiftsDaoInstrumentedTest {

    private lateinit var db: LiftLabDatabase
    private lateinit var dao: LiftsDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.liftsDao()
    }

    @AfterEach fun tearDown() { db.close() }

    // ---- Builders ----
    private fun lift(
        id: Long,
        remoteId: String? = "RID-LIFT-$id",
        movementPattern: MovementPattern = MovementPattern.LEG_PUSH,
        deleted: Boolean = false,
        synced: Boolean = true
    ): com.browntowndev.liftlab.core.data.local.entities.LiftEntity {
        return com.browntowndev.liftlab.core.data.local.entities.LiftEntity(
            id = id,
            name = "Lift $id",
            movementPattern = movementPattern,
            volumeTypesBitmask = 1
        ).apply {
            this.remoteId = remoteId
            this.deleted = deleted
            this.synced = synced
        }
    }

    // ---- Tests ----
    @Test fun getAllUnsynced_includesDeleted() = runBlocking {
        val a = lift(1, synced = false, deleted = false)
        val b = lift(2, synced = false, deleted = true)
        val c = lift(3, synced = true, deleted = false)
        dao.upsertMany(listOf(a,b,c))
        assertEquals(setOf(1L,2L), dao.getAllUnsynced().map { it.id }.toSet())
    }

    @Test fun remoteId_returnsDeletedToo() = runBlocking {
        val a = lift(10, remoteId = "RID-L-10", deleted = false)
        val b = lift(11, remoteId = "RID-L-11", deleted = true)
        dao.upsertMany(listOf(a,b))
        assertNotNull(dao.getByRemoteId("RID-L-10"))
        assertNotNull(dao.getByRemoteId("RID-L-11"))
        assertEquals(setOf(10L,11L), dao.getManyByRemoteId(listOf("RID-L-10","RID-L-11")).map { it.id }.toSet())
    }

    @Test fun getByCategory_excludesDeleted() = runBlocking {
        val keep = lift(20, movementPattern = MovementPattern.LEG_PUSH, deleted = false)
        val hide = lift(21, movementPattern = MovementPattern.LEG_PUSH, deleted = true)
        val other = lift(22, movementPattern = MovementPattern.HORIZONTAL_PULL, deleted = false)
        dao.upsertMany(listOf(keep, hide, other))
        val got = dao.getByCategory(MovementPattern.LEG_PUSH).map { it.id }.toSet()
        assertEquals(setOf(20L), got)
    }

    @Test fun softDelete_ops_and_deleteAll() = runBlocking {
        dao.upsertMany(listOf(lift(30), lift(31), lift(32)))
        dao.softDelete(31)
        dao.softDeleteMany(listOf(30,32))
        assertTrue(dao.getAllUnsynced().map { it.id }.containsAll(listOf(30L,31L,32L)))
        dao.deleteAll()
        // after deleteAll, there should be nothing regardless of deleted flag
        assertTrue(dao.getAllUnsynced().isEmpty())
    }
}
