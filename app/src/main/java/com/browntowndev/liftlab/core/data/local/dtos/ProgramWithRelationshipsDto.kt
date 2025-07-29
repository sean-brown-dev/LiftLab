package com.browntowndev.liftlab.core.data.local.dtos

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.views.program.LiveCustomLiftSetView
import com.browntowndev.liftlab.core.data.local.views.LiveLiftView
import com.browntowndev.liftlab.core.data.local.views.program.LiveWorkoutLiftView
import com.browntowndev.liftlab.core.data.local.views.program.LiveWorkoutView

typealias WorkoutLiftWithRelationships = ProgramWithRelationshipsDto.WorkoutWithRelationshipsDto.WorkoutLiftWithRelationshipsDto
typealias WorkoutWithRelationships = ProgramWithRelationshipsDto.WorkoutWithRelationshipsDto

data class ProgramWithRelationshipsDto(
    @Embedded
    val programEntity: ProgramEntity,
    @Relation(parentColumn = "program_id", entityColumn = "programId", entity = LiveWorkoutView::class)
    val workouts: List<WorkoutWithRelationshipsDto> = emptyList()) {

    data class WorkoutWithRelationshipsDto(
        @Embedded
        val workoutEntity: WorkoutEntity,
        @Relation(parentColumn = "workout_id", entityColumn = "workoutId", entity = LiveWorkoutLiftView::class)
        val lifts: List<WorkoutLiftWithRelationshipsDto> = emptyList()
    ) {
        data class WorkoutLiftWithRelationshipsDto(
            @Embedded
            val workoutLiftEntity: WorkoutLiftEntity,
            @Relation(parentColumn = "liftId", entityColumn =  "lift_id", entity = LiveLiftView::class)
            val liftEntity: LiftEntity,
            @Relation(parentColumn = "workout_lift_id", entityColumn = "workoutLiftId", entity = LiveCustomLiftSetView::class)
            val customLiftSetEntities: List<CustomLiftSetEntity> = emptyList(),
        )
    }
}