package com.browntowndev.liftlab.ui.extensions

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection

fun String.toVolumeType(): VolumeType =
    VolumeType.entries.find { it.name.equals(this, ignoreCase = true) }
        ?: throw IllegalArgumentException("Invalid volume type: $this")

fun VolumeType.displayName(): String {
    return when (this) {
        VolumeType.CHEST -> "Chest"
        VolumeType.BACK -> "Back"
        VolumeType.QUAD -> "Quads"
        VolumeType.HAMSTRING -> "Hamstrings"
        VolumeType.GLUTE -> "Glutes"
        VolumeType.POSTERIOR_DELTOID -> "Posterior Deltoids"
        VolumeType.LATERAL_DELTOID -> "Lateral Deltoids"
        VolumeType.ANTERIOR_DELTOID -> "Anterior Deltoids"
        VolumeType.TRICEP -> "Triceps"
        VolumeType.BICEP -> "Biceps"
        VolumeType.TRAP -> "Traps"
        VolumeType.CALF -> "Calves"
        VolumeType.FOREARM -> "Forearms"
        VolumeType.AB -> "Abs"
        VolumeType.LOWER_BACK -> "Lower Back"
    }
}

fun VolumeTypeImpactSelection.displayName(): String {
    return when (this) {
        VolumeTypeImpactSelection.PRIMARY -> "Primary"
        VolumeTypeImpactSelection.SECONDARY -> "Secondary"
        VolumeTypeImpactSelection.COMBINED -> "All"
    }
}

fun List<VolumeType>.displayNames() =
    fastMap { it.displayName() }.sorted()

fun List<ProgressionScheme>.sortedByDisplayName() =
    ProgressionScheme.entries.sortedBy { it.displayName() }

fun ProgressionScheme.displayName(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION -> "Double Progression"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "Wave Loading"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "Dynamic Double Progression"
        ProgressionScheme.LINEAR_PROGRESSION -> "Linear Progression"
    }
}

fun ProgressionScheme.shortDisplayName(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION -> "DP"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "WL"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "DDP"
        ProgressionScheme.LINEAR_PROGRESSION -> "LP"
    }
}

fun ProgressionScheme.rpeLabel(): String =
    when (this) {
        ProgressionScheme.DOUBLE_PROGRESSION -> "RPE"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "Top Set RPE"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "RPE"
        ProgressionScheme.LINEAR_PROGRESSION -> "Max RPE"
    }
