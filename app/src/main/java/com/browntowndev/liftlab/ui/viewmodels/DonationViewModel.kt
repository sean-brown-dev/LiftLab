package com.browntowndev.liftlab.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.ui.viewmodels.states.DonationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class DonationViewModel(
    billingClientBuilder: BillingClient.Builder,
    transactionScope: TransactionScope,
    eventBus: EventBus
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(DonationState())
    val state = _state.asStateFlow()

    init {
        billingClientBuilder
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .setListener(getPurchasesUpdatedListener())
            .build()
            .also { billingClient ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            _state.update {
                                it.copy(billingClient = billingClient)
                            }

                            viewModelScope.launch {
                                val activeSubscription = getSubscriptionPurchases(billingClient)
                                val availableOneTimeDonations = getProducts(
                                    billingClient = billingClient,
                                    productType = ProductType.INAPP,
                                    productIds = _state.value.oneTimeDonationProductIds)
                                val availableSubscriptions = getProducts(
                                    billingClient = billingClient,
                                    productType = ProductType.SUBS,
                                    productIds = _state.value.monthlyDonationProductIds)

                                _state.update {
                                    it.copy(
                                        activeSubscription = activeSubscription,
                                        oneTimeDonationProducts = availableOneTimeDonations,
                                        subscriptionProducts = availableSubscriptions,
                                    )
                                }
                            }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        billingClient.startConnection(this)
                    }
                })
            }
    }

    fun setNewDonationOption(newDonation: ProductDetails) {
        _state.update {
            it.copy(newDonationSelection =  newDonation)
        }
    }

    fun processDonation(activity: Activity) {
        _state.value.newDonationSelection?.let { productDetails ->
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(productDetails)
                    .let { builder ->
                        if (productDetails.productType == ProductType.SUBS) {
                            builder.setOfferToken(productDetails.subscriptionOfferDetails!!.first().offerToken)
                        }
                        builder.build()
                    }
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            _state.value.billingClient?.launchBillingFlow(activity, billingFlowParams)
        }
    }

    private fun getPurchasesUpdatedListener(): PurchasesUpdatedListener {
        return PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        val acknowledgePurchaseResponseListener =
            AcknowledgePurchaseResponseListener { result ->
                if (result.responseCode != 200) {
                    _state.update {
                        it.copy(billingError = result.debugMessage)
                    }
                }
            }

        viewModelScope.launch {
            _state.value.billingClient?.acknowledgePurchase(
                acknowledgePurchaseParams,
                acknowledgePurchaseResponseListener
            )
        }
    }

    private suspend fun getSubscriptionPurchases(billingClient: BillingClient): String? {
        val purchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        val purchasesResult = billingClient.queryPurchasesAsync(purchaseParams)
        val activeSubscription = purchasesResult.purchasesList
            .firstOrNull { purchase ->
                val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                if (isPurchased && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                isPurchased || purchase.purchaseState == Purchase.PurchaseState.PENDING
            }?.products?.firstOrNull()

        return activeSubscription
    }

    private suspend fun getProducts(billingClient: BillingClient, productType: String, productIds: List<String>): List<ProductDetails> {
        val subscriptions = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(subscriptions)

        val productDetailsResult = billingClient.queryProductDetails(params.build())

        return productDetailsResult.productDetailsList ?: listOf()
    }

    fun clearBillingError() {
        _state.update { it.copy(billingError = null) }
    }
}