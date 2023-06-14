package com.browntowndev.liftlab.core.persistence.dtos.queryable

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift

typealias WorkoutLiftWithRelationships = ProgramWithRelationships.WorkoutWithRelationships.WorkoutLiftWithRelationships
typealias WorkoutWithRelationships = ProgramWithRelationships.WorkoutWithRelationships

data class ProgramWithRelationships(
    @Embedded
    val program: Program,
    @Relation(parentColumn = "program_id", entityColumn = "programId", entity = Workout::class)
    val workouts: List<WorkoutWithRelationships> = emptyList()) {

    data class WorkoutWithRelationships(
        @Embedded
        val workout: Workout,
        @Relation(parentColumn = "workout_id", entityColumn = "workoutId", entity = WorkoutLift::class)
        val lifts: List<WorkoutLiftWithRelationships> = emptyList()
    ) {
        data class WorkoutLiftWithRelationships(
            @Embedded
            val workoutLift: WorkoutLift,
            @Relation(parentColumn = "liftId", entityColumn =  "lift_id", entity = Lift::class)
            val lift: Lift,
            @Relation(parentColumn = "workout_lift_id", entityColumn = "workoutLiftId", entity = CustomLiftSet::class)
            val customLiftSets: List<CustomLiftSet> = emptyList(),
        )
    }
}