package com.browntowndev.liftlab.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.PayUtils
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton

@Composable
fun Donate(
    paddingValues: PaddingValues,
    onBackPressed: () -> Unit,
    onDonationRequested: (priceInCents: Long, monthly: Boolean) -> Unit,
) {
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

        var priceInCents by remember { mutableLongStateOf(0L) }
        var monthly by remember { mutableStateOf(false) }
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
        listOf(1, 2).fastForEachIndexed { index, _ ->
            DonationOption(
                rowIndex = index,
                priceInCents = priceInCents,
                onDonationChanged = { priceInCents = it }
            )
        }

        CurrencyTextField(
            modifier = Modifier
                .width(325.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(10.dp),
                ),
            label = {
                Text(text = "Custom Amount")
            },
            onChange = {
                it.replace("$", "").let { donation ->
                    if (donation.isNotEmpty()) {
                        priceInCents = (donation.toFloat() * 100).toLong()
                    }
                }
            }
        )
        PayButton(
            type = ButtonType.Donate,
            allowedPaymentMethods = PayUtils.allowedPaymentMethods.toString(),
            onClick = {
                onDonationRequested(priceInCents, monthly)
            }
        )
    }
}

@Composable
private fun DonationOption(
    rowIndex: Int,
    priceInCents: Long,
    onDonationChanged: (priceInCents: Long) -> Unit,
) {
    val options = if (rowIndex == 0) listOf(5L, 10L, 20L) else listOf(30L, 50L, 100L)
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.fastForEach { donateAmount ->
            val isSelected = remember(
                donateAmount,
                priceInCents
            ) { donateAmount == (priceInCents / 100L) }

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
                onClick = { onDonationChanged(donateAmount * 100L) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$$donateAmount",
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
