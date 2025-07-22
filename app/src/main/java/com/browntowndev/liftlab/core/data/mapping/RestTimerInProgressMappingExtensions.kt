package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.domain.models.RestTimerInProgress
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity

object RestTimerInProgressMappingExtensions {
    fun RestTimerInProgress.toEntity(): RestTimerInProgressEntity {
        return RestTimerInProgressEntity(
            id = id,
            timeStartedInMillis = this.timeStartedInMillis,
            restTime = this.restTime,
        )
    }

    fun RestTimerInProgressEntity.toDomainModel(): RestTimerInProgress {
        return RestTimerInProgress(
            id = id,
            timeStartedInMillis = this.timeStartedInMillis,
            restTime = this.restTime,
        )
    }
}