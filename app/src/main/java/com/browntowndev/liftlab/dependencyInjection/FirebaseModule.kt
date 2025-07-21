package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreClientImpl
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.persistence.firestore.sync.datasources.*
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    singleOf(::FirestoreClientImpl)

    single<SyncableDataSource>(named(FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION)) { CustomLiftSetsDataSource(get<FirestoreClientImpl>(), get(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION)) { HistoricalWorkoutNamesDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION)) { LiftMetricChartsDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.LIFTS_COLLECTION)) { LiftsDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION)) { PreviousSetResultsDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.PROGRAMS_COLLECTION)) { ProgramsDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.SET_LOG_ENTRIES_COLLECTION)) { SetLogEntriesDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION)) { VolumeMetricChartsDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION)) { WorkoutInProgressDataSource(get<FirestoreClientImpl>(), get(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.WORKOUT_LIFTS_COLLECTION)) { WorkoutLiftsDataSource(get<FirestoreClientImpl>(), get(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION)) { WorkoutLogEntriesDataSource(get<FirestoreClientImpl>(), get()) }
    single<SyncableDataSource>(named(FirestoreConstants.WORKOUTS_COLLECTION)) { WorkoutsDataSource(get<FirestoreClientImpl>(), get(), get()) }

    single {
        val dataSources = mapOf(
            FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION)),
            FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION)),
            FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION)),
            FirestoreConstants.LIFTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.LIFTS_COLLECTION)),
            FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION)),
            FirestoreConstants.PROGRAMS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.PROGRAMS_COLLECTION)),
            FirestoreConstants.SET_LOG_ENTRIES_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.SET_LOG_ENTRIES_COLLECTION)),
            FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION)),
            FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION)),
            FirestoreConstants.WORKOUT_LIFTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.WORKOUT_LIFTS_COLLECTION)),
            FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION)),
            FirestoreConstants.WORKOUTS_COLLECTION to get<SyncableDataSource>(named(FirestoreConstants.WORKOUTS_COLLECTION)),
        )
        FirestoreSyncManager(
            firestoreClient = get<FirestoreClientImpl>(),
            syncScope = get(named("FirestoreSyncScope")),
            dataSources = dataSources,
            syncRepository = get(),
        )
    }
}
