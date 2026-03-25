package com.browntowndev.liftlab.dependencyInjection

import androidx.compose.ui.util.fastMap
import androidx.work.WorkManager
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.remote.client.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.client.FirestoreClientImpl
import com.browntowndev.liftlab.core.data.remote.client.FirestoreRemoteDataClient
import com.browntowndev.liftlab.core.data.remote.client.RemoteDataClient
import com.browntowndev.liftlab.core.data.remote.repositories.CustomSetsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiftMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiftsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.LiveWorkoutCompletedSetsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.ProgramSyncPolicyRepositoryImpl
import com.browntowndev.liftlab.core.data.remote.repositories.ProgramSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.SetLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.VolumeMetricChartsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutInProgressSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutLiftsSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutLogEntriesSyncRepository
import com.browntowndev.liftlab.core.data.remote.repositories.WorkoutsSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramSyncPolicyRepository
import com.browntowndev.liftlab.core.domain.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import com.browntowndev.liftlab.core.sync.SyncOrchestrator
import com.browntowndev.liftlab.core.sync.SyncScheduler
import com.browntowndev.liftlab.core.sync.SyncWorker
import com.browntowndev.liftlab.core.sync.WorkManagerSyncScheduler
import com.browntowndev.liftlab.core.sync.policy.PostSyncPolicy
import com.browntowndev.liftlab.core.sync.policy.ProgramPostDownloadPolicy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val RemoteSyncRepositories = named("RemoteSyncRepositories")

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
            customSetsDao = get<LiftLabDatabase>().customSetsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION)) {
        HistoricalWorkoutNamesSyncRepository(
            historicalWorkoutNamesDao = get<LiftLabDatabase>().historicalWorkoutNamesDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION)) {
        LiftMetricChartsSyncRepository(
            liftMetricChartsDao = get<LiftLabDatabase>().liftMetricChartsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIFTS_COLLECTION)) {
        LiftsSyncRepository(
            liftsDao = get<LiftLabDatabase>().liftsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.LIVE_WORKOUT_COMPLETED_SETS_COLLECTION)) {
        LiveWorkoutCompletedSetsSyncRepository(
            liveWorkoutCompletedSetsDao = get<LiftLabDatabase>().liveWorkoutCompletedSetsDao(),
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.PROGRAMS_COLLECTION)) {
        ProgramSyncRepository(
            programsDao = get<LiftLabDatabase>().programsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION)) {
        SetLogEntriesSyncRepository(
            setLogEntriesDao = get<LiftLabDatabase>().setLogEntriesDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION)) {
        VolumeMetricChartsSyncRepository(
            volumeMetricChartsDao = get<LiftLabDatabase>().volumeMetricChartsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION)) {
        WorkoutInProgressSyncRepository(
            workoutInProgressDao = get<LiftLabDatabase>().workoutInProgressDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LIFTS_COLLECTION)) {
        WorkoutLiftsSyncRepository(
            workoutLiftsDao = get<LiftLabDatabase>().workoutLiftsDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUT_LOG_ENTRIES_COLLECTION)) {
        WorkoutLogEntriesSyncRepository(
            workoutLogEntriesDao = get<LiftLabDatabase>().workoutLogEntriesDao()
        )
    }
    single<RemoteSyncRepository>(named(RemoteCollectionNames.WORKOUTS_COLLECTION)) {
        WorkoutsSyncRepository(
            workoutsDao = get<LiftLabDatabase>().workoutsDao()
        )
    }

    single<List<RemoteSyncRepository>>(RemoteSyncRepositories) {
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
        val remoteSyncRepos = get<List<RemoteSyncRepository>>(RemoteSyncRepositories)
        val collectionTypes = remoteSyncRepos.fastMap {
            it.collectionName to it.remoteDtoType
        }.toMap()
        FirestoreRemoteDataClient(get(), collectionTypes)
    }

    single<ProgramSyncPolicyRepository> {
        ProgramSyncPolicyRepositoryImpl(
            programsDao = get<LiftLabDatabase>().programsDao(),
        )
    }
    single<PostSyncPolicy> {
        ProgramPostDownloadPolicy(
            programsRepository = get(),
            programsSyncPolicyRepository = get(),
            transactionScope = get(),
        )
    }

    single {
        val syncRepositories = get<List<RemoteSyncRepository>>(named("RemoteSyncRepositories"))
        SyncOrchestrator(
            syncMetadataRepository = get(),
            syncRepositories = syncRepositories,
            remoteDataClient = get(),
            transactionScope = get(),
            postDownloadPolicies = listOf(get<PostSyncPolicy>()),
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
