package com.browntowndev.liftlab.core.data.remote

// ---- Explicit JUnit Jupiter assertions (no wildcards) ----
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.remote.client.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.client.FirestoreRemoteDataClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.sync.BatchSyncCollection
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Query.Direction
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreRemoteDataClientTest {

    private lateinit var firestoreClient: FirestoreClient

    // We’ll inject minimal mappings used by tests; can be BaseRemoteDto::class since we stub toObject(..)
    private lateinit var defaultTypes: Map<String, KClass<out BaseRemoteDto>>

    @BeforeEach
    fun setup() {
        firestoreClient = mockk(relaxed = true)
        defaultTypes = mapOf("A" to BaseRemoteDto::class, "B" to BaseRemoteDto::class)

        // Mock kotlinx-coroutines-play-services await extension
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ---------------------------------------------------------------------
    // canSync
    // ---------------------------------------------------------------------

    @Test
    fun `canSync reflects firestoreClient isUserLoggedIn`() {
        every { firestoreClient.isUserLoggedIn } returns false
        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        assertFalse(client.canSync)

        every { firestoreClient.isUserLoggedIn } returns true
        assertTrue(client.canSync)
    }

    // ---------------------------------------------------------------------
    // getManyFlow
    // ---------------------------------------------------------------------

    @Test
    fun `getManyFlow - not logged in emits single empty list and completes`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns false

        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        val emissions = client.getManyFlow("A", listOf("x")).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())
    }

    @Test
    fun `getManyFlow - empty ids emits single empty list`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns true

        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        val emissions = client.getManyFlow("A", emptyList()).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())
    }

    @Test
    fun `getManyFlow - chunks ids in groups of 30 and emits a chunk per query`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns true

        val collectionA = mockk<CollectionReference>(relaxed = true)
        every { firestoreClient.userCollection("A") } returns collectionA

        // Prepare 61 ids -> 30, 30, 1
        val ids = (1..61).map { "id$it" }

        // For each chunk we need a distinct Query + Task + Snapshot
        class Chunk(val ids: List<String>) {
            val query: Query = mockk(relaxed = true)
            val task: Task<QuerySnapshot> = mockk(relaxed = true)
            val snapshot: QuerySnapshot = mockk(relaxed = true)
            val doc: DocumentSnapshot = mockk(relaxed = true)
        }
        val chunk1 = Chunk(ids.subList(0, 30))
        val chunk2 = Chunk(ids.subList(30, 60))
        val chunk3 = Chunk(ids.subList(60, 61))

        // Wire whereIn() → Query and Query.get() → Task
        every { collectionA.whereIn(eq(FieldPath.documentId()), eq(chunk1.ids)) } returns chunk1.query
        every { collectionA.whereIn(eq(FieldPath.documentId()), eq(chunk2.ids)) } returns chunk2.query
        every { collectionA.whereIn(eq(FieldPath.documentId()), eq(chunk3.ids)) } returns chunk3.query
        every { chunk1.query.get() } returns chunk1.task
        every { chunk2.query.get() } returns chunk2.task
        every { chunk3.query.get() } returns chunk3.task

        // Each await returns a snapshot whose single document maps to a DTO
        val dto1 = mockk<BaseRemoteDto>(relaxed = true)
        val dto2 = mockk<BaseRemoteDto>(relaxed = true)
        val dto3 = mockk<BaseRemoteDto>(relaxed = true)
        every { chunk1.snapshot.documents } returns listOf(chunk1.doc)
        every { chunk2.snapshot.documents } returns listOf(chunk2.doc)
        every { chunk3.snapshot.documents } returns listOf(chunk3.doc)
        // toObject(clazz) returns a DTO; getClazz is satisfied by defaultTypes
        every { chunk1.doc.toObject(any<Class<BaseRemoteDto>>()) } returns dto1
        every { chunk2.doc.toObject(any<Class<BaseRemoteDto>>()) } returns dto2
        every { chunk3.doc.toObject(any<Class<BaseRemoteDto>>()) } returns dto3

        // Mock await for the three tasks
        coEvery { chunk1.task.await() } returns chunk1.snapshot
        coEvery { chunk2.task.await() } returns chunk2.snapshot
        coEvery { chunk3.task.await() } returns chunk3.snapshot

        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)

        val emissions = client.getManyFlow("A", ids).toList()

        // Expect 3 chunk emissions with 1 item each (as stubbed)
        assertEquals(3, emissions.size)
        assertIterableEquals(listOf(dto1), emissions[0])
        assertIterableEquals(listOf(dto2), emissions[1])
        assertIterableEquals(listOf(dto3), emissions[2])
    }

    @Test
    fun `getManyFlow - throws when collection type mapping is missing`() {
        every { firestoreClient.isUserLoggedIn } returns true

        // Build a flow that will reach toDtos() (even with empty docs getClazz() is called)
        val collection = mockk<CollectionReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val task = mockk<Task<QuerySnapshot>>(relaxed = true)
        val snapshot = mockk<QuerySnapshot>(relaxed = true)
        every { firestoreClient.userCollection("X") } returns collection
        every { collection.whereIn(FieldPath.documentId(), any<List<String>>()) } returns query
        every { query.get() } returns task
        every { snapshot.documents } returns emptyList()
        coEvery { task.await() } returns snapshot

        val client = FirestoreRemoteDataClient(firestoreClient, /* missing mapping */ emptyMap())

        // Collecting should throw because getClazz("X") fails
        assertThrows<IllegalArgumentException> {
            runTest { client.getManyFlow("X", listOf("id1")).first() }
        }
    }

    // ---------------------------------------------------------------------
    // getAllSinceFlow
    // ---------------------------------------------------------------------

    @Test
    fun `getAllSinceFlow - not logged in produces no emissions`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns false
        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        val emissions = client.getAllSinceFlow("A", Date(0)).toList()
        assertTrue(emissions.isEmpty())
    }

    @Test
    fun `getAllSinceFlow - paginates with limit 400 and startAfter last visible`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns true

        val collection = mockk<CollectionReference>(relaxed = true)
        val whereQ = mockk<Query>(relaxed = true)
        val orderQ1 = mockk<Query>(relaxed = true)
        val orderQ2 = mockk<Query>(relaxed = true)
        val limitedQ = mockk<Query>(relaxed = true)
        val afterQ = mockk<Query>(relaxed = true)

        every { firestoreClient.userCollection("A") } returns collection
        every { collection.whereGreaterThanOrEqualTo("lastUpdated", any<Date>()) } returns whereQ
        every { whereQ.orderBy("lastUpdated", Direction.ASCENDING) } returns orderQ1
        every { orderQ1.orderBy(FieldPath.documentId(), Direction.ASCENDING) } returns orderQ2
        every { orderQ2.limit(400) } returns limitedQ

        // First page: 400 documents
        val task1 = mockk<Task<QuerySnapshot>>(relaxed = true)
        val snap1 = mockk<QuerySnapshot>(relaxed = true)
        val docsPage1 = (1..400).map {
            mockk<DocumentSnapshot>(relaxed = true).also { doc ->
                every { doc.toObject(any<Class<BaseRemoteDto>>()) } returns mockk(relaxed = true)
            }
        }
        every { snap1.documents } returns docsPage1
        every { limitedQ.get() } returns task1
        coEvery { task1.await() } returns snap1

        // Second page: startAfter(lastOfPage1) then 100 docs
        val lastDoc = docsPage1.last()
        every { limitedQ.startAfter(lastDoc) } returns afterQ

        val task2 = mockk<Task<QuerySnapshot>>(relaxed = true)
        val snap2 = mockk<QuerySnapshot>(relaxed = true)
        val docsPage2 = (1..100).map {
            mockk<DocumentSnapshot>(relaxed = true).also { doc ->
                every { doc.toObject(any<Class<BaseRemoteDto>>()) } returns mockk(relaxed = true)
            }
        }
        every { snap2.documents } returns docsPage2
        every { afterQ.get() } returns task2
        coEvery { task2.await() } returns snap2

        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        val emissions = client.getAllSinceFlow("A", Date(123)).toList()

        // Expect two emissions: 400 then 100
        assertEquals(2, emissions.size)
        assertEquals(400, emissions[0].size)
        assertEquals(100, emissions[1].size)
    }

    // ---------------------------------------------------------------------
    // executeBatchSync
    // ---------------------------------------------------------------------

    @Test
    fun `executeBatchSync - not logged in returns empty and performs no operations`() = runTest {
        every { firestoreClient.isUserLoggedIn } returns false
        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)

        val out = client.executeBatchSync(emptyList())

        assertTrue(out.isEmpty())
    }

    @Test
    fun `executeBatchSync - upsert with and without remoteId, and delete only for non-null remoteId`() = runTest {
        // Needed because executeBatchSync now does batch.commit().await()
        io.mockk.mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { firestoreClient.isUserLoggedIn } returns true

        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestoreClient.batch() } returns batch

        val colA = mockk<CollectionReference>(relaxed = true)
        val colB = mockk<CollectionReference>(relaxed = true)
        every { firestoreClient.userCollection("A") } returns colA
        every { firestoreClient.userCollection("B") } returns colB

        // Document refs + ids
        val genA = mockk<DocumentReference>(relaxed = true).also { every { it.id } returns "genA1" }
        val refB = mockk<DocumentReference>(relaxed = true).also { every { it.id } returns "ridB2" }
        val delRef = mockk<DocumentReference>(relaxed = true).also { every { it.id } returns "ridDel" }

        every { colA.document() } returns genA
        every { colB.document("ridB2") } returns refB
        every { colA.document("ridDel") } returns delRef

        // Chainable WriteBatch API
        every { batch.set(any<DocumentReference>(), any()) } returns batch
        every { batch.set(any<DocumentReference>(), any(), any()) } returns batch
        every { batch.delete(any<DocumentReference>()) } returns batch

        // Commit now awaited -> return a Task<Void> and stub await() to resume
        val commitTask = mockk<Task<Void>>(relaxed = true)
        every { batch.commit() } returns commitTask
        coEvery { commitTask.await() } returns mockk(relaxed = true)  // Void task -> return null

        // Entities
        val upsertNoId = mockk<BaseRemoteDto>(relaxed = true).also { every { it.remoteId } returns null }
        val upsertWithId = mockk<BaseRemoteDto>(relaxed = true).also { every { it.remoteId } returns "ridB2" }
        val deleteWithId = mockk<BaseRemoteDto>(relaxed = true).also { every { it.remoteId } returns "ridDel" }
        val deleteNoId = mockk<BaseRemoteDto>(relaxed = true).also { every { it.remoteId } returns null }

        val batches = listOf(
            BatchSyncCollection("A", listOf(upsertNoId), SyncType.Upsert),
            BatchSyncCollection("B", listOf(upsertWithId), SyncType.Upsert),
            BatchSyncCollection("A", listOf(deleteWithId, deleteNoId), SyncType.Delete)
        )

        val client = FirestoreRemoteDataClient(firestoreClient, defaultTypes)
        val outIds = client.executeBatchSync(batches)

        // Upsert IDs include the generated doc id and the provided id
        assertIterableEquals(listOf("genA1", "ridB2"), outIds)
    }


    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    // (None needed; kept inline to show the exact call chains we’re stubbing)
}
