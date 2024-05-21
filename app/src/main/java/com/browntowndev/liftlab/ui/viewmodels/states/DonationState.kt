package com.browntowndev.liftlab.ui.viewmodels.states

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

data class DonationState(
    val initialized: Boolean = false,
    val isProcessingDonation: Boolean = false,
    val billingClient: BillingClient? = null,
    val activeSubscription: ProductDetails? = null,
    val newDonationSelection: ProductDetails? = null,
    val billingCompletionMessage: String? = null,
    val oneTimeDonationProducts: List<ProductDetails> = listOf(),
    val subscriptionProducts: List<ProductDetails> = listOf(),
) {
    val oneTimeDonationProductIds by lazy {
        hashSetOf(
            "liftlab_donate_5",
            "liftlab_donate_10",
            "liftlab_donate_20",
            "liftlab_donate_30",
            "liftlab_donate_50",
            "liftlab_donate_100",
        )
    }

    val monthlyDonationProductIds by lazy {
        hashSetOf(
            "liftlab_donate_monthly_5",
            "liftlab_donate_monthly_10",
            "liftlab_donate_monthly_20",
            "liftlab_donate_monthly_30",
            "liftlab_donate_monthly_50",
            "liftlab_donate_monthly_100",
        )
    }
}
