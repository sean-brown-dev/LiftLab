package com.browntowndev.liftlab.core.data.remote

import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.SyncMetadataDto
import com.browntowndev.liftlab.core.data.remote.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SyncOrchestratorTest {

    private lateinit var syncMetadataRepository: SyncMetadataRepository
    private lateinit var remoteDataClient: RemoteDataClient
    private lateinit var transactionScope: TransactionScope

    private lateinit var repoA: RemoteSyncRepository
    private lateinit var repoB: RemoteSyncRepository
    private lateinit var orchestrator: SyncOrchestrator

    private val hierarchy: List<HashSet<String>> =
        listOf(hashSetOf("A", "B")) // upserts in this order; deletes in reverse

    @BeforeEach
    fun setUp() {
        syncMetadataRepository = mockk(relaxed = true)
        remoteDataClient = mockk(relaxed = true)

        // Preferred TransactionScope pattern
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        repoA = mockk(relaxed = true) {
            every { collectionName } returns "A"
        }
        repoB = mockk(relaxed = true) {
            every { collectionName } returns "B"
        }

        orchestrator = SyncOrchestrator(
            syncMetadataRepository = syncMetadataRepository,
            syncRepositories = listOf(repoA, repoB),
            remoteDataClient = remoteDataClient,
            transactionScope = transactionScope,
            syncHierarchy = hierarchy
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // -------------------- syncToRemote --------------------

    @Test
    fun `syncToRemote - returns early when canSync is false`() = runTest {
        every { remoteDataClient.canSync } returns false

        orchestrator.syncToRemote()

        // No transaction
        coVerify(exactly = 0) { transactionScope.execute(any()) }
        // No remote operations
        coVerify(exactly = 0) { remoteDataClient.executeBatchSync(any()) }
        coVerify(exactly = 0) {
            @Suppress("UnusedFlow")
            remoteDataClient.getManyFlow(any(), any())
        }
        coVerify(exactly = 0) {
            @Suppress("UnusedFlow")
            remoteDataClient.getAllSinceFlow(any(), any())
        }
    }

    @Test
    fun `syncToRemote - upserts non-deleted, fetches updated DTOs per collection, then deletes in reverse order`() =
        runTest {
            every { remoteDataClient.canSync } returns true
            every { repoA.collectionName } returns "A"
            every { repoB.collectionName } returns "B"

            // --- Upsert phase inputs (first getAllUnsynced call per repo) ---
            val toUpsertA1 = remoteDto(remoteId = "rA1", lastUpdated = Date(1), deleted = false)
            val toUpsertA2 = remoteDto(remoteId = "rA2", lastUpdated = Date(2), deleted = false)
            val toUpsertB1 = remoteDto(remoteId = "rB1", lastUpdated = Date(3), deleted = false)
            val ignoredDeletedA = remoteDto(
                remoteId = "rAd",
                lastUpdated = Date(4),
                deleted = true
            ) // not included in upsert batch

            // --- Delete phase inputs (second getAllUnsynced call per repo) ---
            val deletedWithRemoteIdA =
                remoteDto(remoteId = "rid-A", lastUpdated = Date(6), deleted = true)
            val deletedWithoutRemoteIdB =
                remoteDto(remoteId = null, lastUpdated = Date(5), deleted = true)

            // Per-repo: first call = upsert pass; second call = delete pass
            coEvery { repoA.getAllUnsynced() } returnsMany listOf(
                listOf(toUpsertA1, toUpsertA2, ignoredDeletedA),
                listOf(deletedWithRemoteIdA)
            )
            coEvery { repoB.getAllUnsynced() } returnsMany listOf(
                listOf(toUpsertB1),
                listOf(deletedWithoutRemoteIdB)
            )

            // executeBatchSync: return the union of IDs for any Upsert batches present in this call (order-agnostic, parallel-safe)
            coEvery { remoteDataClient.executeBatchSync(any()) } answers {
                val batches = firstArg<List<BatchSyncCollection>>()
                val hasAUpserts =
                    batches.any { it.collectionName == "A" && it.syncType == SyncType.Upsert }
                val hasBUpserts =
                    batches.any { it.collectionName == "B" && it.syncType == SyncType.Upsert }
                buildList {
                    if (hasAUpserts) addAll(listOf("rA1", "rA2"))
                    if (hasBUpserts) add("rB1")
                }
            }

            // getManyFlow per collection (filter to whatever ids the orchestrator sends)
            coEvery { remoteDataClient.getManyFlow(eq("A"), any()) } answers {
                val ids = secondArg<List<String>>()
                flowOf(listOf(toUpsertA1, toUpsertA2).filter { it.remoteId in ids })
            }
            coEvery { remoteDataClient.getManyFlow(eq("B"), any()) } answers {
                val ids = secondArg<List<String>>()
                flowOf(listOf(toUpsertB1).filter { it.remoteId in ids })
            }

            // Local upserts of fetched items
            coEvery { repoA.upsertMany(listOf(toUpsertA1, toUpsertA2)) } returns listOf(1L, 2L)
            coEvery { repoB.upsertMany(listOf(toUpsertB1)) } returns listOf(3L)

            // Deleted-without-remoteId => mark "synced" via local upsert of a copied DTO
            coEvery { repoB.upsertMany(match { it.size == 1 }) } returns listOf(10L)

            // Deleted-with-remoteId => remote delete then local delete
            coEvery { repoA.deleteManyByRemoteId(listOf("rid-A")) } returns 1

            // Act
            orchestrator.syncToRemote()

            // Assert: wrapped in a transaction
            coVerify(exactly = 1) { transactionScope.execute(any()) }

            // Upsert fetch-and-apply per collection (order-agnostic)
            coVerify(exactly = 1) {
                remoteDataClient.getManyFlow(
                    eq("A"),
                    match { it.containsAll(listOf("rA1", "rA2")) })
            }
            coVerify(exactly = 1) {
                remoteDataClient.getManyFlow(
                    eq("B"),
                    match { it.size == 1 && it.contains("rB1") })
            }
            coVerify(exactly = 1) { repoA.upsertMany(listOf(toUpsertA1, toUpsertA2)) }
            coVerify(exactly = 1) { repoB.upsertMany(listOf(toUpsertB1)) }

            // Deleted without remoteId -> upserted as "synced"
            val deletedWithoutRemoteIdMarkedSynced =
                listOf(deletedWithoutRemoteIdB.copyWithBase().apply { synced = true })
            coVerify(exactly = 1) { repoB.upsertMany(deletedWithoutRemoteIdMarkedSynced) }

            // Deleted with remoteId -> remote delete occurred and then local delete by remoteId
            coVerify(atLeast = 1) {
                remoteDataClient.executeBatchSync(match { it.any { b -> b.syncType == SyncType.Delete && b.collectionName == "A" } })
            }
            coVerify(exactly = 1) { repoA.deleteManyByRemoteId(listOf("rid-A")) }
        }


    @Test
    fun `syncToRemote - empty unsynced across all repos results in no remote calls`() = runTest {
        every { remoteDataClient.canSync } returns true

        coEvery { repoA.getAllUnsynced() } returns emptyList()
        coEvery { repoB.getAllUnsynced() } returns emptyList()

        orchestrator.syncToRemote()

        coVerify(exactly = 1) { transactionScope.execute(any()) }
        coVerify(exactly = 0) { remoteDataClient.executeBatchSync(any()) }
        coVerify(exactly = 0) {
            @Suppress("UnusedFlow")
            remoteDataClient.getManyFlow(any(), any())
        }
    }

    @Test
    fun `syncToRemote - on exception records to Crashlytics and rethrows`() = runTest {
        every { remoteDataClient.canSync } returns true

        // Cause failure inside transaction (first repo call blows up)
        coEvery { repoA.getAllUnsynced() } throws RuntimeException("boom")

        // Crashlytics
        mockkStatic(FirebaseCrashlytics::class)
        val crash = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crash

        val ex = assertThrows<RuntimeException> {
            orchestrator.syncToRemote()
        }
        assertTrue(ex.message?.contains("boom") == true)
        io.mockk.verify(exactly = 1) { crash.recordException(any()) }
    }

    // -------------------- syncFromThenToRemote --------------------

    @Test
    fun `syncFromThenToRemote - retries canSync up to 3 times then proceeds`() = runTest {
        // false, false, true -> proceed
        every { remoteDataClient.canSync } returnsMany listOf(false, false, true)

        // Minimal plumbing for download+upload
        // download: no metadata for A,B -> start at epoch
        coEvery { syncMetadataRepository.get(any()) } returns null
        // getAllSince emits two chunks for A and one for B
        val newerA1 = remoteDto("a1", Date(10), deleted = false)
        val newerA2 = remoteDto("a2", Date(20), deleted = false)
        val flowA = listOf(listOf(newerA1), listOf(newerA2)).asFlow()
        val flowB = flowOf(emptyList<BaseRemoteDto>())

        every { repoA.collectionName } returns "A"
        every { repoB.collectionName } returns "B"
        every { remoteDataClient.getAllSinceFlow("A", any()) } returns flowA
        every { remoteDataClient.getAllSinceFlow("B", any()) } returns flowB

        // Locals: nothing existing, so both remotes are strictly newer and should be upserted
        coEvery { repoA.getManyByRemoteId(listOf("a1")) } returns emptyList()
        coEvery { repoA.getManyByRemoteId(listOf("a2")) } returns emptyList()
        coEvery { repoA.upsertMany(listOf(newerA1)) } returns listOf(1L)
        coEvery { repoA.upsertMany(listOf(newerA2)) } returns listOf(2L)

        // upload: nothing to do
        coEvery { repoA.getAllUnsynced() } returns emptyList()
        coEvery { repoB.getAllUnsynced() } returns emptyList()

        // metadata upsert capture
        val metas = mutableListOf<SyncMetadataDto>()
        coEvery { syncMetadataRepository.upsert(capture(metas)) } just Runs

        orchestrator.syncFromThenToRemote(syncAllFromRemote = false)

        // Both download chunks applied
        coVerify { repoA.upsertMany(listOf(newerA1)) }
        coVerify { repoA.upsertMany(listOf(newerA2)) }

        // Metadata updated to the LATEST date seen (20 for A; epoch for B)
        val metaA = metas[0]
        val metaB = metas[1]
        assertEquals("A", metaA.collectionName)
        assertEquals(Date(20), metaA.lastSyncTimestamp)
        assertEquals("B", metaB.collectionName)
        assertEquals(Date(0), metaB.lastSyncTimestamp)
    }

    @Test
    fun `syncFromThenToRemote - returns early when canSync remains false after retries`() =
        runTest {
            every { remoteDataClient.canSync } returns false

            orchestrator.syncFromThenToRemote(syncAllFromRemote = false)

            coVerify(exactly = 0) { transactionScope.execute(any()) }
            coVerify(exactly = 0) {
                @Suppress("UnusedFlow")
                remoteDataClient.getAllSinceFlow(any(), any())
            }
            coVerify(exactly = 0) { remoteDataClient.executeBatchSync(any()) }
        }

    @Test
    fun `downloadNewerChanges - respects syncAll=true (starts at epoch, does not read metadata)`() =
        runTest {
            every { remoteDataClient.canSync } returns true

            // For syncAll=true, metadata.get(...) should not be consulted
            // and lastUpdated passed to getAllSince is Date(0).
            val now1 = Date(1000)
            val dto1 = remoteDto("x1", now1, deleted = false)

            every { repoA.collectionName } returns "A"
            every { repoB.collectionName } returns "B"

            // Emit a single non-empty chunk for A; empty for B
            every { remoteDataClient.getAllSinceFlow("A", Date(0)) } returns flowOf(listOf(dto1))
            every { remoteDataClient.getAllSinceFlow("B", Date(0)) } returns flowOf(emptyList())

            // No matching locals -> upsert
            coEvery { repoA.getManyByRemoteId(listOf("x1")) } returns emptyList()
            coEvery { repoA.upsertMany(listOf(dto1)) } returns listOf(1L)

            // Upload phase: empty
            coEvery { repoA.getAllUnsynced() } returns emptyList()
            coEvery { repoB.getAllUnsynced() } returns emptyList()

            val metas = mutableListOf<SyncMetadataDto>()
            coEvery { syncMetadataRepository.upsert(capture(metas)) } returns Unit

            orchestrator.syncFromThenToRemote(syncAllFromRemote = true)

            // Confirm metadata.get was never called (syncAll bypass)
            coVerify(exactly = 0) { syncMetadataRepository.get(any()) }

            // Metadata updated to now1 (A) and epoch (B)
            assertEquals(Date(1000), metas[0].lastSyncTimestamp)
            assertEquals(Date(0), metas[1].lastSyncTimestamp)
        }

    @Test
    fun `downloadNewerChanges - only upserts remote newer than local`() = runTest {
        every { remoteDataClient.canSync } returns true

        every { repoA.collectionName } returns "A"
        // Metadata indicates last sync at epoch -> all since epoch
        coEvery { syncMetadataRepository.get(any()) } returns SyncMetadataDto("A", Date(0))

        val remoteNewer = remoteDto("id-new", Date(100), deleted = false)
        val remoteOlder = remoteDto("id-old", Date(50), deleted = false)

        // The flow emits both; the local says 'id-old' is already up-to-date (lastUpdated = 100), so skip it.
        every { remoteDataClient.getAllSinceFlow("A", Date(0)) } returns listOf(
            listOf(remoteNewer, remoteOlder)
        ).asFlow()

        // Locals: existing older id with newer timestamp, so not to upsert
        val localOldNewer = remoteDto("id-old", Date(200), deleted = false)
        coEvery {
            repoA.getManyByRemoteId(
                listOf(
                    "id-new",
                    "id-old"
                )
            )
        } returns listOf(localOldNewer)

        // Only 'remoteNewer' should be upserted locally
        coEvery { repoA.upsertMany(listOf(remoteNewer)) } returns listOf(1L)

        // Upload phase: nothing
        coEvery { repoA.getAllUnsynced() } returns emptyList()
        coEvery { repoB.getAllUnsynced() } returns emptyList()

        // Metadata capture
        val metas = mutableListOf<SyncMetadataDto>()
        coEvery { syncMetadataRepository.upsert(capture(metas)) } returns Unit

        orchestrator.syncFromThenToRemote(syncAllFromRemote = false)

        coVerify(exactly = 1) { repoA.upsertMany(listOf(remoteNewer)) }
        // lastSyncTimestamp becomes max(remote chunk), i.e., 100
        assertEquals(Date(100).time, metas[0].lastSyncTimestamp.time)
        assertEquals(Date(0).time, metas[1].lastSyncTimestamp.time)
    }

    // -------------------- helpers --------------------

    private fun remoteDto(
        remoteId: String?,
        lastUpdated: Date?,
        deleted: Boolean
    ): BaseRemoteDto = mockk(relaxed = true) {
        every { this@mockk.remoteId } returns remoteId
        every { this@mockk.lastUpdated } returns lastUpdated
        every { this@mockk.deleted } returns deleted
        // For the deletion path that marks "synced = true" after copy
        every { this@mockk.copyWithBase() } returns mockk(relaxed = true)
    }
}
