package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val previousSetResults: List<SetResult> = listOf(),
) {
    val volumeTypes: List<String> by lazy {
        lift?.volumeTypesBitmask?.getVolumeTypes()
            ?.fastMap {
                it.displayName()
            } ?: listOf()
    }

    val secondaryVolumeTypes: List<String> by lazy {
        lift?.secondaryVolumeTypesBitmask?.getVolumeTypes()
            ?.fastMap {
                it.displayName()
            } ?: listOf()
    }
}