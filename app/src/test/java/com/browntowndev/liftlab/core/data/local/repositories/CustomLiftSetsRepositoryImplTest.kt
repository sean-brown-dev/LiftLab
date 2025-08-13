@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.browntowndev.liftlab.core.data.local.repositories

// JUnit Jupiter assertion imports (explicit, no wildcard)
import app.cash.turbine.test
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
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

    private lateinit var repo: CustomLiftSetsRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repo = CustomLiftSetsRepositoryImpl(dao)

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
