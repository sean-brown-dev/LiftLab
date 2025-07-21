package com.browntowndev.liftlab.core.persistence.room.dtos

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.persistence.room.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.persistence.room.entities.LiftEntity
import com.browntowndev.liftlab.core.persistence.room.entities.ProgramEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLiftEntity

typealias WorkoutLiftWithRelationships = ProgramWithRelationshipsDto.WorkoutWithRelationshipsDto.WorkoutLiftWithRelationshipsDto
typealias WorkoutWithRelationships = ProgramWithRelationshipsDto.WorkoutWithRelationshipsDto

data class ProgramWithRelationshipsDto(
    @Embedded
    val programEntity: ProgramEntity,
    @Relation(parentColumn = "program_id", entityColumn = "programId", entity = WorkoutEntity::class)
    val workouts: List<WorkoutWithRelationshipsDto> = emptyList()) {

    data class WorkoutWithRelationshipsDto(
        @Embedded
        val workoutEntity: WorkoutEntity,
        @Relation(parentColumn = "workout_id", entityColumn = "workoutId", entity = WorkoutLiftEntity::class)
        val lifts: List<WorkoutLiftWithRelationshipsDto> = emptyList()
    ) {
        data class WorkoutLiftWithRelationshipsDto(
            @Embedded
            val workoutLiftEntity: WorkoutLiftEntity,
            @Relation(parentColumn = "liftId", entityColumn =  "lift_id", entity = LiftEntity::class)
            val liftEntity: LiftEntity,
            @Relation(parentColumn = "workout_lift_id", entityColumn = "workoutLiftId", entity = CustomLiftSetEntity::class)
            val customLiftSetEntities: List<CustomLiftSetEntity> = emptyList(),
        )
    }
}