package com.browntowndev.liftlab.sync

import android.util.Log
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.dtos.firestore.CustomLiftSetFirestoreDto
import com.browntowndev.liftlab.core.persistence.dtos.firestore.HistoricalWorkoutNameFirestoreDto
import com.browntowndev.liftlab.core.persistence.repositories.firestore.CustomLiftSetsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firestore.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.persistence.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.sync.FirestoreClient
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSyncManagerTest {
    private lateinit var firestoreClient: FirestoreClient
    private lateinit var customRepo: CustomLiftSetsSyncRepository
    private lateinit var historicalRepo: HistoricalWorkoutNamesSyncRepository
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

        // Common stub
        every { firestoreClient.userId } returns "uid"

        // Collection names
        every { customRepo.collectionName } returns "custom"
        every { historicalRepo.collectionName } returns "historical"

        // Instantiate manager
        syncManager = FirestoreSyncManager(
            firestoreClient = firestoreClient,
            syncScope = testScope,
            customLiftSetsSyncRepository = customRepo,
            historicalWorkoutNamesSyncRepository = historicalRepo,
            liftMetricChartsSyncRepository = mockk(relaxed = true),
            liftsSyncRepository = mockk(relaxed = true),
            previousSetResultsSyncRepository = mockk(relaxed = true),
            programsSyncRepository = mockk(relaxed = true),
            setLogEntriesSyncRepository = mockk(relaxed = true),
            volumeMetricChartsSyncRepository = mockk(relaxed = true),
            workoutInProgressSyncRepository = mockk(relaxed = true),
            workoutLiftsSyncRepository = mockk(relaxed = true),
            workoutLogEntriesSyncRepository = mockk(relaxed = true),
            workoutsSyncRepository = mockk(relaxed = true),
            syncRepository = mockk(relaxed = true)
        )
    }

    @Test
    fun `enqueueSyncRequest processes single upsert happy path`() = runTest {
        // Prepare a DTO
        val dto = CustomLiftSetFirestoreDto(id = 42)

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
        every { mockSnapshot.toObject<CustomLiftSetFirestoreDto>() } returns dto
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
        val dtoA1 = CustomLiftSetFirestoreDto(id = 1)
        val dtoA2 = CustomLiftSetFirestoreDto(id = 2)
        val dtoB3 = HistoricalWorkoutNameFirestoreDto(id = 3)
        val dtoB4 = HistoricalWorkoutNameFirestoreDto(id = 4)

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
        every { snapA1.toObject<CustomLiftSetFirestoreDto>() } returns dtoA1
        every { snapA2.toObject<CustomLiftSetFirestoreDto>() } returns dtoA2
        every { snapB3.toObject<HistoricalWorkoutNameFirestoreDto>() } returns dtoB3
        every { snapB4.toObject<HistoricalWorkoutNameFirestoreDto>() } returns dtoB4

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
}
