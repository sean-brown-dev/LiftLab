package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.repositories.SetLogEntryRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.LiftMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.LiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLogRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.PreviousSetResultsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.RestTimerInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.SyncMetadataRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.VolumeMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.data.workers.LiftLabDatabaseWorker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    single {
        LiftLabDatabase.getInstance(
            context = get(),
            populateInitialData = LiftLabDatabase.PopulateInitialDataCallback(
                onOpen = {
                    LiftLabDatabaseWorker.startDatabaseInitialization(get())
                }
            )
        )
    }
    single { TransactionScope(get()) }

    // DAOs
    single { get<LiftLabDatabase>().programsDao() }
    single { get<LiftLabDatabase>().workoutLiftsDao() }
    single { get<LiftLabDatabase>().workoutsDao() }
    single { get<LiftLabDatabase>().previousSetResultsDao() }
    single { get<LiftLabDatabase>().liftsDao() }
    single { get<LiftLabDatabase>().customSetsDao() }
    single { get<LiftLabDatabase>().workoutInProgressDao() }
    single { get<LiftLabDatabase>().historicalWorkoutNamesDao() }
    single { get<LiftLabDatabase>().workoutLogEntryDao() }
    single { get<LiftLabDatabase>().restTimerInProgressDao() }
    single { get<LiftLabDatabase>().liftMetricChartsDao() }
    single { get<LiftLabDatabase>().volumeMetricChartsDao() }
    single { get<LiftLabDatabase>().setLogEntryDao() }
    single { get<LiftLabDatabase>().syncDao() }

    // Repositories
    singleOf(::ProgramsRepositoryImpl)
    singleOf(::WorkoutLiftsRepositoryImpl)
    singleOf(::WorkoutsRepositoryImpl)
    singleOf(::PreviousSetResultsRepositoryImpl)
    singleOf(::LiftsRepositoryImpl)
    singleOf(::CustomLiftSetsRepositoryImpl)
    singleOf(::WorkoutInProgressRepositoryImpl)
    singleOf(::HistoricalWorkoutNamesRepositoryImpl)
    singleOf(::WorkoutLogRepositoryImpl)
    singleOf(::LiftMetricChartsRepositoryImpl)
    singleOf(::VolumeMetricChartsRepositoryImpl)
    singleOf(::RestTimerInProgressRepositoryImpl)
    singleOf(::SyncMetadataRepositoryImpl)
    singleOf(::WorkoutLogRepositoryImpl)
    singleOf(::SetLogEntryRepositoryImpl)
}
