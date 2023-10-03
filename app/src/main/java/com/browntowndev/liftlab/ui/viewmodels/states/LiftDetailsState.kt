package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val previousSetResults: List<SetResult> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
)