package com.browntowndev.liftlab.core.data.dtos

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.core.data.entities.LiftSet
import com.browntowndev.liftlab.core.data.entities.Program
import com.browntowndev.liftlab.core.data.entities.Workout
import com.browntowndev.liftlab.core.data.entities.WorkoutLift

data class ProgramDTO(
    @Embedded
    val program: Program,
    @Relation(parentColumn = "program_id", entityColumn = "programId", entity = Workout::class)
    val workouts: List<WorkoutDTO>) {

    data class WorkoutDTO(
        @Embedded
        val workout: Workout,
        @Relation(parentColumn = "workout_id", entityColumn = "workoutId", entity = WorkoutLift::class)
        val lifts: List<WorkoutLiftDTO>) {

        data class WorkoutLiftDTO(
            @Embedded
            val workoutLift: WorkoutLift,
            @Relation(parentColumn = "workout_lift_id", entityColumn =  "workoutLiftId", entity = LiftSet::class)
            val sets: List<LiftSetDTO>,
            @Relation(parentColumn = "liftId", entityColumn =  "lift_id", entity = Lift::class)
            val lift: Lift
        ) {
            data class LiftSetDTO(
                @Embedded
                val liftSet: LiftSet
            )
        }
    }
}