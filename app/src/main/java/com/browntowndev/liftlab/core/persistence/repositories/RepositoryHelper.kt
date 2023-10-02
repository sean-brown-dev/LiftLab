package com.browntowndev.liftlab.core.persistence.repositories

import android.content.Context
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.entities.LoggingRepository
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.ProgressionFactory
import com.browntowndev.liftlab.core.progression.StandardProgressionFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RepositoryHelper(context: Context): KoinComponent {
    private val customLiftSetMapper: CustomLiftSetMapper by inject()
    private val workoutLiftMapper: WorkoutLiftMapper by inject()
    private val workoutMapper: WorkoutMapper by inject()
    private val programMapper: ProgramMapper by inject()
    private val setResultsMapper: SetResultMapper by inject()
    private val progressionFactory: ProgressionFactory by inject<StandardProgressionFactory>()
    private val database: LiftLabDatabase = LiftLabDatabase.getInstance(context)

    val programs get() = ProgramsRepository(
        programsDao = database.programsDao(),
        programMapper = programMapper
    )

    val workoutLifts get() = WorkoutLiftsRepository(
        workoutLiftsDao = database.workoutLiftsDao(),
        workoutLiftMapper = workoutLiftMapper
    )

    val previousSetResults get() = PreviousSetResultsRepository(
        previousSetResultDao = database.previousSetResultsDao(),
        setResultsMapper = setResultsMapper,
    )

    val workouts get() = WorkoutsRepository(
        workoutLiftsRepository = workoutLifts,
        customLiftSetsRepository = customLiftSets,
        previousSetResultsRepository = previousSetResults,
        workoutMapper = workoutMapper,
        workoutsDao = database.workoutsDao(),
        progressionFactory = progressionFactory,
    )

    val lifts get() = LiftsRepository(
        liftsDao = database.liftsDao()
    )

    val customLiftSets get() = CustomLiftSetsRepository(
        database.customSetsDao(),
        customLiftSetMapper = customLiftSetMapper
    )

    val workoutInProgress get() = WorkoutInProgressRepository(
        database.workoutInProgressDao(),
        previousSetResultsRepository = previousSetResults,
    )

    val historicalWorkoutNames get() = HistoricalWorkoutNamesRepository(
        database.historicalWorkoutNamesDao()
    )

    val logging get() = LoggingRepository(
        database.loggingDao()
    )

    val restTimer get() = RestTimerInProgressRepository(
        database.restTimerInProgressDao()
    )
}