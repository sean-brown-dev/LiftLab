package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.data.dtos.ProgramDto

data class LabState(
    val programs: List<ProgramDto> = listOf(),
    val workoutOfEditNameModal: ProgramDto.WorkoutDto? = null,
    val programOfEditNameModal: ProgramDto? = null,
    val programToDelete: ProgramDto? = null,
    val workoutToDelete: ProgramDto.WorkoutDto? = null,
) {
    val originalProgramNameOfActiveRename: String? = programOfEditNameModal?.name
    val originalWorkoutNameOfActiveRename: String? = workoutOfEditNameModal?.name
}