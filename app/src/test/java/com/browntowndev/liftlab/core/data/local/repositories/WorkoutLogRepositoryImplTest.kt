@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.sync.SyncScheduler
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLogRepositoryImplTest {

    private lateinit var workoutLogEntryDao: WorkoutLogEntryDao
    private lateinit var setLogEntryDao: SetLogEntryDao
    private lateinit var syncScheduler: SyncScheduler

    private lateinit var repository: WorkoutLogRepositoryImpl

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        workoutLogEntryDao = mockk(relaxed = true)
        setLogEntryDao = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)

        repository = WorkoutLogRepositoryImpl(
            workoutLogEntryDao = workoutLogEntryDao,
            setLogEntryDao = setLogEntryDao,
            syncScheduler = syncScheduler,
        )

        // Static mocks by FILE NAME (MockK uses generated Kt class names)
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.WorkoutLogEntryMappingExtensionsKt")
        mockkStatic("com.browntowndev.liftlab.core.data.mapping.SetLogEntryMappingExtensionsKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun domainLog(
        id: Long,
        setCount: Int = 0,
        date: Date = Date(1_700_000_000_000L),
    ): WorkoutLogEntry {
        val sets = (1..setCount).map { idx ->
            SetLogEntry(
                id = id * 100 + idx,
                workoutLogEntryId = id,
                liftId = 10L + idx,
                workoutLiftDeloadWeek = null,
                liftName = "Lift$idx",
                liftMovementPattern = MovementPattern.LEG_PUSH,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setType = SetType.STANDARD,
                liftPosition = idx,
                setPosition = idx,
                myoRepSetPosition = null,
                repRangeTop = 10,
                repRangeBottom = 8,
                rpeTarget = 8.0f,
                weightRecommendation = null,
                weight = 100f + idx,
                reps = 8,
                rpe = 8.0f,
                persistedOneRepMax = 250,
                isPersonalRecord = false,
                setMatching = null,
                maxSets = null,
                repFloor = null,
                dropPercentage = null,
                isDeload = false
            )
        }
        return WorkoutLogEntry(
            id = id,
            historicalWorkoutNameId = 99L,
            programWorkoutCount = 12,
            programDeloadWeek = 0,
            programName = "Program",
            workoutName = "Workout",
            programId = 777,
            workoutId = 888,
            mesocycle = 1,
            microcycle = 2,
            microcyclePosition = 3,
            date = date,
            durationInMillis = 3_600_000L,
            setLogEntries = sets
        )
    }

    // ---------- getAll ----------

    @Test
    fun `getAll maps entities to domain via extension`() = runTest {
        val entityA = mockk<WorkoutLogEntryEntity>(relaxed = true)
        val entityB = mockk<WorkoutLogEntryEntity>(relaxed = true)
        coEvery { workoutLogEntryDao.getAll() } returns listOf(entityA, entityB)

        val domainA = domainLog(1)
        val domainB = domainLog(2)
        every { entityA.toDomainModel() } returns domainA
        every { entityB.toDomainModel() } returns domainB

        val result = repository.getAll()
        Assertions.assertEquals(listOf(domainA, domainB), result)
        coVerify(exactly = 1) { workoutLogEntryDao.getAll() }
    }

    // ---------- getAllFlow ----------

    @Test
    fun `getAllFlow maps flattened DTOs through extension and emits domain list`() = runTest {
        val fakeFlattened: List<Any> = listOf(Any(), Any())
        every { workoutLogEntryDao.getAllFlattenedFlow() } returns flowOf(fakeFlattened as List<Nothing>)

        val domain = listOf(domainLog(1, setCount = 2), domainLog(2, setCount = 1))
        // Stub mapping on the actual instance, not any<List<*>>()
        every { fakeFlattened.toDomainModel() } returns domain

        val emitted = repository.getAllFlow().first()
        Assertions.assertEquals(domain, emitted)
        verify(exactly = 1) { workoutLogEntryDao.getAllFlattenedFlow() }
    }

    // ---------- getById ----------

    @Test
    fun `getById returns mapped domain when found`() = runTest {
        val entity = mockk<WorkoutLogEntryEntity>(relaxed = true)
        coEvery { workoutLogEntryDao.get(42L) } returns entity

        val domain = domainLog(42)
        every { entity.toDomainModel() } returns domain

        val result = repository.getById(42L)
        Assertions.assertEquals(domain, result)
        coVerify(exactly = 1) { workoutLogEntryDao.get(42L) }
    }

    @Test
    fun `getById returns null when missing`() = runTest {
        coEvery { workoutLogEntryDao.get(123L) } returns null
        val result = repository.getById(123L)
        Assertions.assertNull(result)
        coVerify(exactly = 1) { workoutLogEntryDao.get(123L) }
    }

    // ---------- getMany ----------

    @Test
    fun `getMany maps each entity`() = runTest {
        val ids = listOf(1L, 2L, 3L)
        val e1 = mockk<WorkoutLogEntryEntity>(relaxed = true)
        val e2 = mockk<WorkoutLogEntryEntity>(relaxed = true)
        val e3 = mockk<WorkoutLogEntryEntity>(relaxed = true)

        coEvery { workoutLogEntryDao.getMany(ids) } returns listOf(e1, e2, e3)
        every { e1.toDomainModel() } returns domainLog(1)
        every { e2.toDomainModel() } returns domainLog(2)
        every { e3.toDomainModel() } returns domainLog(3)

        val result = repository.getMany(ids)
        Assertions.assertEquals(listOf(domainLog(1), domainLog(2), domainLog(3)), result)
        coVerify(exactly = 1) { workoutLogEntryDao.getMany(ids) }
    }

    // ---------- update ----------

    @Test
    fun `update - no existing entity no-op (no DAO writes, no sync)`() = runTest {
        val model = domainLog(5, setCount = 0)
        coEvery { workoutLogEntryDao.get(5L) } returns null

        repository.update(model)

        coVerify(exactly = 1) { workoutLogEntryDao.get(5L) }
        coVerify(exactly = 0) { workoutLogEntryDao.update(any()) }
        coVerify(exactly = 0) { setLogEntryDao.getForWorkoutLogEntry(any()) }
        verify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `update - deletes removed set logs, updates parent, schedules sync`() = runTest {
        // Current entity
        val currentEntity = mockk<WorkoutLogEntryEntity>(relaxed = true)
        every { currentEntity.id } returns 10L
        every { currentEntity.remoteId } returns "rid-10"
        every { currentEntity.remoteLastUpdated } returns Date(1000)
        coEvery { workoutLogEntryDao.get(10L) } returns currentEntity

        // Existing set logs
        val existingSetEntities = listOf(101L, 102L, 103L).map { id ->
            mockk<com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity>(relaxed = true).also {
                every { it.id } returns id
                every { it.remoteId } returns "rid-$id"
                every { it.remoteLastUpdated } returns Date(2000 + id)
            }
        }
        coEvery { setLogEntryDao.getForWorkoutLogEntry(10L) } returns existingSetEntities

        // Incoming with no sets -> deletes all
        val incoming = domainLog(id = 10L, setCount = 0)

        val mappedParentEntity = mockk<WorkoutLogEntryEntity>(relaxed = true)
        every { incoming.toEntity() } returns mappedParentEntity

        every {
            mappedParentEntity.applyRemoteStorageMetadata(
                remoteId = "rid-10",
                remoteLastUpdated = any(),
                synced = false
            )
        } returns mappedParentEntity

        coJustRun { workoutLogEntryDao.update(mappedParentEntity) }
        coEvery { setLogEntryDao.softDeleteMany(any()) } returns 3

        repository.update(incoming)

        coVerify(exactly = 1) { workoutLogEntryDao.update(mappedParentEntity) }

        val slotIds = slot<List<Long>>()
        coVerify(exactly = 1) { setLogEntryDao.softDeleteMany(capture(slotIds)) }
        Assertions.assertEquals(setOf(101L, 102L, 103L), slotIds.captured.toSet())

        coVerify(exactly = 0) { setLogEntryDao.upsertMany(any()) }

        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    // ---------- deleteById ----------

    @Test
    fun `deleteById - when row deleted, cascade soft-deletes set logs and schedules sync`() = runTest {
        coEvery { workoutLogEntryDao.softDelete(55L) } returns 1
        coEvery { setLogEntryDao.softDeleteByWorkoutLogEntryId(55L) } returns 3

        val result = repository.deleteById(55L)

        Assertions.assertEquals(1, result)
        coVerify(exactly = 1) { setLogEntryDao.softDeleteByWorkoutLogEntryId(55L) }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }

    @Test
    fun `deleteById - when nothing deleted, does not delete set logs and does not schedule sync`() = runTest {
        coEvery { workoutLogEntryDao.softDelete(56L) } returns 0

        val result = repository.deleteById(56L)

        Assertions.assertEquals(0, result)
        coVerify(exactly = 0) { setLogEntryDao.softDeleteByWorkoutLogEntryId(any()) }
        verify(exactly = 0) { syncScheduler.scheduleSync() }
    }

    // ---------- getFlow(workoutLogEntryId) ----------

    @Test
    fun `getFlow maps flattened list to first domain item and emits it`() = runTest {
        val flattenedFlow: Flow<List<Any>> = flowOf(listOf(Any()))
        every { workoutLogEntryDao.getFlattenedFlow(777L) } returns flattenedFlow as Flow<List<Nothing>>

        val mappedList = listOf(domainLog(777, setCount = 1))
        // Need the actual instance produced by the DAO in the map step.
        // The repo calls .toDomainModel() on the emitted list instance,
        // so we mock that specific list object.
        val daoListInstance = (flattenedFlow.first() as List<Nothing>)
        every { daoListInstance.toDomainModel() } returns mappedList

        val emitted = repository.getFlow(777L).first()
        Assertions.assertEquals(mappedList[0], emitted)
        verify(exactly = 1) { workoutLogEntryDao.getFlattenedFlow(777L) }
    }

    @Test
    fun `getFlow drops emission when mapping returns empty list`() = runTest {
        val flattenedFlow: Flow<List<Any>> = flowOf(listOf(Any()))
        every { workoutLogEntryDao.getFlattenedFlow(778L) } returns flattenedFlow as Flow<List<Nothing>>

        val daoListInstance = (flattenedFlow.first() as List<Nothing>)
        every { daoListInstance.toDomainModel() } returns emptyList()

        // Collect to trigger mapping; there will be no items.
        repository.getFlow(778L).collect { /* no-op */ }

        verify(exactly = 1) { workoutLogEntryDao.getFlattenedFlow(778L) }
        verify(exactly = 1) { daoListInstance.toDomainModel() }
    }

    // ---------- getWorkoutLogsForLiftFlow(liftId) ----------

    @Test
    fun `getWorkoutLogsForLiftFlow maps flattened to domain list`() = runTest {
        val fakeFlattened: List<Any> = listOf(Any(), Any())
        every { workoutLogEntryDao.getLogsByLiftIdFlow(999L) } returns flowOf(fakeFlattened as List<Nothing>)

        val domain = listOf(domainLog(1), domainLog(2))
        every { fakeFlattened.toDomainModel() } returns domain

        val emitted = repository.getWorkoutLogsForLiftFlow(999L).first()
        Assertions.assertEquals(domain, emitted)
        verify(exactly = 1) { workoutLogEntryDao.getLogsByLiftIdFlow(999L) }
    }

    // ---------- getMostRecentSetResultsForLiftIds ----------

    @Test
    fun `getMostRecentSetResultsForLiftIds flattens set logs from mapped most-recent logs`() = runTest {
        val liftIds = listOf(10L, 20L, 30L)

        val fakeFlattenedMostRecent: List<Any> = listOf(Any(), Any(), Any())
        coEvery { workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds, includeDeloads = false) } returns fakeFlattenedMostRecent as List<Nothing>

        val domainLogs = listOf(
            domainLog(1, setCount = 2),
            domainLog(2, setCount = 1)
        )
        every { fakeFlattenedMostRecent.toDomainModel() } returns domainLogs

        val result = repository.getMostRecentSetResultsForLiftIds(liftIds, includeDeloads = false)

        Assertions.assertEquals(domainLogs.flatMap { it.setLogEntries }, result)
        coVerify(exactly = 1) { workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds, false) }
    }

    // ---------- getMostRecentSetResultsForLiftIdsPriorToDate ----------

    @Test
    fun `getMostRecentSetResultsForLiftIdsPriorToDate flattens set logs from mapped logs`() = runTest {
        val liftIds = listOf(1L, 2L)
        val date = Date(1_699_999_999_000L)

        val fakeFlattenedPrior: List<Any> = listOf(Any())
        coEvery { workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date) } returns fakeFlattenedPrior as List<Nothing>

        val domainLogs = listOf(domainLog(7, setCount = 3))
        every { fakeFlattenedPrior.toDomainModel() } returns domainLogs

        val result = repository.getMostRecentSetResultsForLiftIdsPriorToDate(
            liftIds = liftIds,
            linearProgressionLiftIds = emptySet(),
            date = date
        )
        Assertions.assertEquals(domainLogs.flatMap { it.setLogEntries }, result)
        coVerify(exactly = 1) { workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date) }
    }

    // ---------- insertWorkoutLogEntry ----------

    @Test
    fun `insertWorkoutLogEntry builds entity, inserts, and schedules sync`() = runTest {
        coEvery { workoutLogEntryDao.insert(any()) } returns 4321L

        val returnedId = repository.insertWorkoutLogEntry(
            historicalWorkoutNameId = 3L,
            programDeloadWeek = 1,
            programWorkoutCount = 12,
            mesoCycle = 2,
            microCycle = 4,
            microcyclePosition = 2,
            date = Date(1_700_100_000_000L),
            durationInMillis = 65_000L
        )

        Assertions.assertEquals(4321L, returnedId)
        coVerify(exactly = 1) { workoutLogEntryDao.insert(any()) }
        verify(exactly = 1) { syncScheduler.scheduleSync() }
    }
}
