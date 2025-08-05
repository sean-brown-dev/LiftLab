package com.browntowndev.liftlab.core.data.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase.PurchaseState
import com.browntowndev.liftlab.core.common.THANK_YOU_DIALOG_BODY
import com.browntowndev.liftlab.core.common.toFriendlyMessage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingManagerImpl(
    billingClientBuilder: BillingClient.Builder,
    private val externalScope: CoroutineScope
) : BillingManager, PurchasesUpdatedListener, BillingClientStateListener {
    val oneTimeDonationProductIds =
        setOf(
            "liftlab_donate_5",
            "liftlab_donate_10",
            "liftlab_donate_20",
            "liftlab_donate_30",
            "liftlab_donate_50",
            "liftlab_donate_100",
        )

    val monthlyDonationProductIds =
        setOf(
            "liftlab_donate_monthly_5",
            "liftlab_donate_monthly_10",
            "liftlab_donate_monthly_20",
            "liftlab_donate_monthly_30",
            "liftlab_donate_monthly_50",
            "liftlab_donate_monthly_100",
        )

    private val _oneTimeDonationProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val oneTimeDonationProducts = _oneTimeDonationProducts.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val subscriptionProducts = _subscriptionProducts.asStateFlow()

    private val _activeSubscription = MutableStateFlow<ProductDetails?>(null)
    override val activeSubscription = _activeSubscription.asStateFlow()

    private val _isProcessingPurchase = MutableStateFlow(false)
    override val isProcessingPurchase = _isProcessingPurchase.asStateFlow()

    private val _billingMessage = MutableStateFlow<String?>(null)
    override val billingMessage = _billingMessage.asStateFlow()

    private lateinit var billingClient: BillingClient

    init {
        try {
            Log.d("BillingRepositoryImpl", "Initializing billing client")
            billingClient = billingClientBuilder
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build()

            if (!billingClient.isReady) {
                billingClient.startConnection(this)
            }
        } catch(e: Exception) {
            Log.e("BillingRepositoryImpl", "Error initializing billing client", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            _billingMessage.update { "Failed to initialize billing client" }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        try {
            Log.d("BillingRepositoryImpl", "Billing setup finished with result: ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingResponseCode.OK) {
                externalScope.launch {
                    queryProducts()
                    acknowledgeUnacknowledgedPurchases(ProductType.SUBS)
                    acknowledgeUnacknowledgedPurchases(ProductType.INAPP)
                    queryActiveSubscription()
                }
            } else {
                Log.e("BillingRepositoryImpl", "Billing setup failed with result: ${billingResult.responseCode}")
                FirebaseCrashlytics.getInstance().log("Billing setup failed with result: ${billingResult.responseCode}")
                _billingMessage.update { "Failed to initialize billing client" }
            }
        } catch (e: Exception) {
            Log.e("BillingRepositoryImpl", "Error in onBillingSetupFinished", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            _billingMessage.update { "Failed to initialize billing client" }
        }
    }

    override fun onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        if (::billingClient.isInitialized) {
            billingClient.startConnection(this)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    externalScope.launch { handlePurchase(purchase) }
                } else if (purchase.purchaseState == PurchaseState.UNSPECIFIED_STATE) {
                    _billingMessage.update { "An error may have occurred. Please verify the donation was successful." }
                    _isProcessingPurchase.update { false }
                }
            }
        } else {
            _billingMessage.update { billingResult.responseCode.toFriendlyMessage() }
            _isProcessingPurchase.update { false }
        }
    }

    override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        _isProcessingPurchase.update { true }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    if (productDetails.productType == ProductType.SUBS) {
                        productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun clearBillingMessage() {
        _billingMessage.update { null }
    }

    private suspend fun queryProducts() {
        val oneTimeProducts = getProducts(ProductType.INAPP, oneTimeDonationProductIds)
            .sortedBy { it.oneTimePurchaseOfferDetails?.priceAmountMicros }
        _oneTimeDonationProducts.update { oneTimeProducts }
        Log.d("BillingRepositoryImpl", "One-time donation products: $oneTimeProducts")

        val subs = getProducts(ProductType.SUBS, monthlyDonationProductIds)
            .sortedBy {
                it.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.priceAmountMicros
            }
        _subscriptionProducts.update { subs }
        Log.d("BillingRepositoryImpl", "Subscription products: $subs")
    }

    private suspend fun queryActiveSubscription() {
        val purchaseParams = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        val purchasesResult = billingClient.queryPurchasesAsync(purchaseParams)

        val activeSubId = purchasesResult.purchasesList
            .firstOrNull { it.purchaseState == PurchaseState.PURCHASED || it.purchaseState == PurchaseState.PENDING }
            ?.products?.firstOrNull()

        _activeSubscription.update {
            _subscriptionProducts.value.find { it.productId == activeSubId }
        }
    }


    private suspend fun handlePurchase(purchase: Purchase) {
        try {
            if (purchase.products.isEmpty()) {
                _billingMessage.update { BillingResponseCode.USER_CANCELED.toFriendlyMessage() }
                return
            }

            val productId = purchase.products.first()
            val isOneTimeDonation = oneTimeDonationProductIds.contains(productId)
            val result: BillingResult?

            if (isOneTimeDonation) {
                val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                result = billingClient.consumePurchase(consumeParams).billingResult
            } else {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
            }

            if (result.responseCode == BillingResponseCode.OK) {
                if (isOneTimeDonation) {
                    _billingMessage.update { THANK_YOU_DIALOG_BODY }
                } else {
                    // Refresh active subscription state after successful acknowledgement
                    queryActiveSubscription()
                }
            } else {
                val productDetails = (_oneTimeDonationProducts.value + _subscriptionProducts.value).find { it.productId == productId }
                val errorMessage = result.responseCode.toFriendlyMessage()
                _billingMessage.update { "Error processing: ${productDetails?.name}. $errorMessage" }
            }
        } finally {
            _isProcessingPurchase.update { false }
        }
    }

    private suspend fun acknowledgeUnacknowledgedPurchases(productType: String) {
        val purchaseParams = QueryPurchasesParams.newBuilder().setProductType(productType).build()
        billingClient.queryPurchasesAsync(purchaseParams).purchasesList.forEach { purchase ->
            if (purchase.purchaseState == PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                handlePurchase(purchase)
            }
        }
    }

    private suspend fun getProducts(productType: String, productIds: Set<String>): List<ProductDetails> {
        if (productIds.isEmpty()) return emptyList()

        val products = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        val productDetailsResult = billingClient.queryProductDetails(params)
        return productDetailsResult.productDetailsList ?: emptyList()
    }
}