package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import kotlin.time.Duration

data class SettingsState(
    val importConfirmationDialogShown: Boolean = false,
    val defaultRestTime: Duration? = null,
    val defaultIncrement: Float? = null,
    val isDonateScreenVisible: Boolean = false,
    val queriedForProgram: Boolean = false,
    val activeProgram: ProgramDto? = null,
)
