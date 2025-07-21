package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.standard.*
import com.browntowndev.liftlab.core.domain.repositories.sync.SyncMetadataRepository
import org.koin.core.module.dsl.singleOf
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
    single { get<LiftLabDatabase>().setLogEntryDao() }
    single { get<LiftLabDatabase>().syncDao() }

    // Repositories
    single { ProgramsRepository(get(), get(), get(), get()) }
    single { WorkoutLiftsRepositoryImpl(get(), get(), get()) }
    single {
        WorkoutsRepositoryImpl(
            workoutsDao = get(),
            workoutMapper = get(),
            programsRepository = get(),
            workoutLiftsRepositoryImpl = get(),
            customLiftSetsRepositoryImpl = get(),
            firestoreSyncManager = get(),
        )
    }
    single { PreviousSetResultsRepository(get(), get(), get()) }
    single { LiftsRepository(get(), get()) }
    single { CustomLiftSetsRepositoryImpl(get(), get(), get(), get()) }
    single { WorkoutInProgressRepositoryImpl(get(), get(), get()) }
    single { HistoricalWorkoutNamesRepositoryImpl(get(), get()) }
    single {
        LoggingRepository(
            workoutLogEntryDao = get(),
            setLogEntryDao = get(),
            workoutLogEntryMapper = get(),
            setResultMapper = get(),
            firestoreSyncManager = get(),
        )
    }
    single { LiftMetricChartsRepository(get(), get()) }
    single { VolumeMetricChartsRepositoryImpl(get(), get()) }
    singleOf(::SyncMetadataRepository)
    singleOf(::RestTimerInProgressRepository)
}
