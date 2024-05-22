package com.browntowndev.liftlab.ui.viewmodels.states

import kotlin.time.Duration

data class SettingsState(
    val importConfirmationDialogShown: Boolean = false,
    val defaultRestTime: Duration? = null,
    val defaultIncrement: Float? = null,
    val isDonateScreenVisible: Boolean = false,
)
