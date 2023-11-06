package com.browntowndev.liftlab.ui.viewmodels.states

import kotlin.time.Duration

data class SettingsState(
    val importConfirmationDialogShown: Boolean = false,
    val defaultRestTimeString: Duration? = null,
    val defaultIncrement: Float? = null,
)
