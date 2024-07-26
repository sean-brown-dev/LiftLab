package com.browntowndev.liftlab.core.persistence.repositories


import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import java.util.Date

class WorkoutInProgressRepository(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
): Repository {
    suspend fun insert(workoutInProgress: WorkoutInProgressDto) {
        // Delete any that exist. Just calling delete because selecting then checking for null
        // still results in 1 SQL query anyway
        delete()

        workoutInProgressDao.insert(
            workoutInProgress = WorkoutInProgress(
                workoutId = workoutInProgress.workoutId,
                startTime = workoutInProgress.startTime,
            )
        )
    }

    suspend fun delete() {
        workoutInProgressDao.delete()
    }

    suspend fun getWithoutCompletedSets(): WorkoutInProgressDto? {
        val inProgressEntity = workoutInProgressDao.get()

        return if (inProgressEntity != null) {
            return WorkoutInProgressDto(
                workoutId = inProgressEntity.workoutId,
                startTime = inProgressEntity.startTime,
                completedSets = listOf()
            )
        } else null
    }

    suspend fun get(mesoCycle: Int, microCycle: Int): WorkoutInProgressDto? {
        val inProgressWorkout =  workoutInProgressDao.get()
        return if (inProgressWorkout != null) {

            val completedSets = previousSetResultsRepository.getForWorkout(
                workoutId = inProgressWorkout.workoutId,
                mesoCycle = mesoCycle,
                microCycle = microCycle,
            )

            WorkoutInProgressDto(
                workoutId = inProgressWorkout.workoutId,
                startTime = inProgressWorkout.startTime,
                completedSets = completedSets,
            )
        } else null
    }
}