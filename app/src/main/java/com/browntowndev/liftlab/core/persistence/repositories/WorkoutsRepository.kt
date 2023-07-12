package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.ProgressionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutsRepository(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
    private val progressionFactory: ProgressionFactory,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun get(id: Long): WorkoutDto {
        return workoutMapper.map(workoutsDao.get(id))
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getNextToPerform(
        programMetadata: ActiveProgramMetadataDto,
    ): LiveData<LoggingWorkoutDto> {
        return workoutsDao.getByMicrocyclePosition(programMetadata.currentMicrocyclePosition)
            .flatMapLatest { workout ->
                val previousSetResults =
                    previousSetResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicro(
                        workoutId = workout.workout.id,
                        mesoCycle = programMetadata.currentMesocycle,
                        microCycle = programMetadata.currentMicrocycle,
                    )

                val loggingWorkout = progressionFactory.calculate(
                    workout = workoutMapper.map(workout),
                    previousSetResults = previousSetResults,
                    programDeloadWeek = programMetadata.deloadWeek,
                    microCycle = programMetadata.currentMicrocycle,
                )

                val inProgressCompletedSets = previousSetResultsRepository.getForWorkout(
                    workoutId = workout.workout.id,
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle
                ).associateBy { result ->
                    "${result.liftId}-${result.setPosition}-${(result as? MyoRepSetResultDto)?.myoRepSetPosition}"
                }

                flowOf(
                    if ((inProgressCompletedSets.size) > 0) {
                        loggingWorkout.copy(
                            lifts = loggingWorkout.lifts.fastMap { workoutLift ->
                                workoutLift.copy(
                                    sets = workoutLift.sets.fastMap { set ->
                                        val completedSet = inProgressCompletedSets[
                                                "${workoutLift.liftId}-${set.setPosition}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}"
                                        ]

                                        if (completedSet != null) {
                                            when (set) {
                                                is LoggingStandardSetDto -> set.copy(
                                                    complete = true,
                                                    completedWeight = completedSet.weight,
                                                    completedReps = completedSet.reps,
                                                    completedRpe = completedSet.rpe,
                                                )

                                                is LoggingDropSetDto -> set.copy(
                                                    complete = true,
                                                    completedWeight = completedSet.weight,
                                                    completedReps = completedSet.reps,
                                                    completedRpe = completedSet.rpe,
                                                )

                                                is LoggingMyoRepSetDto -> set.copy(
                                                    complete = true,
                                                    completedWeight = completedSet.weight,
                                                    completedReps = completedSet.reps,
                                                    completedRpe = completedSet.rpe,
                                                )

                                                else -> throw Exception("${set::class.simpleName} is not defined.")
                                            }
                                        } else set
                                    }
                                )
                            }
                        )
                    } else loggingWorkout
                )
            }.asLiveData()
    }
}