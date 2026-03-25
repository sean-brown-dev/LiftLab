package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class GetPersonalRecordsUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    suspend operator fun invoke(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>
    ): Map<Long, PersonalRecord> {
        return setLogEntryRepository.getPersonalRecordsForLifts(liftIds).associateBy { it.liftId }
    }
}