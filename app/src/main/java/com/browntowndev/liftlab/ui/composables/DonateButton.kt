package com.browntowndev.liftlab.ui.composables

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient

@Composable
fun DonateButton(paymentsClient: PaymentsClient) {
    var isReadyToPay by remember { mutableStateOf(false) }

    // Check if Google Pay is available and ready to use.
    isReadyToPay(
        paymentsClient = paymentsClient,
        onIsReadyToPayCompleted = { isReadyToPay = it }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isReadyToPay) {
            Button(onClick = { requestPayment(paymentsClient) }) {
                Text(text = "Donate")
            }
        } else {
            Text(text = "Google Pay is not available.")
        }
    }
}

private fun isReadyToPay(
    paymentsClient: PaymentsClient,
    onIsReadyToPayCompleted: (Boolean) -> Unit,
) {
    val request = IsReadyToPayRequest.fromJson(
        """
        {
          "allowedPaymentMethods": [
            {"type": "CARD", "parameters": {"allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"]}},
            {"type": "PAYPAL"}
          ],
          "apiVersion": 2,
          "apiVersionMinor": 0
        }
        """.trimIndent()
    )

    val task = paymentsClient.isReadyToPay(request)
    task.addOnCompleteListener { completedTask ->
        if (completedTask.isSuccessful) {
            // Google Pay is available.
            onIsReadyToPayCompleted(true)
        } else {
            // Google Pay is not available.
            onIsReadyToPayCompleted(false)
        }
    }
}

private fun requestPayment(
    paymentsClient: PaymentsClient,
) {
    val paymentDataRequestJson =
        """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "allowedPaymentMethods": [
            {"type": "CARD", "parameters": {"allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"]}},
            {"type": "PAYPAL"}
          ],
          "transactionInfo": {"totalPrice": "10.00", "totalPriceStatus": "FINAL", "currencyCode": "USD"},
          "merchantInfo": {"merchantName": "Example Merchant"}
        }
        """.trimIndent()

    val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
    val task = paymentsClient.loadPaymentData(request)
    task.addOnCompleteListener { completedTask ->

    }
}