package com.browntowndev.liftlab.dependencyInjection

import androidx.compose.ui.util.fastMap
import androidx.work.WorkManager
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.FirestoreClientImpl
import com.browntowndev.liftlab.core.data.remote.FirestoreRemoteDataClient
import com.browntowndev.liftlab.core.data.remote.RemoteDataClient
import com.browntowndev.liftlab.core.data.remote.SyncOrchestrator
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.data.remote.SyncWorker
import com.browntowndev.liftlab.core.data.remote.WorkManagerSyncScheduler
import com.browntowndev.liftlab.core.data.remote.repositories.CustomSetsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiftMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiftsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiveWorkoutCompletedSetsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.ProgramSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.SetLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.VolumeMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutInProgressSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutLiftsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutsSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val syncModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<FirestoreClient> { FirestoreClientImpl(get(), get(), get(AppScope)) }
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
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIVE_WORKOUT_COMPLETED_SETS_COLLECTION)) {
        LiveWorkoutCompletedSetsSyncRepository(
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
            get<RemoteSyncRepository>(named(RemoteCollectionNames.LIVE_WORKOUT_COMPLETED_SETS_COLLECTION)),
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
            syncMetadataRepository = get(),
            syncRepositories = syncRepositories,
            remoteDataClient = get(),
            transactionScope = get(),
            syncHierarchy = listOf(
                // Level 0: No dependencies
                hashSetOf(
                    RemoteCollectionNames.LIFTS_COLLECTION,
                    RemoteCollectionNames.PROGRAMS_COLLECTION,
                    RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                    RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION,
                ),
                // Level 1: Depends on Level 0
                hashSetOf(
                    RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION,
                    RemoteCollectionNames.WORKOUTS_COLLECTION,
                ),
                // Level 2: Depends on Level 1
                hashSetOf(
                    RemoteCollectionNames.WORKOUT_LIFTS_COLLECTION,
                    RemoteCollectionNames.WORKOUT_LOG_ENTRIES_COLLECTION,
                    RemoteCollectionNames.LIVE_WORKOUT_COMPLETED_SETS_COLLECTION,
                ),
                // Level 3: Depends on Level 2
                hashSetOf(
                    RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION,
                    RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION,
                    RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION,
                ),
            )
        )
    }

    workerOf(::SyncWorker)
}
