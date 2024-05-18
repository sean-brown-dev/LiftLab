package com.browntowndev.liftlab.ui.composables

import androidx.compose.runtime.Composable
import com.browntowndev.liftlab.core.common.PayUtils
import com.google.android.gms.wallet.PaymentsClient
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton

@Composable
fun DonateButton(paymentsClient: PaymentsClient) {
    PayButton(
        type = ButtonType.Donate,
        allowedPaymentMethods = PayUtils.allowedPaymentMethods.toString(),
        onClick = {
        }
    )
}

