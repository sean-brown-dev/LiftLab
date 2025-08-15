package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import kotlinx.coroutines.flow.Flow

interface SetLogEntryRepository: Repository<SetLogEntry, Long> {
    suspend fun insertFromLiveWorkoutCompletedSets(
        workoutLogEntryId: Long,
        workoutId: Long,
        excludeFromCopy: List<Long>
    )

    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecord>

    fun getLatestForWorkout(workoutId: Long, includeDeload: Boolean): Flow<List<SetLogEntry>>

    fun getForSpecificWorkoutCompletionFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetLogEntry>>

    suspend fun getAllCompletionDataForWorkout(workoutId: Long): List<SetLogEntry>
}