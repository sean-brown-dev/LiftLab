package com.browntowndev.liftlab.ui.models.workout

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.appendSuperscript
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.core.domain.enums.getVolumeTypes
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import java.util.Locale.US
import kotlin.collections.getOrDefault


private fun getVolumeTypeMapForGenericWorkoutLifts(lifts: List<WorkoutLiftUiModel>, impact: VolumeTypeImpact):  HashMap<String, Pair<Float, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Float, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }
        val secondaryVolumeTypes = lift.liftSecondaryVolumeTypes?.getVolumeTypes()?.toHashSet()

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Float, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = (lift as? CustomWorkoutLift)?.customLiftSets?.any { it is MyoRepSet } ?: false
            var newTotalVolume: Float = if(secondaryVolumeTypes?.contains(volumeType) == true)
                lift.setCount / 2f
            else
                lift.setCount.toFloat()

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeMapForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftUiModel>, impact: VolumeTypeImpact):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Int, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = lift.sets.any { it is LoggingMyoRepSet }
            var newTotalVolume: Int = lift.setCount

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeLabelsForGenericWorkoutLifts(lifts: List<WorkoutLiftUiModel>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForGenericWorkoutLifts(lifts, impact).map { (volumeType, totalVolume) ->
        val volume = if (totalVolume.first % 1.0 == 0.0) {
            String.format(US, "%.0f", totalVolume.first) // No decimals if the value is a whole number
        } else {
            String.format(US, "%.1f", totalVolume.first) // One decimal if there is a non-zero decimal part
        }

        val plainVolumeString = "$volumeType: $volume"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun WorkoutUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(this.lifts, impact)
}

private fun getVolumeTypeLabelsForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLift>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForLoggingWorkoutLifts(lifts, impact).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun LoggingWorkoutUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForLoggingWorkoutLifts(this.lifts, impact)
}

fun ProgramUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(
        lifts = this.workouts.flatMap { workout ->
            workout.lifts
        },
        impact = impact,
    )
}