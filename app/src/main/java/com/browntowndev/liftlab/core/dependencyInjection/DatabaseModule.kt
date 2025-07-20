package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.*
import com.browntowndev.liftlab.core.persistence.repositories.firestore.SyncMetadataRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
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
    single { WorkoutLiftsRepository(get(), get(), get()) }
    single {
        WorkoutsRepository(
            workoutsDao = get(),
            workoutMapper = get(),
            programsRepository = get(),
            workoutLiftsRepository = get(),
            customLiftSetsRepository = get(),
            firestoreSyncManager = get(),
        )
    }
    single { PreviousSetResultsRepository(get(), get(), get()) }
    single { LiftsRepository(get(), get()) }
    single { CustomLiftSetsRepository(get(), get(), get(), get()) }
    single { WorkoutInProgressRepository(get(), get(), get()) }
    single { HistoricalWorkoutNamesRepository(get(), get()) }
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
    single { VolumeMetricChartsRepository(get(), get()) }
    singleOf(::SyncMetadataRepository)
    singleOf(::RestTimerInProgressRepository)
}
