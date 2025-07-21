package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.SetLogEntry

interface SetLogEntryRepository: Repository<SetLogEntry, Long> {
    suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    )

    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecord>
}