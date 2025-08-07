package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.ui.models.workout.displayName

object Utils {
    fun getAllVolumeTypeDisplayNames(): List<String> =
        VolumeType.entries.fastMap { it.displayName() }
}