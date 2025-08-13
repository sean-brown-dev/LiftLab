@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit, no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private data class TestSet(
    override val id: Long,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
) : GenericLiftSet

class CustomLiftSetsRepositoryImplTest {

    @MockK lateinit var dao: CustomSetsDao
    @MockK lateinit var sync: SyncScheduler

    private lateinit var repo: CustomLiftSetsRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repo = CustomLiftSetsRepositoryImpl(dao, sync)

        // Mock top-level extension functions used by the repository
        // Mapping: com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensionsKt
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensionsKt")

        // Firestore metadata extension: generated file name pattern "${className}CopyWithMetadata"
        // => com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntityCopyWithMetadataKt
        mockkStatic("com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntityCopyWithMetadataKt")

        // Default no-op stubs; individual tests override as needed
        every { any<CustomLiftSetEntity>().toDomainModel() } answers {
            val e = firstArg<CustomLiftSetEntity>()
            TestSet(
                id = e.id,
                workoutLiftId = e.workoutLiftId,
                position = e.position,
                rpeTarget = e.rpeTarget,
                repRangeBottom = e.repRangeBottom,
                repRangeTop = e.repRangeTop,
            )
        }
        every { any<GenericLiftSet>().toEntity() } answers {
            val m = firstArg<GenericLiftSet>()
            CustomLiftSetEntity(
                id = m.id,
                workoutLiftId = m.workoutLiftId,
                type = SetType.STANDARD,
                position = m.position,
                rpeTarget = m.rpeTarget,
                repRangeBottom = m.repRangeBottom,
                repRangeTop = m.repRangeTop,
            )
        }
        every {
            any<CustomLiftSetEntity>().applyRemoteStorageMetadata(
                remoteId = any(),
                remoteLastUpdated = any(),
                synced = any()
            )
        } answers { firstArg() } // return same instance to keep assertions simple
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ---------- Queries ----------

    @Test
    fun `getAll maps entities to domain`() = runTest {
        val e1 = entity(id = 1L)
        val e2 = entity(id = 2L)
        coEvery { dao.getAll() } returns listOf(e1, e2)

        val result = repo.getAll()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
        coVerify(exactly = 1) { dao.getAll() }
    }

    @Test
    fun `getAllFlow maps entities to domain (turbine)`() = runTest {
        val e1 = entity(id = 10L)
        coEvery { dao.getAllFlow() } returns flowOf(listOf(e1))

        repo.getAllFlow().test {
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals(10L, first.first().id)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) {
            @Suppress("UnusedFlow")
            dao.getAllFlow()
        }
    }

    @Test
    fun `getById returns mapped model when found`() = runTest {
        val e = entity(id = 5L)
        coEvery { dao.get(5L) } returns e

        val r = repo.getById(5L)

        assertNotNull(r)
        assertEquals(5L, r!!.id)
        coVerify(exactly = 1) { dao.get(5L) }
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.get(99L) } returns null

        val r = repo.getById(99L)

