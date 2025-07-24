package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName

interface HistoricalWorkoutNamesRepository : Repository<HistoricalWorkoutName, Long> {
    suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long?
}
