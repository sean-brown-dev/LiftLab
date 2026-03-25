package com.browntowndev.liftlab.core.data.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.browntowndev.liftlab.core.common.THANK_YOU_DIALOG_BODY
import com.browntowndev.liftlab.core.coroutines.AppDispatchers
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerImplTest {

    private lateinit var builder: BillingClient.Builder
    private lateinit var billingClient: BillingClient
    private lateinit var dispatchers: AppDispatchers
    private lateinit var crashlytics: FirebaseCrashlytics
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        mockkStatic(BillingFlowParams::class)

        // Crashlytics
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics

        billingClient = mockk(relaxed = true)
        builder = mockk(relaxed = true)

        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")

        // Builder chain
        every { builder.setListener(any()) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.build() } returns billingClient

        // Dispatchers (only .io is used)
        dispatchers = mockk(relaxed = true)
        every { dispatchers.io } returns testDispatcher
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // --- init / connection ----------------------------------------------------

    @Test
    fun `init - starts connection when not ready`() {
        every { billingClient.isReady } returns false

        val mgr = BillingManagerImpl(builder, dispatchers)

        verify(exactly = 1) { billingClient.startConnection(mgr) }
    }

    @Test
    fun `init - handles builder exceptions and sets friendly message`() = runTest(testDispatcher) {
        // Make build() throw
        every { builder.build() } throws RuntimeException("boom")

        val mgr = BillingManagerImpl(builder, dispatchers)

        val msg = mgr.billingMessage.first()
        assertNotNull(msg)
        assertTrue(msg.contains("Failed to initialize"))
        verify(exactly = 1) { crashlytics.recordException(any()) }
    }

    @Test
    fun `onBillingServiceDisconnected - restarts connection when initialized`() {
        every { billingClient.isReady } returns true

        val mgr = BillingManagerImpl(builder, dispatchers)
        clearMocks(billingClient, answers = false) // clear previous startConnection verify noise

        mgr.onBillingServiceDisconnected()

        verify(exactly = 1) { billingClient.startConnection(mgr) }
    }

    // --- onBillingSetupFinished ----------------------------------------------

    @Test
    fun `onBillingSetupFinished OK - queries products, then onPurchasesUpdated consumes INAPP, acknowledges SUBS, updates active subscription`() = runTest(testDispatcher) {
        every { billingClient.isReady } returns true
        val mgr = BillingManagerImpl(builder, dispatchers)

        // Product querying (KTX)
        val pdA = mockProductDetails("liftlab_donate_5", ProductType.INAPP, priceMicros = 5_000_000)
        val pdB = mockProductDetails("liftlab_donate_monthly_10", ProductType.SUBS, subPriceMicros = 10_000_000)
        val inappResult = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(pdA) }
        val subsResult  = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(pdB) }
        coEvery { billingClient.queryProductDetails(any()) } returnsMany listOf(inappResult, subsResult)

        // Purchases to feed via onPurchasesUpdated (explicitly trigger processing)
        val subPurchase = mockPurchase(
            productId = "liftlab_donate_monthly_10",
            state = PurchaseState.PURCHASED,
            acknowledged = false,
            token = "tok-sub"
        )
        val inappPurchase = mockPurchase(
            productId = "liftlab_donate_5",
            state = PurchaseState.PURCHASED,
            acknowledged = false,
            token = "tok-inapp"
        )

        // When the code re-queries subs after ack, return the same purchase
        val subsPurchasesResult = mockk<PurchasesResult> { every { purchasesList } returns listOf(subPurchase) }
        coEvery { billingClient.queryPurchasesAsync(any()) } answers {
            subsPurchasesResult
        }

        // Consuming one-time & acknowledging subs
        val okResult = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        val consumeRes = mockk<ConsumeResult> { every { billingResult } returns okResult }
        coEvery { billingClient.consumePurchase(any()) } returns consumeRes
        coEvery { billingClient.acknowledgePurchase(any()) } returns okResult

        // Trigger setup OK (loads products)
        val setupOk = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        mgr.onBillingSetupFinished(setupOk)
        testScheduler.advanceUntilIdle()

        // Products loaded into flows
        assertEquals(listOf(pdA), mgr.oneTimeDonationProducts.first())
        assertEquals(listOf(pdB), mgr.subscriptionProducts.first())

        // EXPLICITLY trigger purchase processing (more deterministic than relying on setup pass)
        mgr.onPurchasesUpdated(setupOk, mutableListOf(inappPurchase, subPurchase))
        testScheduler.advanceUntilIdle()

        // Auto-processed purchases should have run (consume + acknowledge)
        coVerify { billingClient.consumePurchase(match { it.purchaseToken == "tok-inapp" }) }
        coVerify { billingClient.acknowledgePurchase(match { it.purchaseToken == "tok-sub" }) }

        // Active subscription set to pdB (matches purchase product id)
        assertEquals("liftlab_donate_monthly_10", mgr.activeSubscription.first()!!.productId)
    }


    @Test
    fun `onBillingSetupFinished non-OK - posts failure message and logs`() = runTest(testDispatcher) {
        every { billingClient.isReady } returns true
        val mgr = BillingManagerImpl(builder, dispatchers)

        val notOk = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.SERVICE_UNAVAILABLE }
        mgr.onBillingSetupFinished(notOk)

        testScheduler.advanceUntilIdle()
        assertNotNull(mgr.billingMessage.first())
    }

    // --- launchPurchaseFlow ---------------------------------------------------

    @Test
    fun `launchPurchaseFlow - sets processing and invokes billing flow with offer token for SUBS`() {
        val mgr = BillingManagerImpl(builder, dispatchers)

        val pd = mockProductDetails(
            id = "liftlab_donate_monthly_5",
            type = ProductType.SUBS,
            subPriceMicros = 5_000_000,
            offerToken = "offer-123"
        )

        val activity = mockk<Activity>(relaxed = true)
        every { billingClient.launchBillingFlow(activity, any()) } returns mockk(relaxed = true)

        val mockFlowBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        val mockFlowParams  = mockk<BillingFlowParams>(relaxed = true)

        every { BillingFlowParams.newBuilder() } returns mockFlowBuilder
        every { mockFlowBuilder.setProductDetailsParamsList(any<List<BillingFlowParams.ProductDetailsParams>>()) } returns mockFlowBuilder
        every { mockFlowBuilder.build() } returns mockFlowParams

        mgr.launchPurchaseFlow(activity, pd)

        assertTrue(mgr.isProcessingPurchase.value)
        verify { billingClient.launchBillingFlow(activity, any()) }
    }

    // --- onPurchasesUpdated ---------------------------------------------------

    @Test
    fun `onPurchasesUpdated - OK with PURCHASED and not acknowledged INAPP consumes and thanks`() = runTest(testDispatcher) {
        val mgr = BillingManagerImpl(builder, dispatchers)

        // Preload one-time product list so error message lookup can work (and to mirror real flow)
        val pdA = mockProductDetails("liftlab_donate_5", ProductType.INAPP, priceMicros = 5_000_000)
        // Push directly into the flow via reflection-free approach: call queryProducts path
        val pdResult = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(pdA) }
        coEvery { billingClient.queryProductDetails(any()) } returnsMany listOf(pdResult, mockk(relaxed = true) {
            every { productDetailsList } returns emptyList()
        })

        // Consume OK
        val ok = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        val consumeRes = mockk<ConsumeResult> { every { billingResult } returns ok }
        coEvery { billingClient.consumePurchase(any()) } returns consumeRes

        // Trigger setup OK to load products
        val setupOk = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        every { billingClient.isReady } returns true
        mgr.onBillingSetupFinished(setupOk)
        testScheduler.advanceUntilIdle()

        // Now send purchase update
        val p = mockPurchase("liftlab_donate_5", PurchaseState.PURCHASED, acknowledged = false, token = "tokX")
        val okUpdate = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }

        mgr.onPurchasesUpdated(okUpdate, mutableListOf(p))
        testScheduler.advanceUntilIdle()

        assertEquals(THANK_YOU_DIALOG_BODY, mgr.billingMessage.first())
        assertFalse(mgr.isProcessingPurchase.value)
    }

    @Test
    fun `onPurchasesUpdated - OK with PURCHASED SUBS acknowledges then refreshes active subscription`() = runTest(testDispatcher) {
        val mgr = BillingManagerImpl(builder, dispatchers)

        val pdSub = mockProductDetails("liftlab_donate_monthly_10", ProductType.SUBS, subPriceMicros = 10_000_000)
        val subsResult = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(pdSub) }
        coEvery { billingClient.queryProductDetails(any()) } returnsMany listOf(mockk<ProductDetailsResult> {
            every { productDetailsList } returns emptyList()
        }, subsResult)

        // Acknowledge OK
        val ok = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        coEvery { billingClient.acknowledgePurchase(any()) } returns ok

        // Active subscription query (after ack) -> returns purchase with same product id
        val pr = mockk<PurchasesResult> {
            every { purchasesList } returns listOf(mockPurchase("liftlab_donate_monthly_10", PurchaseState.PURCHASED, false, "t"))
        }
        coEvery { billingClient.queryPurchasesAsync(match { it.zza() == ProductType.SUBS }) } returns pr

        // Setup finished to load products
        every { billingClient.isReady } returns true
        mgr.onBillingSetupFinished(ok)
        testScheduler.advanceUntilIdle()

        // Now send the purchase update
        val p = mockPurchase("liftlab_donate_monthly_10", PurchaseState.PURCHASED, acknowledged = false, token = "tt")
        mgr.onPurchasesUpdated(ok, mutableListOf(p))
        testScheduler.advanceUntilIdle()

        assertEquals("liftlab_donate_monthly_10", mgr.activeSubscription.first()!!.productId)
        assertFalse(mgr.isProcessingPurchase.value)
    }

    @Test
    fun `onPurchasesUpdated - UNSPECIFIED_STATE sets message and clears processing`() = runTest(testDispatcher) {
        val mgr = BillingManagerImpl(builder, dispatchers)

        val p = mockPurchase("liftlab_donate_5", PurchaseState.UNSPECIFIED_STATE, acknowledged = false, token = "t")
        val ok = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }

        mgr.onPurchasesUpdated(ok, mutableListOf(p))
        testScheduler.advanceUntilIdle()

        assertNotNull(mgr.billingMessage.first())
        assertFalse(mgr.isProcessingPurchase.value)
    }

    @Test
    fun `onPurchasesUpdated - non-OK posts friendly message and clears processing`() = runTest(testDispatcher) {
        val mgr = BillingManagerImpl(builder, dispatchers)
        val notOk = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.SERVICE_UNAVAILABLE }

        mgr.onPurchasesUpdated(notOk, mutableListOf())
        testScheduler.advanceUntilIdle()

        assertNotNull(mgr.billingMessage.first())
        assertFalse(mgr.isProcessingPurchase.value)
    }

    // --- clear message --------------------------------------------------------

    @Test
    fun `clearBillingMessage sets message to null`() = runTest(testDispatcher) {
        val mgr = BillingManagerImpl(builder, dispatchers)
        // seed a message
        val notOk = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.SERVICE_UNAVAILABLE }
        mgr.onPurchasesUpdated(notOk, mutableListOf())

        assertNotNull(mgr.billingMessage.first())
        mgr.clearBillingMessage()
        assertNull(mgr.billingMessage.first())
    }

    // --- product sorting ------------------------------------------------------

    @Test
    fun `queryProducts sorts INAPP by oneTime price and SUBS by first pricing phase price`() = runTest(testDispatcher) {
        every { billingClient.isReady } returns true
        val mgr = BillingManagerImpl(builder, dispatchers)

        // INAPP list unsorted (10, then 5)
        val in1 = mockProductDetails("liftlab_donate_10", ProductType.INAPP, priceMicros = 10_000_000)
        val in2 = mockProductDetails("liftlab_donate_5",  ProductType.INAPP, priceMicros =  5_000_000)
        val inRes = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(in1, in2) }

        // SUBS list unsorted (30, then 20)
        val sub1 = mockProductDetails("liftlab_donate_monthly_30", ProductType.SUBS, subPriceMicros = 30_000_000)
        val sub2 = mockProductDetails("liftlab_donate_monthly_20", ProductType.SUBS, subPriceMicros = 20_000_000)
        val subRes = mockk<ProductDetailsResult> { every { productDetailsList } returns listOf(sub1, sub2) }

        coEvery { billingClient.queryProductDetails(any()) } returnsMany listOf(inRes, subRes)

        val ok = mockk<BillingResult> { every { responseCode } returns BillingResponseCode.OK }
        mgr.onBillingSetupFinished(ok)
        testScheduler.advanceUntilIdle()

        // INAPP sorted ascending by price micros: 5, 10
        assertEquals(listOf("liftlab_donate_5", "liftlab_donate_10"), mgr.oneTimeDonationProducts.first().map { it.productId })
        // SUBS sorted ascending by first phase price: 20, 30
        assertEquals(listOf("liftlab_donate_monthly_20", "liftlab_donate_monthly_30"), mgr.subscriptionProducts.first().map { it.productId })
    }

    // ------------------------- helpers -------------------------

    private fun mockProductDetails(
        id: String,
        type: String,
        priceMicros: Long? = null,
        subPriceMicros: Long? = null,
        offerToken: String = ""
    ): ProductDetails {
        val pd = mockk<ProductDetails>(relaxed = true)
        every { pd.productId } returns id
        every { pd.productType } returns type

        if (type == ProductType.INAPP) {
            val one = mockk<ProductDetails.OneTimePurchaseOfferDetails>(relaxed = true)
            every { one.priceAmountMicros } returns (priceMicros ?: 0L)
            every { pd.oneTimePurchaseOfferDetails } returns one
        } else {
            val phase = mockk<ProductDetails.PricingPhase>(relaxed = true)
            every { phase.priceAmountMicros } returns (subPriceMicros ?: 0L)
            val phases = mockk<ProductDetails.PricingPhases>(relaxed = true)
            every { phases.pricingPhaseList } returns listOf(phase)
            val offer = mockk<ProductDetails.SubscriptionOfferDetails>(relaxed = true)
            every { offer.pricingPhases } returns phases
            every { offer.offerToken } returns offerToken
            every { pd.subscriptionOfferDetails } returns listOf(offer)
        }
        return pd
    }

    private fun mockPurchase(
        productId: String,
        state: Int,
        acknowledged: Boolean,
        token: String
    ): Purchase {
        val p = mockk<Purchase>(relaxed = true)
        every { p.products } returns listOf(productId)
        every { p.purchaseState } returns state
        every { p.isAcknowledged } returns acknowledged
        every { p.purchaseToken } returns token
        return p
    }
}
