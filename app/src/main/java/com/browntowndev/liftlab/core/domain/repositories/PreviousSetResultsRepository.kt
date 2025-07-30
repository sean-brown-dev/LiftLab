package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import kotlinx.coroutines.flow.Flow

interface PreviousSetResultsRepository : Repository<SetResult, Long> {
    fun getByWorkoutIdExcludingGivenMesoAndMicroFlow(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int
    ): Flow<List<SetResult>>

    fun getForWorkoutFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetResult>>

    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult>

    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int, microCycle: Int,
        liftIds: List<Long>
    ): List<PersonalRecord>

    suspend fun deleteAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDeleteInstead: List<Long>,
    )

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int)
}
