package com.browntowndev.liftlab.core.persistence.repositories

import android.content.Context
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RepositoryHelper(context: Context): KoinComponent {
    private val customLiftSetMapper: CustomLiftSetMapper by inject()
    private val workoutLiftMapper: WorkoutLiftMapper by inject()
    private val workoutMapper: WorkoutMapper by inject()
    private val programMapper: ProgramMapper by inject()
    private val database: LiftLabDatabase = LiftLabDatabase.getInstance(context)

    val programs get() = ProgramsRepository(
        programsDao = database.programsDao(),
        programMapper = programMapper
    )

    val workoutLifts get() = WorkoutLiftsRepository(
        workoutLiftsDao = database.workoutLiftsDao(),
        workoutLiftMapper = workoutLiftMapper
    )

    val workouts get() = WorkoutsRepository(
        workoutsDao = database.workoutsDao(),
        workoutLiftsRepository = workoutLifts,
        customSetsDao = database.customSetsDao(),
        workoutMapper = workoutMapper,
        customLiftSetMapper = customLiftSetMapper,
    )

    val lifts get() = LiftsRepository(
        liftsDao = database.liftsDao()
    )

    val customLiftSets get() = CustomLiftSetsRepository(
        database.customSetsDao(),
        customLiftSetMapper = customLiftSetMapper
    )
}