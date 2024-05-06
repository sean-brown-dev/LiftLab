package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutsRepository(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun insert(workout: WorkoutDto): Long {
        return workoutsDao.insert(workout = workoutMapper.map(workout))
    }

    suspend fun delete(workout: WorkoutDto) {
        workoutsDao.delete(workoutMapper.map(workout))
    }

    suspend fun updateMany(workouts: List<WorkoutDto>) {
        workoutsDao.updateMany(workouts.map { workoutMapper.map(it) })
    }

    suspend fun update(workout: WorkoutDto) {
        val updWorkout = workoutMapper.map(workout)
        val updSets = workout.lifts
            .filterIsInstance<CustomWorkoutLiftDto>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        workoutLiftsRepository.updateMany(workout.lifts)
        customLiftSetsRepository.updateMany(updSets)
    }

    suspend fun get(workoutId: Long): WorkoutDto? {
        return workoutsDao.get(workoutId)?.let {
             workoutMapper.map(it)
        }
    }

    suspend fun setAllWorkoutLiftDeloadWeeksToNull(workoutId: Long, programDeloadWeek: Int) {
        val updatedLifts = get(workoutId)?.lifts
            ?.filterIsInstance<StandardWorkoutLiftDto>()
            ?.filter { lift -> lift.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION }
            ?.map { lift ->
                val stepSizeOptions = Utils.getPossibleStepSizes(
                    repRangeTop = lift.repRangeTop,
                    repRangeBottom = lift.repRangeBottom,
                    stepCount = programDeloadWeek - 2
                )

                lift.copy(
                    stepSize = if(stepSizeOptions.contains(lift.stepSize)) {
                        lift.stepSize
                    } else {
                        stepSizeOptions.firstOrNull()
                    }
                )
            }

        if (updatedLifts?.isNotEmpty() == true) {
            workoutLiftsRepository.updateMany(updatedLifts)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getByMicrocyclePosition(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<WorkoutDto?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).flatMapLatest { workoutEntity ->
            flowOf(
                workoutEntity?.let { workoutMapper.map(it) }
            )
        }
    }
}