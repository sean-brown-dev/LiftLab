package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.data.dtos.ProgramDto

data class LabState(
    val program: ProgramDto? = null,
    val isEditingProgramName: Boolean = false,
    val isDeletingProgram: Boolean = false,
    val workoutOfEditNameModal: ProgramDto.WorkoutDto? = null,
    val workoutToDelete: ProgramDto.WorkoutDto? = null,
    val isReordering: Boolean = false,
) {
    val originalProgramName: String? = program?.name
    val originalWorkoutNameOfActiveRename: String? = workoutOfEditNameModal?.name
    val workoutCount
        get() = program?.workouts?.count() ?: 0
}