        assertNull(r)
        coVerify(exactly = 1) { dao.get(99L) }
    }

    @Test
    fun `getMany maps entities to domain`() = runTest {
        val e1 = entity(id = 1L)
        val e2 = entity(id = 2L)
        coEvery { dao.getMany(listOf(1L, 2L)) } returns listOf(e1, e2)

        val r = repo.getMany(listOf(1L, 2L))

        assertEquals(listOf(1L, 2L), r.map { it.id })
        coVerify(exactly = 1) { dao.getMany(listOf(1L, 2L)) }
    }

    // ---------- Update ----------

    @Test
    fun `update does nothing when current is missing`() = runTest {
        val m = TestSet(1L, 100L, 0, 8f, 5, 8)
        coEvery { dao.get(1L) } returns null

        repo.update(m)

        coVerify(exactly = 1) { dao.get(1L) }
        coVerify(exactly = 0) { dao.update(any()) }
        io.mockk.verify(exactly = 0) { sync.scheduleSync() }
    }

    @Test
    fun `update applies metadata, updates, and schedules sync`() = runTest {
        val m = TestSet(1L, 100L, 0, 8f, 5, 8)
        val current = entity(id = 1L)
        coEvery { dao.get(1L) } returns current
        coEvery { dao.update(any()) } just Runs

        repo.update(m)

        coVerify(exactly = 1) { dao.get(1L) }
        coVerify(exactly = 1) { dao.update(any()) }
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    @Test
    fun `updateMany returns early when no current entities`() = runTest {
        val m1 = TestSet(1L, 10L, 0, 8f, 5, 8)
        val m2 = TestSet(2L, 10L, 1, 8f, 5, 8)
        coEvery { dao.getMany(listOf(1L, 2L)) } returns emptyList()

        repo.updateMany(listOf(m1, m2))

        coVerify(exactly = 1) { dao.getMany(listOf(1L, 2L)) }
        coVerify(exactly = 0) { dao.updateMany(any()) }
        io.mockk.verify(exactly = 0) { sync.scheduleSync() }
    }

    @Test
    fun `updateMany updates only models that exist and schedules sync`() = runTest {
        val m1 = TestSet(1L, 10L, 0, 8f, 5, 8)
        val m2 = TestSet(2L, 10L, 1, 8f, 5, 8)
        val currentOnlyM1 = entity(id = 1L)
        coEvery { dao.getMany(listOf(1L, 2L)) } returns listOf(currentOnlyM1)
        coEvery { dao.updateMany(any()) } returns Unit

        repo.updateMany(listOf(m1, m2))

        // Only id=1 should be in the update call
        coVerify(exactly = 1) { dao.updateMany(withArg { list ->
            assertEquals(1, list.size)
            assertEquals(1L, list.first().id)
        }) }
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    // ---------- Upsert & Insert ----------

    @Test
    fun `upsert returns existing id when DAO returns -1 and schedules sync`() = runTest {
        val model = TestSet(100L, 9L, 0, 8f, 5, 8)
        coEvery { dao.upsert(any()) } returns -1L

        val ret = repo.upsert(model)

        assertEquals(100L, ret)
        coVerify(exactly = 1) { dao.upsert(any()) }
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    @Test
    fun `upsert returns DAO id when not -1 and schedules sync`() = runTest {
        val model = TestSet(100L, 9L, 0, 8f, 5, 8)
        coEvery { dao.upsert(any()) } returns 55L

        val ret = repo.upsert(model)

        assertEquals(55L, ret)
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    @Test
    fun `upsertMany resolves -1 entries to original entity ids and schedules sync`() = runTest {
        val m1 = TestSet(1L, 9L, 0, 8f, 5, 8)
        val m2 = TestSet(2L, 9L, 1, 8f, 5, 8)
        val m3 = TestSet(3L, 9L, 2, 8f, 5, 8)

        // dao.upsertMany returns [-1, 10, -1]
        coEvery { dao.upsertMany(any()) } returns listOf(-1L, 10L, -1L)

        val ids = repo.upsertMany(listOf(m1, m2, m3))

        assertEquals(listOf(1L, 10L, 3L), ids)
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    @Test
    fun `insertMany returns ids and schedules sync`() = runTest {
        val m1 = TestSet(1L, 9L, 0, 8f, 5, 8)
        val m2 = TestSet(2L, 9L, 1, 8f, 5, 8)
        coEvery { dao.insertMany(any()) } returns listOf(11L, 12L)

        val ids = repo.insertMany(listOf(m1, m2))

        assertEquals(listOf(11L, 12L), ids)
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    @Test
    fun `insert returns id and schedules sync`() = runTest {
        val m = TestSet(1L, 9L, 0, 8f, 5, 8)
        coEvery { dao.insert(any()) } returns 77L

        val id = repo.insert(m)

        assertEquals(77L, id)
        io.mockk.verify(exactly = 1) { sync.scheduleSync() }
    }

    // ---------- Deletes ----------

    @Nested
    inner class DeleteByPosition {

        @Test
        fun `no sets for lift = returns 0 and no sync`() = runTest {
            coEvery { dao.getByWorkoutLiftId(333L) } returns emptyList()

            val count = repo.deleteByPosition(333L, 0)

            assertEquals(0, count)
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `no set matches position = returns 0 and no sync`() = runTest {
            val e1 = entity(id = 1L, position = 0)
            val e2 = entity(id = 2L, position = 1)
            coEvery { dao.getByWorkoutLiftId(333L) } returns listOf(e1, e2)

            val count = repo.deleteByPosition(333L, position = 5)

            assertEquals(0, count)
            coVerify(exactly = 0) { dao.softDeleteMany(any()) }
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `matched but DAO returns 0 = returns 0 and no sync`() = runTest {
            val e1 = entity(id = 10L, position = 5)
            coEvery { dao.getByWorkoutLiftId(444L) } returns listOf(e1)
            coEvery { dao.softDeleteMany(listOf(10L)) } returns 0

            val count = repo.deleteByPosition(444L, 5)

            assertEquals(0, count)
            coVerify(exactly = 0) { dao.syncPositions(any(), any()) }
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `matched and DAO returns count gt 0 = sync positions and schedule`() = runTest {
            val e1 = entity(id = 10L, position = 5)
            val e2 = entity(id = 11L, position = 5)
            coEvery { dao.getByWorkoutLiftId(555L) } returns listOf(e1, e2)
            coEvery { dao.softDeleteMany(listOf(10L, 11L)) } returns 2

            val count = repo.deleteByPosition(555L, 5)

            assertEquals(2, count)
            coVerify(exactly = 1) { dao.syncPositions(555L, 5) }
            io.mockk.verify(exactly = 1) { sync.scheduleSync() }
        }
    }

    @Nested
    inner class DeleteSingleAndManyById {

        @Test
        fun `delete by id - no rows affected = 0 and no sync`() = runTest {
            coEvery { dao.softDelete(999L) } returns 0

            val c = repo.deleteById(999L)

            assertEquals(0, c)
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `delete by id - rows affected gt 0 = schedule sync`() = runTest {
            coEvery { dao.softDelete(1L) } returns 1

            val c = repo.deleteById(1L)

            assertEquals(1, c)
            io.mockk.verify(exactly = 1) { sync.scheduleSync() }
        }

        @Test
        fun `delete(model) delegates to deleteWithoutRefetch`() = runTest {
            val m = TestSet(7L, 1L, 0, 8f, 5, 8)
            coEvery { dao.softDelete(7L) } returns 1

            val c = repo.delete(m)

            assertEquals(1, c)
            coVerify(exactly = 1) { dao.softDelete(7L) }
            io.mockk.verify(exactly = 1) { sync.scheduleSync() }
        }

        @Test
        fun `deleteMany empty list = returns 0 and no sync`() = runTest {
            val c = repo.deleteMany(emptyList())
            assertEquals(0, c)
            coVerify(exactly = 0) { dao.softDeleteMany(any()) }
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `deleteMany non-empty returns 0 = no sync`() = runTest {
            val models = listOf(TestSet(1L,1L,0,8f,5,8))
            coEvery { dao.softDeleteMany(listOf(1L)) } returns 0

            val c = repo.deleteMany(models)

            assertEquals(0, c)
            io.mockk.verify(exactly = 0) { sync.scheduleSync() }
        }

        @Test
        fun `deleteMany non-empty returns gt 0 = schedule sync`() = runTest {
            val models = listOf(TestSet(1L,1L,0,8f,5,8), TestSet(2L,1L,1,8f,5,8))
            coEvery { dao.softDeleteMany(listOf(1L, 2L)) } returns 2

            val c = repo.deleteMany(models)

            assertEquals(2, c)
            io.mockk.verify(exactly = 1) { sync.scheduleSync() }
        }
    }

    // ---------- Helpers ----------

    private fun entity(
        id: Long,
        workoutLiftId: Long = 42L,
        type: SetType = SetType.STANDARD,
        position: Int = 0,
        rpe: Float = 8f,
        bottom: Int = 5,
        top: Int = 8,
        setGoal: Int? = null,
        repFloor: Int? = null,
        dropPct: Float? = null,
        maxSets: Int? = null,
        setMatching: Boolean = false,
    ): CustomLiftSetEntity = CustomLiftSetEntity(
        id = id,
        workoutLiftId = workoutLiftId,
        type = type,
        position = position,
        rpeTarget = rpe,
        repRangeBottom = bottom,
        repRangeTop = top,
        setGoal = setGoal,
        repFloor = repFloor,
        dropPercentage = dropPct,
        maxSets = maxSets,
        setMatching = setMatching,
    )
}
