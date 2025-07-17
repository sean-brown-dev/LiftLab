package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.*
import com.browntowndev.liftlab.core.persistence.repositories.firebase.SyncMetadataRepository
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
    single { get<LiftLabDatabase>().syncDao() }

    // Repositories
    single { ProgramsRepository(get(), get(), get(), get(named("FirestoreSyncScope"))) }
    single { WorkoutLiftsRepository(get(), get(), get(), get(named("FirestoreSyncScope"))) }
    single {
        WorkoutsRepository(
            workoutsDao = get(),
            workoutMapper = get(),
            programsRepository = get(),
            workoutLiftsRepository = get(),
            customLiftSetsRepository = get(),
            firestoreSyncManager = get(),
            syncScope = get(named("FirestoreSyncScope")),
        )
    }
    single { PreviousSetResultsRepository(get(), get(), get(), get(named("FirestoreSyncScope"))) }
    single { LiftsRepository(get(), get(), get(named("FirestoreSyncScope"))) }
    single { CustomLiftSetsRepository(get(), get(), get(), get(named("FirestoreSyncScope"))) }
    single { WorkoutInProgressRepository(get(), get(), get(), get(named("FirestoreSyncScope"))) }
    single { HistoricalWorkoutNamesRepository(get(), get(), get(named("FirestoreSyncScope"))) }
    single {
        LoggingRepository(
            workoutLogEntryDao = get(),
            setLogEntryDao = get<LiftLabDatabase>().setLogEntryDao(),
            workoutLogEntryMapper = get(),
            setResultMapper = get(),
            firestoreSyncManager = get(),
            syncScope = get(named("FirestoreSyncScope"))
        )
    }
    single { RestTimerInProgressRepository(get(), get()) }
    single { LiftMetricChartsRepository(get(), get(), get(named("FirestoreSyncScope"))) }
    single { VolumeMetricChartsRepository(get(), get(), get(named("FirestoreSyncScope"))) }
    single { SyncMetadataRepository(get()) }

}
