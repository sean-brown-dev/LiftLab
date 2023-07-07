package com.browntowndev.liftlab.core.persistence.repositories


import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress

class WorkoutInProgressRepository(
    private val workoutInProgressDao: WorkoutInProgressDao
): Repository {
    suspend fun insert(workoutInProgress: WorkoutInProgressDto) {
        workoutInProgressDao.insert(
            workoutInProgress = WorkoutInProgress(
                workoutId = workoutInProgress.workoutId,
                startTime = workoutInProgress.startTime,
            )
        )
    }

    suspend fun delete(workoutId: Long) {
        workoutInProgressDao.delete(workoutId)
    }
}