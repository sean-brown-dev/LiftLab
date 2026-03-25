package com.browntowndev.liftlab.dependencyInjection

import com.android.billingclient.api.BillingClient
import com.browntowndev.liftlab.core.data.billing.BillingManager
import com.browntowndev.liftlab.core.data.billing.BillingManagerImpl
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.maintenance.DatabaseMaintenance
import com.browntowndev.liftlab.core.data.local.maintenance.RoomDatabaseMaintenance
import com.browntowndev.liftlab.core.data.local.repositories.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.LiftMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.LiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.LiveWorkoutCompletedSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.ProgramsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.RestTimerInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.SetLogEntryRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.SettingsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.SyncMetadataRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.VolumeMetricChartsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.WorkoutLogRepositoryImpl
import com.browntowndev.liftlab.core.data.local.repositories.WorkoutsRepositoryImpl
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
import com.browntowndev.liftlab.ui.infrastructure.WalCaretaker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val persistenceModule = module {
    // Database
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
    single<DatabaseMaintenance> { RoomDatabaseMaintenance(get()) }
    single<WalCaretaker> { WalCaretaker(get(), get(AppScope)) }

    // Repositories (resolve DAOs directly from the DB, no DAO singletons exported)
    single<ProgramsRepository> {
        ProgramsRepositoryImpl(
            programsDao = get<LiftLabDatabase>().programsDao(),
            workoutsDao = get<LiftLabDatabase>().workoutsDao(),
            workoutLiftsDao = get<LiftLabDatabase>().workoutLiftsDao(),
            customSetsDao = get<LiftLabDatabase>().customSetsDao(),
            liveWorkoutCompletedSetsDao = get<LiftLabDatabase>().liveWorkoutCompletedSetsDao(),
            workoutInProgressDao = get<LiftLabDatabase>().workoutInProgressDao(),
            syncScheduler = get(),
            transactionScope = get(),
        )
    }
    single<WorkoutLiftsRepository> {
        WorkoutLiftsRepositoryImpl(
            workoutLiftsDao = get<LiftLabDatabase>().workoutLiftsDao(),
            syncScheduler = get(),
        )
    }
    single<WorkoutsRepository> {
        WorkoutsRepositoryImpl(
            workoutsDao = get<LiftLabDatabase>().workoutsDao(),
        )
    }
    single<LiveWorkoutCompletedSetsRepository> {
        LiveWorkoutCompletedSetsRepositoryImpl(
            liveWorkoutCompletedSetsDao = get<LiftLabDatabase>().liveWorkoutCompletedSetsDao(),
            syncScheduler = get(),
        )
    }
    single<LiftsRepository> {
        LiftsRepositoryImpl(
            liftsDao = get<LiftLabDatabase>().liftsDao(),
            syncScheduler = get(),
        )
    }
    single<CustomLiftSetsRepository> {
        CustomLiftSetsRepositoryImpl(
            customSetsDao = get<LiftLabDatabase>().customSetsDao(),
        )
    }
    single<WorkoutInProgressRepository> {
        WorkoutInProgressRepositoryImpl(
            workoutInProgressDao = get<LiftLabDatabase>().workoutInProgressDao(),
            syncScheduler = get(),
        )
    }
    single<HistoricalWorkoutNamesRepository> {
        HistoricalWorkoutNamesRepositoryImpl(
            historicalWorkoutNamesDao = get<LiftLabDatabase>().historicalWorkoutNamesDao(),
            syncScheduler = get(),
        )
    }
    single<WorkoutLogRepository> {
        WorkoutLogRepositoryImpl(
            workoutLogEntryDao = get<LiftLabDatabase>().workoutLogEntriesDao(),
            setLogEntryDao = get<LiftLabDatabase>().setLogEntriesDao(),
            syncScheduler = get(),
        )
    }
    single<LiftMetricChartsRepository> {
        LiftMetricChartsRepositoryImpl(
            liftMetricChartsDao = get<LiftLabDatabase>().liftMetricChartsDao(),
            syncScheduler = get(),
        )
    }
    single<VolumeMetricChartsRepository> {
        VolumeMetricChartsRepositoryImpl(
            volumeMetricChartsDao = get<LiftLabDatabase>().volumeMetricChartsDao(),
            syncScheduler = get(),
        )
    }
    single<RestTimerInProgressRepository> {
        RestTimerInProgressRepositoryImpl(
            restTimerInProgressDao = get<LiftLabDatabase>().restTimerInProgressDao()
        )
    }
    single<SyncMetadataRepository> {
        SyncMetadataRepositoryImpl(
            dao = get<LiftLabDatabase>().syncDao(),
        )
    }
    single<SetLogEntryRepository> {
        SetLogEntryRepositoryImpl(
            setLogEntryDao = get<LiftLabDatabase>().setLogEntriesDao(),
            syncScheduler = get(),
        )
    }
    single<SettingsRepository> { SettingsRepositoryImpl() }

    // Billing
    single { BillingClient.newBuilder(get()) }
    single<BillingManager> {
        BillingManagerImpl(
            billingClientBuilder = get(),
            dispatchers = get(),
        )
    }

    // Workers
    workerOf(::LiftLabDatabaseWorker)
}
