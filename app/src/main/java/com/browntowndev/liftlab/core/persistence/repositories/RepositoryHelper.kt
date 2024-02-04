package com.browntowndev.liftlab.core.persistence.repositories

import android.content.Context
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RepositoryHelper(context: Context): KoinComponent {
    private val customLiftSetMapper: CustomLiftSetMapper by inject()
    private val workoutLiftMapper: WorkoutLiftMapper by inject()
    private val workoutMapper: WorkoutMapper by inject()
    private val programMapper: ProgramMapper by inject()
    private val setResultMapper: SetResultMapper by inject()
    private val workoutLogEntryMapper: WorkoutLogEntryMapper by inject()
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
        setResultsMapper = setResultMapper,
    )

    val workouts get() = WorkoutsRepository(
        workoutLiftsRepository = workoutLifts,
        customLiftSetsRepository = customLiftSets,
        workoutMapper = workoutMapper,
        workoutsDao = database.workoutsDao(),
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
        database.loggingDao(),
        workoutLogEntryMapper,
        setResultMapper,
    )

    val restTimer get() = RestTimerInProgressRepository(
        database.restTimerInProgressDao()
    )

    val liftMetricCharts get() = LiftMetricChartRepository(
        database.liftMetricChartsDao()
    )

    val volumemetricCharts get() = VolumeMetricChartRepository(
        database.volumeMetricChartsDao()
    )
}