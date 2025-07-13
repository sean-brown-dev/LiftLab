package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.RepositoryHelper
import com.browntowndev.liftlab.core.persistence.sync.FirebaseSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module

val repositoryModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseSyncManager(get(), get(), LiftLabDatabase.getInstance(get())) }
    factory { RepositoryHelper(get()).lifts }
    factory { RepositoryHelper(get()).customLiftSets }
    factory { RepositoryHelper(get()).workoutLifts }
    factory { RepositoryHelper(get()).workouts }
    factory { RepositoryHelper(get()).programs }
    factory { RepositoryHelper(get()).previousSetResults }
    factory { RepositoryHelper(get()).workoutInProgress }
    factory { RepositoryHelper(get()).historicalWorkoutNames }
    factory { RepositoryHelper(get()).logging }
    factory { RepositoryHelper(get()).restTimer }
    factory { RepositoryHelper(get()).liftMetricCharts }
    factory { RepositoryHelper(get()).volumeMetricCharts }
    factory { RepositoryHelper(get()).sync }
    single { TransactionScope(LiftLabDatabase.getInstance(get())) }
}
