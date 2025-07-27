package com.browntowndev.liftlab.core.domain.useCase.workout

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class GetPersonalRecordsUseCase(
    private val setResultsRepository: PreviousSetResultsRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    suspend fun get(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>
    ): Map<Long, PersonalRecord> {
        val prevWorkoutPersonalRecords = setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(
            workoutId = workoutId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftIds = liftIds,
        )
        return setLogEntryRepository.getPersonalRecordsForLifts(liftIds)
            .associateBy { it.liftId }
            .toMutableMap()
            .apply {
                prevWorkoutPersonalRecords.fastForEach { prevWorkoutPr ->
                    get(prevWorkoutPr.liftId)?.let { allWorkoutsPr ->
                        if (allWorkoutsPr.personalRecord < prevWorkoutPr.personalRecord) {
                            put(prevWorkoutPr.liftId, prevWorkoutPr)
                        }
                    } ?: put(prevWorkoutPr.liftId, prevWorkoutPr)
                }
            }
    }
}