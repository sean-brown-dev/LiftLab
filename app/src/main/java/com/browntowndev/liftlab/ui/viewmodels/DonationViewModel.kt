package com.browntowndev.liftlab.ui.viewmodels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.core.data.billing.BillingRepository
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.ui.viewmodels.states.DonationState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus

class DonationViewModel(
    private val billingRepository: BillingRepository,
    eventBus: EventBus
): BaseViewModel(eventBus) {
    private var _state = MutableStateFlow(DonationState())
    val state = _state.asStateFlow()

    init {
        combine(
            billingRepository.oneTimeDonationProducts,
            billingRepository.subscriptionProducts,
            billingRepository.activeSubscription,
            billingRepository.isProcessingPurchase,
            billingRepository.billingMessage
        ) { oneTime, subs, activeSub, isProcessing, message ->
            DonationState(
                oneTimeDonationProducts = oneTime,
                subscriptionProducts = subs,
                activeSubscription = activeSub,
                isProcessingDonation = isProcessing,
                billingCompletionMessage = message,
            )
        }.onEach { donationState ->
            _state.update {
                it.copy(
                    initialized = true,
                    oneTimeDonationProducts = donationState.oneTimeDonationProducts,
                    subscriptionProducts = donationState.subscriptionProducts,
                    activeSubscription = donationState.activeSubscription,
                    isProcessingDonation = donationState.isProcessingDonation,
                    billingCompletionMessage = donationState.billingCompletionMessage,
                )
            }
        }.catch {
            Log.e("DonationViewModel", "Error getting billing state", it)
            FirebaseCrashlytics.getInstance().recordException(it)
            emitUserMessage("Failed to load Donation screen.")
        }.launchIn(viewModelScope)
    }

    fun setNewDonationOption(newDonation: ProductDetails?) {
        _state.update { it.copy(newDonationSelection = newDonation) }
    }

    fun processDonation(activity: Activity) {
        _state.value.newDonationSelection?.let { productDetails ->
            billingRepository.launchPurchaseFlow(activity, productDetails)
        }
    }

    fun clearBillingError() {
        billingRepository.clearBillingMessage()
    }
}