package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.ui.models.workout.DisplayProgressionScheme

object EnumMapping {
    fun DisplayProgressionScheme.toDomainModel() =
        ProgressionScheme.fromDisplayName(name)

    fun ProgressionScheme.toUiModel() =
        DisplayProgressionScheme(
            name = displayName,
            shortName = shortName,
            isLinearProgression = isLinearProgression,
            canHaveCustomSets = canHaveCustomSets,
            rpeLabel = rpeLabel,
        )

    fun List<ProgressionScheme>.toDomainModel() =
        fastMap {
            DisplayProgressionScheme(
                name = it.displayName,
                shortName = it.shortName,
                isLinearProgression = it.isLinearProgression,
                canHaveCustomSets = it.canHaveCustomSets,
                rpeLabel = it.rpeLabel,
            )
        }

    fun List<VolumeType>.toDisplayNames() =
        fastMap { it.displayName }
}