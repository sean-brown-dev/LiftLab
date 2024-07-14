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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.INCREMENT_OPTIONS
import com.browntowndev.liftlab.core.common.REST_TIME_RANGE
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.common.toWholeNumberOrOneDecimalString
import com.browntowndev.liftlab.ui.composables.ConfirmationDialog
import com.browntowndev.liftlab.ui.composables.Donate
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.HyperlinkTextField
import com.browntowndev.liftlab.ui.composables.LiftLabDialog
import com.browntowndev.liftlab.ui.composables.NumberPickerSpinner
import com.browntowndev.liftlab.ui.composables.SectionLabel
import com.browntowndev.liftlab.ui.composables.TimeSelectionSpinner
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.format.DateTimeFormatter
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
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
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val settingsViewModel: SettingsViewModel = koinViewModel {
        parametersOf(onNavigateBack)
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
                SettingDivider(paddingValues = PaddingValues(bottom = 25.dp))
                SectionLabel(
                    modifier = Modifier.padding(bottom = 5.dp),
                    text = "PERIODIZATION AND PROGRESSION",
                    fontSize = 14.sp
                )
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Deload Prompt",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Get prompted to begin or skip deload microcycle.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        enabled = !state.liftSpecificDeloading,
                        checked = state.promptOnDeloadStart && !state.liftSpecificDeloading,
                        onCheckedChange = settingsViewModel::handlePromptForDeloadWeekChange,
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
                    modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Lift Specific Deloads",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Set deloads at the lift level. Disables deload microcycles.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.liftSpecificDeloading,
                        onCheckedChange = settingsViewModel::handleLiftSpecificDeloadChange,
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
                        Text(
                            text = "WARNING: Disabling may reduce weight recommendation accuracy.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = !state.useAllLiftDataForRecommendations,
                        onCheckedChange = settingsViewModel::handleUseAllDataForRecommendationsChange,
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
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp),
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
                        Text(
                            text = "WARNING: Disabling may reduce weight recommendation accuracy.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.useOnlyResultsFromLiftInSamePosition,
                        onCheckedChange = settingsViewModel::handleUseOnlyLiftsFromSamePositionChange,
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
                SettingDivider()
                SectionLabel(text = "DEFAULTS", fontSize = 14.sp)
                var isIncrementDialogVisible by remember { mutableStateOf(false) }
                val defaultIncrement = remember(state.defaultIncrement) {
                    state.defaultIncrement ?: DEFAULT_INCREMENT_AMOUNT
                }
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Weight Increment", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { isIncrementDialogVisible = true }) {
                        Text(
                            text = remember(defaultIncrement) { defaultIncrement.toWholeNumberOrOneDecimalString() },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                        )
                    }
                }

                LiftLabDialog(
                    isVisible = isIncrementDialogVisible,
                    header = "Default Weight Increment",
                    subHeader = "Used Unless Overridden on Lift",
                    onDismiss = { isIncrementDialogVisible = false }
                ) {
                    NumberPickerSpinner(
                        options = INCREMENT_OPTIONS,
                        initialValue = defaultIncrement,
                        onChanged = settingsViewModel::updateIncrement,
                    )
                }
            }
            item {
                var isRestTimeDialogVisible by remember { mutableStateOf(false) }
                val defaultRestTime = remember(state.defaultRestTime) {
                    state.defaultRestTime
                        ?: DEFAULT_REST_TIME.toDuration(DurationUnit.MILLISECONDS)
                }
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rest Time", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { isRestTimeDialogVisible = true }) {
                        Text(
                            text = remember(defaultRestTime) { defaultRestTime.toTimeString() },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                        )
                    }
                }

                LiftLabDialog(
                    isVisible = isRestTimeDialogVisible,
                    header = "Default Weight Increment",
                    subHeader = "Used Unless Overridden on Lift",
                    onDismiss = { isRestTimeDialogVisible = false }
                ) {
                    TimeSelectionSpinner(
                        time = defaultRestTime,
                        onTimeChanged = settingsViewModel::updateDefaultRestTime,
                        rangeInMinutes = REST_TIME_RANGE,
                        secondsStepSize = 5,
                    )
                }
            }
            item {
                SettingDivider()
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
                    ConfirmationDialog(
                        header = "Warning!",
                        textAboveContent = "This will replace all of your data with the data in the imported database. There is no way to undo this.",
                        onConfirm = {
                            settingsViewModel.toggleImportConfirmationDialog()
                            onRestore()
                        },
                        onCancel = { settingsViewModel.toggleImportConfirmationDialog() }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row (horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Export Database", fontSize = 18.sp)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(isPersistent = true),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        text = "Backs up to \"Documents\\Lift Lab Backups\"."
                                    )
                                }
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Outlined.Info,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(R.string.backup_tooltip)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onBackup) {
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
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val context = LocalContext.current
                    Row (horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Scheduled Backups", fontSize = 18.sp)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(isPersistent = true),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        text = "Runs a daily backup to the \"Documents\\Lift Lab Backups\" folder.\n\n" +
                                                "Use an app, such as Autosync for Google Drive, to back them up to your " +
                                                "Google Drive in case you switch devices."
                                    )
                                }
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Outlined.Info,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(R.string.backup_tooltip)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.scheduledBackupsEnabled,
                        onCheckedChange = {
                            settingsViewModel.updateAreScheduledBackupsEnabled(
                                context = context,
                                enabled = it
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
                if (state.scheduledBackupsEnabled) {
                    var isBackupTimeDialogVisible by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Backup Time", fontSize = 18.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { isBackupTimeDialogVisible = true }) {
                            val backupTime = remember(state.scheduledBackupTime) {
                                DateTimeFormatter.ofPattern("hh:mm a").format(state.scheduledBackupTime)
                            }
                            Text(
                                text = backupTime,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                            )
                        }
                    }

                    if (isBackupTimeDialogVisible) {
                        val context = LocalContext.current
                        val timePickerState = rememberTimePickerState(
                            initialHour = state.scheduledBackupTime.hour,
                            initialMinute = state.scheduledBackupTime.minute,
                            is24Hour = false
                        )

                        ConfirmationDialog(
                            header = "Backup Time",
                            textAboveContent = "",
                            textAboveContentPadding = PaddingValues(0.dp),
                            contentPadding = PaddingValues(start = 10.dp, bottom = 10.dp, end = 10.dp),
                            onConfirm = {
                                settingsViewModel.updateScheduledBackupTime(
                                    context = context,
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute
                                )
                                isBackupTimeDialogVisible = false
                            },
                            onCancel = { isBackupTimeDialogVisible = false },
                        ) {
                            TimePicker(
                                state = timePickerState
                            )
                        }
                    }
                }
            }
            item {
                SettingDivider()
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

@Composable
private fun SettingDivider(
    paddingValues: PaddingValues = PaddingValues(top = 25.dp, bottom = 25.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues),
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(.95f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
