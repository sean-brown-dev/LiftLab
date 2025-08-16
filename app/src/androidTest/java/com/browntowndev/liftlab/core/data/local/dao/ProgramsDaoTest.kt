package com.browntowndev.liftlab.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Date

class ProgramsDaoTest {

    private lateinit var context: Context
    private lateinit var db: LiftLabDatabase
    private lateinit var dao: ProgramsDao

    @BeforeEach
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LiftLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.programsDao()
    }

    @AfterEach
    fun teardown() {
        db.close()
    }

    private fun program(
        id: Long,
        isActive: Boolean,
        remoteId: String? = "rid-$id",
        lastUpdated: Date? = Date(1000 * id)
    ) = ProgramEntity(
        id = id,
        name = "P$id",
        deloadWeek = 0,
        isActive = isActive,
        currentMicrocycle = 0,
        currentMicrocyclePosition = 0,
        currentMesocycle = 0
    ).apply {
        this.remoteId = remoteId
        this.remoteLastUpdated = lastUpdated
        this.deleted = false
        this.synced = true
    }

    @Test
    @DisplayName("insertAndGetAllActive returns only active programs")
    fun insertAndGetAllActive_returnsOnlyActivePrograms() = runBlocking {
        dao.upsertMany(listOf(program(1, true), program(2, true), program(3, false)))
        val ids = dao.getAllActive().map { it.programEntity.id }.toSet()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    @DisplayName("getManyByRemoteId maps correctly (ignores order)")
    fun getManyByRemoteId_mapsCorrectly() = runBlocking {
        dao.upsertMany(
            listOf(
                program(10, true, remoteId = "cloud-10"),
                program(11, false, remoteId = "cloud-11"),
                program(12, true, remoteId = "cloud-12")
            )
        )
        val ids = dao.getManyByRemoteId(listOf("cloud-12", "cloud-10"))
            .map { it.programEntity.id }
            .toSet()

        // Order not guaranteed by DAO; assert set equality.
        assertEquals(setOf(12L, 10L), ids)
    }

    @Test
    @DisplayName("updateMany can deactivate extras (policy helper behavior)")
    fun updateMany_canDeactivateExtras_policyHelperBehavior() = runBlocking {
        val p21 = program(21, true)
        val p22 = program(22, true)
        val p23 = program(23, true)
        dao.upsertMany(listOf(p21, p22, p23))

        dao.updateMany(listOf(p22.copy(isActive = false), p23.copy(isActive = false)))

        val activeIds = dao.getAllActive().map { it.programEntity.id }
        assertEquals(listOf(21L), activeIds)
    }

    @Test
    @DisplayName("deleteMany removes rows by entity")
    fun deleteMany_removesRowsByEntity() = runBlocking {
        val p31 = program(31, false)
        val p32 = program(32, false)
        val p33 = program(33, false)
        dao.upsertMany(listOf(p31, p32, p33))

        val removed = dao.deleteMany(listOf(p32, p33))
        assertEquals(2, removed)

        val remaining = dao.getMany(listOf(31L, 32L, 33L)).map { it.programEntity.id }
        assertEquals(listOf(31L), remaining)
    }

    @Test
    @DisplayName("getManyByRemoteId default (includeDeleted = true) returns deleted and non-deleted")
    fun getManyByRemoteId_includesDeletedByDefault() = runBlocking {
        val a = program(201, isActive = false, remoteId = "rid-a")
        val b = program(202, isActive = false, remoteId = "rid-b")
        dao.upsertMany(listOf(a, b))

        // Soft delete 'b'
        dao.softDeleteMany(listOf(202L))

        // Default call should include both
        val resultIds = dao.getManyByRemoteId(listOf("rid-a", "rid-b"))
            .map { it.programEntity.id }
            .toSet()

        assertEquals(setOf(201L, 202L), resultIds)
    }

    @Test
    @DisplayName("getManyByRemoteId with [includeDeleted = false] excludes deleted rows")
    fun getManyByRemoteId_excludesDeletedWhenFlagFalse() = runBlocking {
        val a = program(211, isActive = false, remoteId = "rid-a")
        val b = program(212, isActive = false, remoteId = "rid-b")
        dao.upsertMany(listOf(a, b))

        // Soft delete 'b'
        dao.softDeleteMany(listOf(212L))

        // Call with flag false should exclude deleted
        val resultIds = dao.getManyByRemoteId(listOf("rid-a", "rid-b"), includeDeleted = false)
            .map { it.programEntity.id }
            .toSet()

        assertEquals(setOf(211L), resultIds)
    }

    // ---------------------------
    // New "deleted filtering" tests
    // ---------------------------

    @Test
    @DisplayName("getAllActive excludes deleted programs")
    fun getAllActive_excludesDeletedPrograms() = runBlocking {
        val a = program(100, isActive = true)
        val b = program(101, isActive = true)
        dao.upsertMany(listOf(a, b))

        // Soft-delete 'b'
        dao.softDeleteMany(listOf(b.id))

        val ids = dao.getAllActive().map { it.programEntity.id }.toSet()
        assertEquals(setOf(100L), ids)
    }

    @Test
    @DisplayName("getMany excludes deleted programs")
    fun getMany_excludesDeletedPrograms() = runBlocking {
        val a = program(120, isActive = false)
        val b = program(121, isActive = false)
        val c = program(122, isActive = false)
        dao.upsertMany(listOf(a, b, c))

        // Delete 'b'
        dao.softDeleteMany(listOf(121L))

        val resultIds = dao.getMany(listOf(120L, 121L, 122L))
            .map { it.programEntity.id }
            .toSet()

        assertEquals(setOf(120L, 122L), resultIds)
    }

    @Test
    @DisplayName("getAllUnsynced returns all unsynced programs, including deleted ones")
    fun getAllUnsynced_includesDeletedPrograms() = runBlocking {
        val a = program(130, isActive = false).apply { synced = false } // not deleted, unsynced
        val b = program(131, isActive = false).apply { synced = false } // will be deleted + unsynced
        val c = program(132, isActive = false).apply { synced = true }  // synced, should be excluded
        dao.upsertMany(listOf(a, b, c))

        // Mark 'b' deleted after insert
        dao.softDeleteMany(listOf(131L))

        val resultIds = dao.getAllUnsynced()  // expect both a and b
            .map { it.programEntity.id }
            .toSet()

        assertEquals(setOf(130L, 131L), resultIds)
    }
}
