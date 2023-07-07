package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.RepositoryHelper
import org.koin.dsl.module

val repositoryModule = module {
    factory { RepositoryHelper(get()).lifts }
    factory { RepositoryHelper(get()).customLiftSets }
    factory { RepositoryHelper(get()).workoutLifts }
    factory { RepositoryHelper(get()).workouts }
    factory { RepositoryHelper(get()).programs }
    factory { RepositoryHelper(get()).previousSetResults }
    factory { RepositoryHelper(get()).workoutInProgress }
    factory { RepositoryHelper(get()).historicalWorkoutNames }
    factory { RepositoryHelper(get()).logging }
    single { TransactionScope(LiftLabDatabase.getInstance(get())) }
}
