package com.browntowndev.liftlab.core.data.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow

/**
 * Interface for a repository that handles all interactions with the Google Play Billing Library.
 * This abstracts the billing logic away from ViewModels.
 */
interface BillingManager {
    /**
     * Flow emitting the list of available one-time donation products.
     */
    val oneTimeDonationProducts: Flow<List<ProductDetails>>

    /**
     * Flow emitting the list of available subscription products.
     */
    val subscriptionProducts: Flow<List<ProductDetails>>

    /**
     * Flow emitting the user's currently active subscription, or null if none exists.
     */
    val activeSubscription: Flow<ProductDetails?>

    /**
     * Flow emitting true when a purchase is in progress.
     */
    val isProcessingPurchase: Flow<Boolean>

    /**
     * Flow emitting a message to be shown to the user after a billing operation (e.g., "Thank you!" or an error).
     * Emits null when there is no message to show.
     */
    val billingMessage: Flow<String?>

    /**
     * Launches the Google Play purchase flow for a given product.
     * The result of this operation is handled internally and exposed via the public Flows.
     *
     * @param activity The activity context needed to launch the billing flow UI.
     * @param productDetails The product the user wishes to purchase.
     */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails)

    /**
     * Call this when the UI has displayed the billing message to clear it.
     */
    fun clearBillingMessage()
}