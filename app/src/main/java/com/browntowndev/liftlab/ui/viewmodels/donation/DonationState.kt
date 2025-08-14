package com.browntowndev.liftlab.ui.viewmodels.donation

import androidx.compose.runtime.Stable
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

@Stable
data class DonationState(
    val initialized: Boolean = false,
    val isProcessingDonation: Boolean = false,
    val billingClient: BillingClient? = null,
    val activeSubscription: ProductDetails? = null,
    val newDonationSelection: ProductDetails? = null,
    val billingCompletionMessage: String? = null,
    val oneTimeDonationProducts: List<ProductDetails> = listOf(),
    val subscriptionProducts: List<ProductDetails> = listOf(),
)
