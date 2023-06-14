package com.browntowndev.liftlab.core.persistence.repositories

import android.content.Context
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RepositoryHelper(context: Context): KoinComponent {
    private val workoutMapper: WorkoutMapper by inject()
    private val programMapper: ProgramMapper by inject()
    private val database: LiftLabDatabase = LiftLabDatabase.getInstance(context)

    val lifts get() = LiftsRepository(liftsDao = database.liftsDao())
    val workouts get() = WorkoutsRepository(workoutsDao = database.workoutsDao(), workoutMapper = workoutMapper)
    val programs get() = ProgramsRepository(programsDao = database.programsDao(), programMapper = programMapper)
}