package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.*
import com.browntowndev.liftlab.core.persistence.repositories.firebase.SyncMetadataRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { LiftLabDatabase.getInstance(get()) }
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
    single { get<LiftLabDatabase>().syncDao() }

    // Repositories
    single { ProgramsRepository(get(), get()) }
    single { WorkoutLiftsRepository(get(), get()) }
    single {
        WorkoutsRepository(
            workoutsDao = get(),
            workoutMapper = get(),
            programsRepository = get(),
            workoutLiftsRepository = get(),
            customLiftSetsRepository = get()
        )
    }
    single { PreviousSetResultsRepository(get(), get()) }
    single { LiftsRepository(get()) }
    single { CustomLiftSetsRepository(get(), get()) }
    single { WorkoutInProgressRepository(get(), get()) }
    single { HistoricalWorkoutNamesRepository(get()) }
    single {
        LoggingRepository(
            workoutLogEntryDao = get(),
            setLogEntryDao = get<LiftLabDatabase>().setLogEntryDao(),
            workoutLogEntryMapper = get(),
            setResultMapper = get()
        )
    }
    single { RestTimerInProgressRepository(get()) }
    single { LiftMetricChartsRepository(get()) }
    single { VolumeMetricChartsRepository(get()) }
    single { SyncMetadataRepository(get()) }

}
