package com.browntowndev.liftlab.dependencyInjection

import androidx.compose.ui.util.fastMap
import androidx.work.WorkManager
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.sync.FirestoreClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.data.sync.FirestoreClientImpl
import com.browntowndev.liftlab.core.data.sync.FirestoreRemoteDataClient
import com.browntowndev.liftlab.core.data.sync.RemoteDataClient
import com.browntowndev.liftlab.core.data.sync.SyncOrchestrator
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import com.browntowndev.liftlab.core.data.sync.SyncWorker
import com.browntowndev.liftlab.core.data.sync.WorkManagerSyncScheduler
import com.browntowndev.liftlab.core.data.sync.repositories.CustomSetsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.LiftMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.LiftsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.PreviousSetResultsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.ProgramSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.SetLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.VolumeMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.WorkoutInProgressSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.WorkoutLiftsSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.WorkoutLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.sync.repositories.WorkoutsSyncRepository
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val syncModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<FirestoreClient> { FirestoreClientImpl(get(), get()) }
    single { WorkManager.getInstance(get()) }
    single<SyncScheduler> {
        WorkManagerSyncScheduler(get())
    }

    single<RemoteSyncRepository>(named(RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION)) {
        CustomSetsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION)) {
        HistoricalWorkoutNamesSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION)) {
        LiftMetricChartsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIFTS_COLLECTION)) {
        LiftsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.PREVIOUS_SET_RESULTS_COLLECTION)) {
        PreviousSetResultsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.PROGRAMS_COLLECTION)) {
        ProgramSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION)) {
        SetLogEntriesSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION)) {
        VolumeMetricChartsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION)) {
        WorkoutInProgressSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LIFTS_COLLECTION)) {
        WorkoutLiftsSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LOG_ENTRIES_COLLECTION)) {
        WorkoutLogEntriesSyncRepository(
            get()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUTS_COLLECTION)) {
        WorkoutsSyncRepository(
            get()
        )
    }

    single<List<RemoteSyncRepository>>(named("RemoteSyncRepositories")) {
        listOf(
            get<RemoteSyncRepository>(named(RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.LIFTS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.PREVIOUS_SET_RESULTS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.PROGRAMS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LIFTS_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LOG_ENTRIES_COLLECTION)),
            get<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUTS_COLLECTION)),
        )
    }

    single<RemoteDataClient> {
        val remoteSyncRepos = get<List<RemoteSyncRepository>>(named("RemoteSyncRepositories"))
        val collectionTypes = remoteSyncRepos.fastMap {
            it.collectionName to it.remoteDtoType
        }.toMap()
        FirestoreRemoteDataClient(get(), collectionTypes)
    }

    single {
        val syncRepositories = get<List<RemoteSyncRepository>>(named("RemoteSyncRepositories"))
        SyncOrchestrator(
            get(),
            syncRepositories,
            get()
        )
    }

    workerOf(::SyncWorker)
}
