package com.browntowndev.liftlab.ui.models.metrics

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry

typealias LiftId = Long
typealias WorkoutLogId = Long

data class AllWorkoutTopSetsUiModel(
    private val topSetsByWorkout: Map<WorkoutLogId, WorkoutTopSetsUiModel>
) {
    val size get() = topSetsByWorkout.size

    fun isEmpty() = topSetsByWorkout.isEmpty()

    operator fun get(workoutLogId: WorkoutLogId): WorkoutTopSetsUiModel? = topSetsByWorkout[workoutLogId]

    fun findTopSet(workoutId: WorkoutLogId, liftId: LiftId): WorkoutTopSetsUiModel.TopSetUiModel? {
        return topSetsByWorkout[workoutId]?.get(liftId)
    }

    data class WorkoutTopSetsUiModel(
        private val recordsByExercise: Map<LiftId, TopSetUiModel>
    ) {
        val liftIds: Set<LiftId> get() = recordsByExercise.keys

        val topSets: List<TopSetUiModel> get() = recordsByExercise.values.toList()

        val size get() = recordsByExercise.size

        val personalRecordCount get() = recordsByExercise.count { it.value.setLog.isPersonalRecord }

        operator fun get(id: LiftId): TopSetUiModel? = recordsByExercise[id]

        data class TopSetUiModel(
            val setCount: Int,
            val setLog: SetLogEntry
        ) {
            val isPersonalRecord get() = setLog.isPersonalRecord
            val reps get() = setLog.reps
            val weight get() = setLog.weight
            val rpe get() = setLog.rpe
            val liftName get() = setLog.liftName
        }
    }
}
