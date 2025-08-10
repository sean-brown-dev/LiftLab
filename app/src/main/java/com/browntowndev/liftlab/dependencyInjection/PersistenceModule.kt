package com.browntowndev.liftlab.dependencyInjection

import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.data.billing.BillingManager
import com.browntowndev.liftlab.core.data.billing.BillingManagerImpl
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.repositories.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.LiftMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.LiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.LiveWorkoutCompletedSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.RestTimerInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.SetLogEntryRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.SettingsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.SyncMetadataRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.VolumeMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLogRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.data.workers.LiftLabDatabaseWorker
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val persistenceModule = module {
    single {
        LiftLabDatabase.getInstance(
            context = get(),
            populateInitialData = LiftLabDatabase.PopulateInitialDataCallback(
                onCreate = {
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
    single { get<LiftLabDatabase>().liveWorkoutCompletedSetsDao() }
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
    single<ProgramsRepository> {
        ProgramsRepositoryImpl(
            programsDao = get(),
            restTimerInProgressDao = get(),
            syncScheduler = get(),
        )
    }
    single<WorkoutLiftsRepository> {
        WorkoutLiftsRepositoryImpl(
            workoutLiftsDao = get(),
            syncScheduler = get(),
        )
    }
    single<WorkoutsRepository> {
        WorkoutsRepositoryImpl(
            workoutLiftsDao = get(),
            customSetsDao = get(),
            programsRepository = get(),
            workoutsDao = get(),
            syncScheduler = get(),
        )
    }
    single<LiveWorkoutCompletedSetsRepository> {
        LiveWorkoutCompletedSetsRepositoryImpl(
            liveWorkoutCompletedSetsDao = get(),
            syncScheduler = get(),
        )
    }
    single<LiftsRepository> {
        LiftsRepositoryImpl(
            liftsDao = get(),
            syncScheduler = get(),
        )
    }
    single<CustomLiftSetsRepository> {
        CustomLiftSetsRepositoryImpl(
            customSetsDao = get(),
            workoutLiftsDao = get(),
            syncScheduler = get(),
        )
    }
    single<WorkoutInProgressRepository> {
        WorkoutInProgressRepositoryImpl(
            workoutInProgressDao = get(),
            syncScheduler = get(),
        )
    }
    single<HistoricalWorkoutNamesRepository> {
        HistoricalWorkoutNamesRepositoryImpl(
            historicalWorkoutNamesDao = get(),
            syncScheduler = get(),
        )
    }
    single<WorkoutLogRepository> {
        WorkoutLogRepositoryImpl(
            workoutLogEntryDao = get(),
            setLogEntryDao = get(),
            syncScheduler = get(),
        )
    }
    single<LiftMetricChartsRepository> {
        LiftMetricChartsRepositoryImpl(
            liftMetricChartsDao = get(),
            syncScheduler = get(),
        )
    }
    single<VolumeMetricChartsRepository> {
        VolumeMetricChartsRepositoryImpl(
            volumeMetricChartsDao = get(),
            syncScheduler = get(),
        )
    }
    single<RestTimerInProgressRepository> {
        RestTimerInProgressRepositoryImpl(
            restTimerInProgressDao = get()
        )
    }
    single<SyncMetadataRepository> {
        SyncMetadataRepositoryImpl(
            dao = get(),
        )
    }
    single<SetLogEntryRepository> {
        SetLogEntryRepositoryImpl(
            setLogEntryDao = get(),
            syncScheduler = get(),
        )
    }

    single<SettingsRepository> { SettingsRepositoryImpl() }

    single { BillingClient.newBuilder(get()) }
    single<BillingManager> {
        BillingManagerImpl(
            billingClientBuilder = get(),
            dispatchers = get(),
        )
    }

    workerOf(::LiftLabDatabaseWorker)
}
