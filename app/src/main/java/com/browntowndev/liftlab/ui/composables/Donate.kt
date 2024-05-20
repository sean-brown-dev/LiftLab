package com.browntowndev.liftlab.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.findActivity
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Donate(
    paddingValues: PaddingValues,
    onBackPressed: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    if (activity == null) return

    val billingClientBuilder = remember { BillingClient.newBuilder(context) }
    val donationViewModel: DonationViewModel = koinViewModel {
        parametersOf(billingClientBuilder)
    }
    val state by donationViewModel.state.collectAsState()

    BackHandler {
        onBackPressed()
    }

    Column (
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(50.dp)
                .padding(top = 10.dp),
            painter = painterResource(id = R.drawable.donate_icon),
            contentDescription = ""
        )
        Text(text = stringResource(R.string.thank_you), fontSize = 24.sp)
        Text(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            text = stringResource(R.string.donate_message)
        )

        var monthly by remember { mutableStateOf(false) }
        val productOptions = remember(monthly) {
            if (monthly) {
                state.subscriptionProducts
            } else {
                state.oneTimeDonationProducts
            }
        }
        Row (verticalAlignment = Alignment.CenterVertically) {
            DurationOption(
                text = "One Time",
                isSelected = !monthly,
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                onSelected = { monthly = false }
            )
            DurationOption(
                text = "Monthly",
                isSelected = monthly,
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                onSelected = { monthly = true }
            )
        }
        DonationOption(
            productOptions,
            selectedProduct = state.newDonationSelection,
            onDonationChanged = { donationViewModel.setNewDonationOption(it) }
        )
        OutlinedButton(
            modifier = Modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
            onClick = {
                donationViewModel.processDonation(activity)
            }
        ) {
            val buttonText = remember(monthly) {
                if (monthly) R.string.setup_donation else R.string.send_donation
            }
            Text(
                modifier = Modifier.padding(10.dp),
                text = stringResource(buttonText),
                fontSize = 16.sp,
            )
        }
    }

    if (state.billingError?.isNotEmpty() == true) {
        Dialog(onDismissRequest = { donationViewModel.clearBillingError() }) {
            Text(text = "Error processing donation. Message: ${state.billingError}")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonationOption(
    options: List<ProductDetails>,
    selectedProduct: ProductDetails?,
    onDonationChanged: (product: ProductDetails) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 3,
    ) {
        options.fastForEach { option ->
            val isSelected = remember(option, selectedProduct) { option == selectedProduct }
            val optionText = remember(option) {
                option.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: option.oneTimePurchaseOfferDetails?.formattedPrice
            }
            Surface(
                modifier = Modifier
                    .height(95.dp)
                    .width(95.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    if (!isSelected) {
                        onDonationChanged(option)
                    }
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$optionText",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationOption(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onSelected: () -> Unit,
) {
    Surface(
        shape = shape,
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primaryContainer),
        onClick = onSelected,
    ) {
        Row(
            modifier = Modifier
                .width(125.dp)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.one_time_donation)
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
