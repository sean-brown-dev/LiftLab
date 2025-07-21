package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.persistence.room.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.room.entities.SetLogEntryRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.LiftMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.LiftsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.WorkoutLogRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.PreviousSetResultsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.ProgramsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.RestTimerInProgressRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.SyncMetadataRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.VolumeMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.persistence.room.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.workers.LiftLabDatabaseWorker
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
