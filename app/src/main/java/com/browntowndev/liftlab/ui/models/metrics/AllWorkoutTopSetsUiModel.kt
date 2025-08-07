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

    data class WorkoutTopSetsUiModel(
        private val recordsByLift: Map<LiftId, TopSetUiModel>,
        val personalRecordCount: Int,
    ) {
        val liftIds: Set<LiftId> get() = recordsByLift.keys
        val topSets: List<TopSetUiModel> get() = recordsByLift.values.toList()
        val size get() = recordsByLift.size

        operator fun get(id: LiftId): TopSetUiModel? = recordsByLift[id]

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
