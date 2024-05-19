package com.browntowndev.liftlab.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.browntowndev.liftlab.core.common.PayUtils
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton

@Composable
fun DonateButton(
    onDonationRequested: (priceInCents: Long) -> Unit,
) {
    var priceInCents by remember { mutableLongStateOf(10000L) }

    PayButton(
        type = ButtonType.Donate,
        allowedPaymentMethods = PayUtils.allowedPaymentMethods.toString(),
        onClick = {
            onDonationRequested(priceInCents)
        }
    )
}
