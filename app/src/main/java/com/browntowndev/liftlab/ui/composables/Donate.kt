package com.browntowndev.liftlab.ui.composables

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.findActivity
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.valentinilk.shimmer.shimmer
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

    DonationMenu(
        initialized = state.initialized,
        paddingValues = paddingValues,
        newDonationSelection = state.newDonationSelection,
        subscriptionProducts = state.subscriptionProducts,
        oneTimeDonationProducts = state.oneTimeDonationProducts,
        billingError = state.billingError,
        onClearBillingError = donationViewModel::clearBillingError,
        onUpdateDonationProduct = donationViewModel::setNewDonationOption,
        onProcessDonation = { donationViewModel.processDonation(activity) }
    )
}

@Composable
private fun DonationMenu(
    initialized: Boolean,
    paddingValues: PaddingValues,
    newDonationSelection: ProductDetails?,
    subscriptionProducts: List<ProductDetails>,
    oneTimeDonationProducts: List<ProductDetails>,
    billingError: String?,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
) {
    Column(
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
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.thank_you), fontSize = 24.sp)
            Text(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                text = stringResource(R.string.donate_message),
                textAlign = TextAlign.Center,
            )
        }

        var monthly by remember { mutableStateOf(false) }
        val productOptions: List<ProductDetails?> =
            remember(monthly, subscriptionProducts, oneTimeDonationProducts) {
                if (monthly) {
                    subscriptionProducts
                } else {
                    oneTimeDonationProducts
                }
            }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DurationOption(
                modifier = if (initialized) Modifier else Modifier.shimmer(),
                text = remember(initialized) { if (initialized) "One Time" else "" },
                isSelected = remember(initialized, monthly) { if (initialized) !monthly else false },
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                onSelected = {
                    if (initialized && monthly) {
                        onUpdateDonationProduct(null)
                        monthly = false
                    }
                }
            )
            DurationOption(
                modifier = if (initialized) Modifier else Modifier.shimmer(),
                text = remember(initialized) { if (initialized) "Monthly" else "" },
                isSelected = remember(initialized, monthly) { if (initialized) monthly else false },
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                onSelected = {
                    if (initialized && !monthly) {
                        onUpdateDonationProduct(null)
                        monthly = true
                    }
                }
            )
        }
        DonationOption(
            modifier = if (initialized) Modifier else Modifier.shimmer(),
            options = if (initialized) productOptions else List(6) { null },
            selectedProduct = newDonationSelection,
            onDonationChanged = onUpdateDonationProduct,
        )
        val isDonationSelected = remember(newDonationSelection) { newDonationSelection != null }
        val primary = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outline
        val borderColor = remember(isDonationSelected) { if (isDonationSelected) primary else outline }
        OutlinedButton(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.CenterHorizontally)
                .apply {
                    if (!initialized) {
                        shimmer()
                    }
                },
            enabled = isDonationSelected && initialized,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                width = 1.dp,
                color = borderColor
            ),
            onClick = onProcessDonation,
        ) {
            val buttonTextId = remember(monthly) {
                if (monthly) R.string.setup_donation
                else R.string.send_donation
            }
            Text(
                modifier = Modifier.padding(10.dp),
                text = if (initialized) stringResource(buttonTextId)
                else List(stringResource(buttonTextId).length) { "" }.joinToString(separator = " "),
                fontSize = 16.sp,
            )
        }
    }

    if (billingError?.isNotEmpty() == true) {
        Dialog(onDismissRequest = onClearBillingError) {
            Text(text = billingError)
        }
    }
}

@Composable
private fun DurationOption(
    modifier: Modifier,
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onSelected: () -> Unit,
) {
    Surface(
        modifier = modifier,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonationOption(
    modifier: Modifier,
    options: List<ProductDetails?>,
    selectedProduct: ProductDetails?,
    onDonationChanged: (product: ProductDetails?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachRow = 3,
    ) {
        options.fastForEach { option ->
            val isSelected = remember(option, selectedProduct) { option != null && option == selectedProduct }
            val optionText = remember(option) {
                option?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: option?.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: ""
            }
            Surface(
                modifier = modifier.then(
                    Modifier
                        .height(95.dp)
                        .width(95.dp)
                ),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    if (!isSelected && option != null) {
                        onDonationChanged(option)
                    }
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = optionText,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
