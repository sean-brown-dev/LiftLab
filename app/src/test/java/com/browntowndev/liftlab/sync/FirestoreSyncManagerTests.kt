package com.browntowndev.liftlab.sync

import android.util.Log
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.domain.repositories.sync.CustomLiftSetsSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.sync.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.sync.ProgramsSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.sync.WorkoutsSyncRepository
import com.browntowndev.liftlab.core.data.remote.dto.CustomLiftSetRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.HistoricalWorkoutNameRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutRemoteDto
import com.browntowndev.liftlab.core.data.remote.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.data.remote.sync.SyncQueueEntry
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSyncManagerTest {
    private lateinit var firestoreClient: FirestoreClient
    private lateinit var customRepo: CustomLiftSetsSyncRepository
    private lateinit var historicalRepo: HistoricalWorkoutNamesSyncRepository
    private lateinit var workoutsSyncRepository: WorkoutsSyncRepository
    private lateinit var programsSyncRepository: ProgramsSyncRepository
    private lateinit var syncManager: FirestoreSyncManager
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @BeforeEach
    fun setUp() {
        // Stub Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        // Stub Crashlytics
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        // Mocks
        firestoreClient = mockk(relaxed = true)
        customRepo = mockk(relaxed = true)
        historicalRepo = mockk(relaxed = true)
        workoutsSyncRepository = mockk(relaxed = true)
        programsSyncRepository = mockk(relaxed = true)


        // Common stub
        every { firestoreClient.userId } returns "uid"

        // Collection names
        every { customRepo.collectionName } returns "custom"
        every { historicalRepo.collectionName } returns "historical"
        every { workoutsSyncRepository.collectionName } returns "workouts"
        every { programsSyncRepository.collectionName } returns "programs"


        // Instantiate manager
        syncManager = FirestoreSyncManager(
            firestoreClient = firestoreClient,
            syncScope = testScope,
            customLiftSetsSyncRepository = customRepo,
            historicalWorkoutNamesSyncRepository = historicalRepo,
            liftMetricChartsSyncRepository = mockk(relaxed = true),
            liftsSyncRepository = mockk(relaxed = true),
            previousSetResultsSyncRepository = mockk(relaxed = true),
            programsSyncRepository = programsSyncRepository,
            setLogEntriesSyncRepository = mockk(relaxed = true),
            volumeMetricChartsSyncRepository = mockk(relaxed = true),
            workoutInProgressSyncRepository = mockk(relaxed = true),
            workoutLiftsSyncRepository = mockk(relaxed = true),
            workoutLogEntriesSyncRepository = mockk(relaxed = true),
            workoutsSyncRepository = workoutsSyncRepository,
            syncRepository = mockk(relaxed = true)
        )
    }

    @Test
    fun `enqueueSyncRequest processes single upsert happy path`() = runTest {
        // Prepare a DTO
        val dto = CustomLiftSetRemoteDto(id = 42)

        // Mock the Repo
        coEvery { customRepo.getMany(listOf(42L)) } returns listOf(dto)
        coEvery { customRepo.upsertMany(any()) } returns emptyList()

        // Mock Firestore interaction
        val mockCollection = mockk<CollectionReference>()
        val mockDocRef = mockk<DocumentReference>()
        every { firestoreClient.userCollection("custom") } returns mockCollection

        // Mock DocumentReference methods
        every { mockCollection.document() } returns mockDocRef
        every { mockDocRef.id } returns "generatedId"
        val setTcs = TaskCompletionSource<Void>()
        setTcs.setResult(null)
        every { mockDocRef.set(any()) } returns setTcs.task

        // Mock DocumentSnapshot
        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockSnapshot.toObject<CustomLiftSetRemoteDto>() } returns dto
        val getTcs = TaskCompletionSource<DocumentSnapshot>()
        getTcs.setResult(mockSnapshot)
        every { mockDocRef.get() } returns getTcs.task

        // Enqueue and run
        val entry = SyncQueueEntry("custom", listOf(42L), SyncType.Upsert)
        syncManager.enqueueSyncRequest(entry)
        testScope.advanceUntilIdle()

        // Assert repository was called with our DTO
        coVerify { customRepo.upsertMany(listOf(dto)) }
    }

    @Test
    fun `enqueueBatchSyncRequest processes batch upsert happy path`() = runTest {
        val dtoA1 = CustomLiftSetRemoteDto(id = 1)
        val dtoA2 = CustomLiftSetRemoteDto(id = 2)
        val dtoB3 = HistoricalWorkoutNameRemoteDto(id = 3)
        val dtoB4 = HistoricalWorkoutNameRemoteDto(id = 4)

        coEvery { customRepo.getMany(listOf(1L, 2L)) } returns listOf(dtoA1, dtoA2)
        coEvery { historicalRepo.getMany(listOf(3L, 4L)) } returns listOf(dtoB3, dtoB4)

        val mockBatch = mockk<WriteBatch>(relaxed = true)
        every { firestoreClient.batch() } returns mockBatch

        every { mockBatch.set(any(), any()) } returns mockBatch
        every { mockBatch.delete(any()) } returns mockBatch
        val commitTcs = TaskCompletionSource<Void>()
        commitTcs.setResult(null)
        every { mockBatch.commit() } returns commitTcs.task

        val collA = mockk<CollectionReference>()
        val collB = mockk<CollectionReference>()
        val docA1 = mockk<DocumentReference>()
        every { docA1.id } returns "1"
        val docA2 = mockk<DocumentReference>()
        every { docA2.id } returns "2"
        val docB3 = mockk<DocumentReference>()
        every { docB3.id } returns "3"
        val docB4 = mockk<DocumentReference>()
        every { docB4.id } returns "4"
        every { firestoreClient.userCollection("custom") } returns collA
        every { firestoreClient.userCollection("historical") } returns collB
        every { collA.document() } returnsMany listOf(docA1, docA2)
        every { collB.document() } returnsMany listOf(docB3, docB4)

        val snapA1 = mockk<DocumentSnapshot>()
        val snapA2 = mockk<DocumentSnapshot>()
        val snapB3 = mockk<DocumentSnapshot>()
        val snapB4 = mockk<DocumentSnapshot>()
        val getTcsA1 = TaskCompletionSource<DocumentSnapshot>()
        getTcsA1.setResult(snapA1)
        every { docA1.get() } returns getTcsA1.task
        val getTcsA2 = TaskCompletionSource<DocumentSnapshot>()
        getTcsA2.setResult(snapA2)
        every { docA2.get() } returns getTcsA2.task
        val getTcsB3 = TaskCompletionSource<DocumentSnapshot>()
        getTcsB3.setResult(snapB3)
        every { docB3.get() } returns getTcsB3.task
        val getTcsB4 = TaskCompletionSource<DocumentSnapshot>()
        getTcsB4.setResult(snapB4)
        every { docB4.get() } returns getTcsB4.task
        every { snapA1.toObject<CustomLiftSetRemoteDto>() } returns dtoA1
        every { snapA2.toObject<CustomLiftSetRemoteDto>() } returns dtoA2
        every { snapB3.toObject<HistoricalWorkoutNameRemoteDto>() } returns dtoB3
        every { snapB4.toObject<HistoricalWorkoutNameRemoteDto>() } returns dtoB4

        coEvery { customRepo.upsertMany(any()) } returns emptyList()
        coEvery { historicalRepo.upsertMany(any()) } returns emptyList()

        val batch = BatchSyncQueueEntry(
            id = "batch1",
            batch = listOf(
                SyncQueueEntry("custom", listOf(1L, 2L), SyncType.Upsert),
                SyncQueueEntry("historical", listOf(3L, 4L), SyncType.Upsert)
            )
        )

        syncManager.enqueueBatchSyncRequest(batch)
        testScope.advanceUntilIdle()

        coVerify { customRepo.upsertMany(listOf(dtoA1, dtoA2)) }
        coVerify { historicalRepo.upsertMany(listOf(dtoB3, dtoB4)) }
    }

    @Test
    fun `enqueueSyncRequest waits for batch request processing the same collection`() = runTest {
        val batchTcs = TaskCompletionSource<Void>()

        val dtoA1 = CustomLiftSetRemoteDto(id = 1)
        coEvery { customRepo.getMany(listOf(1L)) } returns listOf(dtoA1)
        coEvery { customRepo.upsertMany(any()) } returns emptyList()

        val mockBatch = mockk<WriteBatch>(relaxed = true)
        every { firestoreClient.batch() } returns mockBatch
        every { mockBatch.commit() } returns batchTcs.task

        val collA = mockk<CollectionReference>()
        val docA1 = mockk<DocumentReference>(relaxed = true)
        val mockSingleDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestoreClient.userCollection("custom") } returns collA
        every { collA.document() } returnsMany listOf(docA1, mockSingleDocRef)
        val getTcsA1 = TaskCompletionSource<DocumentSnapshot>()
        val mockSnapshotA1 = mockk<DocumentSnapshot>()
        every { mockSnapshotA1.toObject<CustomLiftSetRemoteDto>() } returns dtoA1
        getTcsA1.setResult(mockSnapshotA1)
        every { docA1.get() } returns getTcsA1.task

        val batch = BatchSyncQueueEntry(
            id = "batch1",
            batch = listOf(SyncQueueEntry("custom", listOf(1L), SyncType.Upsert))
        )
        syncManager.enqueueBatchSyncRequest(batch)

        advanceUntilIdle()

        val singleDto = CustomLiftSetRemoteDto(id = 42)
        coEvery { customRepo.getMany(listOf(42L)) } returns listOf(singleDto)
        var singleRequestProcessed = false
        coEvery { customRepo.upsertMany(listOf(singleDto)) } answers {
            singleRequestProcessed = true
            emptyList()
        }

        val setTcs = TaskCompletionSource<Void>()
        setTcs.setResult(null)
        every { mockSingleDocRef.set(any()) } returns setTcs.task
        val getTcsSingle = TaskCompletionSource<DocumentSnapshot>()
        val mockSnapshotSingle = mockk<DocumentSnapshot>()
        every { mockSnapshotSingle.toObject<CustomLiftSetRemoteDto>() } returns singleDto
        getTcsSingle.setResult(mockSnapshotSingle)
        every { mockSingleDocRef.get() } returns getTcsSingle.task

        val entry = SyncQueueEntry("custom", listOf(42L), SyncType.Upsert)
        syncManager.enqueueSyncRequest(entry)

        advanceUntilIdle()
        assertFalse(singleRequestProcessed, "Single request should not be processed while batch is running")

        batchTcs.setResult(null)
        yield()
        advanceUntilIdle()

        assertTrue(singleRequestProcessed, "Single request should be processed after batch is finished")
    }

    @Test
    fun `enqueueBatchSyncRequest waits for single request processing the same collection`() = runTest {
        val singleTcs = TaskCompletionSource<DocumentSnapshot>()

        val singleDto = CustomLiftSetRemoteDto(id = 42)
        coEvery { customRepo.getMany(listOf(42L)) } returns listOf(singleDto)
        coEvery { customRepo.upsertMany(listOf(singleDto)) } returns emptyList()

        val mockCollection = mockk<CollectionReference>()
        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        val mockBatchDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestoreClient.userCollection("custom") } returns mockCollection
        every { mockCollection.document() } returnsMany listOf(mockDocRef, mockBatchDocRef)
        val setTcs = TaskCompletionSource<Void>()
        setTcs.setResult(null)
        every { mockDocRef.set(any()) } returns setTcs.task
        every { mockDocRef.get() } returns singleTcs.task

        val entry = SyncQueueEntry("custom", listOf(42L), SyncType.Upsert)
        syncManager.enqueueSyncRequest(entry)

        advanceUntilIdle()

        val batchDto = CustomLiftSetRemoteDto(id = 1)
        coEvery { customRepo.getMany(listOf(1L)) } returns listOf(batchDto)
        var batchRequestProcessed = false
        coEvery { customRepo.upsertMany(listOf(batchDto)) } answers {
            batchRequestProcessed = true
            emptyList()
        }

        val mockBatch = mockk<WriteBatch>(relaxed = true)
        every { firestoreClient.batch() } returns mockBatch
        val commitTcs = TaskCompletionSource<Void>()
        commitTcs.setResult(null)
        every { mockBatch.commit() } returns commitTcs.task
        val getTcsBatch = TaskCompletionSource<DocumentSnapshot>()
        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockSnapshot.toObject<CustomLiftSetRemoteDto>() } returns batchDto
        getTcsBatch.setResult(mockSnapshot)
        every { mockBatchDocRef.get() } returns getTcsBatch.task


        val batch = BatchSyncQueueEntry(
            id = "batch1",
            batch = listOf(SyncQueueEntry("custom", listOf(1L), SyncType.Upsert))
        )
        syncManager.enqueueBatchSyncRequest(batch)

        advanceUntilIdle()
        assertFalse(batchRequestProcessed, "Batch request should not be processed while single request is running")

        val mockSingleSnapshot = mockk<DocumentSnapshot>()
        every { mockSingleSnapshot.toObject<CustomLiftSetRemoteDto>() } returns singleDto
        singleTcs.setResult(mockSingleSnapshot)
        advanceUntilIdle()

        assertTrue(batchRequestProcessed, "Batch request should be processed after single request is finished")
    }

    @Test
    fun `deletion watcher deletes child when parent is deleted`() = runTest {
        val workout1 = WorkoutRemoteDto(id = 1, programId = 10).apply { remoteId = "fId1" }
        val workout2 = WorkoutRemoteDto(id = 2, programId = 10).apply { remoteId = "fId2" }
        val workoutsFlow = MutableStateFlow(listOf(workout1, workout2))
        every { workoutsSyncRepository.getAllFlow() } returns workoutsFlow
        coEvery { programsSyncRepository.getMany(any()) } returns emptyList()

        val mockBatch = mockk<WriteBatch>()
        every { mockBatch.delete(any()) } returns mockBatch
        val commitTcs = TaskCompletionSource<Void>()
        commitTcs.setResult(null)
        every { mockBatch.commit() } returns commitTcs.task
        every { firestoreClient.batch() } returns mockBatch

        val workoutsCollection = mockk<CollectionReference>()
        every { firestoreClient.userCollection("workouts") } returns workoutsCollection
        val docRef = mockk<DocumentReference>()
        every { workoutsCollection.document("fId1") } returns docRef

        syncManager.tryStartDeletionWatchers()
        advanceUntilIdle()

        workoutsFlow.value = listOf(workout2) // workout1 is "deleted"
        advanceUntilIdle()

        verify { mockBatch.delete(docRef) }
    }

    @Test
    fun `deletion watcher does not delete child when parent exists`() = runTest {
        val workout1 = WorkoutRemoteDto(id = 1, programId = 10).apply { remoteId = "fId1" }
        val workout2 = WorkoutRemoteDto(id = 2, programId = 10).apply { remoteId = "fId2" }
        val workoutsFlow = MutableStateFlow(listOf(workout1, workout2))
        every { workoutsSyncRepository.getAllFlow() } returns workoutsFlow

        val program = ProgramRemoteDto(id = 10)
        coEvery { programsSyncRepository.getMany(listOf(10L)) } returns listOf(program)
        val mockBatch = mockk<WriteBatch>(relaxed = true)
        every { firestoreClient.batch() } returns mockBatch

        syncManager.tryStartDeletionWatchers()
        advanceUntilIdle()

        workoutsFlow.value = listOf(workout2) // workout1 is "deleted"
        advanceUntilIdle()

        verify(exactly = 0) { mockBatch.delete(any()) }
    }
}
