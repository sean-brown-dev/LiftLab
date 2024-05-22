package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.INCREMENT_OPTIONS
import com.browntowndev.liftlab.core.common.REST_TIME_RANGE
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.ui.composables.ConfirmationModal
import com.browntowndev.liftlab.ui.composables.Donate
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.HyperlinkTextField
import com.browntowndev.liftlab.ui.composables.NumberPickerSpinner
import com.browntowndev.liftlab.ui.composables.SectionLabel
import com.browntowndev.liftlab.ui.composables.TimeSelectionSpinner
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun Settings(
    roomBackup: RoomBackup,
    paddingValues: PaddingValues,
    screenId: String?,
    initialized: Boolean,
    isProcessingDonation: Boolean,
    activeSubscription: ProductDetails?,
    newDonationSelection: ProductDetails?,
    subscriptionProducts: List<ProductDetails>,
    oneTimeDonationProducts: List<ProductDetails>,
    billingCompletionMessage: String?,
    onClearBillingError: () -> Unit,
    onUpdateDonationProduct: (donationProduct: ProductDetails?) -> Unit,
    onProcessDonation: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val settingsViewModel: SettingsViewModel = koinViewModel {
        parametersOf(roomBackup, onNavigateBack)
    }
    val state by settingsViewModel.state.collectAsState()

    settingsViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = settingsViewModel)

    if (state.isDonateScreenVisible) {
        Donate(
            paddingValues = paddingValues,
            initialized = initialized,
            isProcessingDonation = isProcessingDonation,
            activeSubscription = activeSubscription,
            newDonationSelection = newDonationSelection,
            subscriptionProducts = subscriptionProducts,
            oneTimeDonationProducts = oneTimeDonationProducts,
            billingError = billingCompletionMessage,
            onClearBillingError = onClearBillingError,
            onUpdateDonationProduct = onUpdateDonationProduct,
            onProcessDonation = onProcessDonation,
            onBackPressed = { settingsViewModel.toggleDonationScreen() },
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionLabel(modifier = Modifier.padding(top = 25.dp, bottom = 10.dp), text = "DONATE", fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.support_lift_lab),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = settingsViewModel::toggleDonationScreen) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = R.drawable.donate_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.donate),
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, bottom = 25.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(.95f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                SectionLabel(text = "DATA MANAGEMENT", fontSize = 14.sp)
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Import Database", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = settingsViewModel::toggleImportConfirmationDialog) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = R.drawable.upload_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.import_database),
                        )
                    }
                }
                if (state.importConfirmationDialogShown) {
                    ConfirmationModal(
                        header = "Warning!",
                        body = "This will replace all of your data with the data in the imported database. There is no way to undo this.",
                        onConfirm = { settingsViewModel.importDatabase() },
                        onCancel = { settingsViewModel.toggleImportConfirmationDialog() }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Export Database", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = settingsViewModel::exportDatabase) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = R.drawable.download_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.export_database),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, bottom = 25.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(.95f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                SectionLabel(text = "PROGRESSION", fontSize = 14.sp)
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = "WARNING: Disabling may reduce weight recommendation accuracy.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Weight Recommendations",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Only Sets from Previous Workout",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    var useAllData by remember {
                        mutableStateOf(
                            SettingsManager.getSetting(
                                USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
                                DEFAULT_USE_ALL_WORKOUT_DATA
                            )
                        )
                    }
                    Switch(
                        checked = !useAllData,
                        onCheckedChange = {
                            useAllData = !it
                            SettingsManager.setSetting(
                                USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
                                !it
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.secondary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Weight Recommendations",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Only Sets from Lifts in Same Order Position",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    var enforcePosition by remember {
                        mutableStateOf(
                            SettingsManager.getSetting(
                                ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
                                DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
                            )
                        )
                    }
                    Switch(
                        checked = enforcePosition,
                        onCheckedChange = {
                            enforcePosition = it
                            SettingsManager.setSetting(
                                ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
                                it
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.secondary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, bottom = 25.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(.95f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                SectionLabel(text = "DEFAULTS", fontSize = 14.sp)
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Weight Increment", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    NumberPickerSpinner(
                        modifier = Modifier.padding(start = 165.dp),
                        options = INCREMENT_OPTIONS,
                        initialValue = state.defaultIncrement ?: DEFAULT_INCREMENT_AMOUNT,
                        onChanged = { settingsViewModel.updateIncrement(it) }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rest Time", fontSize = 18.sp)
                    TimeSelectionSpinner(
                        modifier = Modifier.padding(start = 100.dp),
                        time = state.defaultRestTimeString ?: DEFAULT_REST_TIME.toDuration(
                            DurationUnit.MILLISECONDS
                        ),
                        onTimeChanged = { settingsViewModel.updateDefaultRestTime(it) },
                        rangeInMinutes = REST_TIME_RANGE,
                        secondsStepSize = 5,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, bottom = 25.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(.95f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            item {
                SectionLabel(
                    modifier = Modifier.padding(bottom = 10.dp),
                    text = "CONTACT AND TERMS",
                    fontSize = 14.sp
                )
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.instagram_glyph_white),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = stringResource(R.string.instagram_icon),
                    )
                    HyperlinkTextField(
                        text = stringResource(R.string.ig_handle),
                        url = stringResource(R.string.ig_url)
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Outlined.Email,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = stringResource(R.string.email_icon),
                    )
                    HyperlinkTextField(
                        text = stringResource(R.string.email_address),
                        url = "mailto:${stringResource(R.string.email_address)}"
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 25.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Outlined.Info,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = stringResource(R.string.terms_of_service_icon),
                    )
                    HyperlinkTextField(
                        text = stringResource(R.string.terms_of_service),
                        url = stringResource(R.string.terms_of_service_url)
                    )
                }
            }
        }
    }
}
