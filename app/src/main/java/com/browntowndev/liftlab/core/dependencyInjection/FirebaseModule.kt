package com.browntowndev.liftlab.core.dependencyInjection

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.sync.*
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.firebase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single {
        CustomLiftSetsSyncRepository(
            dao = get<LiftLabDatabase>().customSetsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        HistoricalWorkoutNamesSyncRepository(
            dao = get<LiftLabDatabase>().historicalWorkoutNamesDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        LiftMetricChartsSyncRepository(
            dao = get<LiftLabDatabase>().liftMetricChartsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        LiftsSyncRepository(
            dao = get<LiftLabDatabase>().liftsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        PreviousSetResultsSyncRepository(
            dao = get<LiftLabDatabase>().previousSetResultsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        ProgramsSyncRepository(
            dao = get<LiftLabDatabase>().programsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        RestTimerInProgressSyncRepository(
            dao = get<LiftLabDatabase>().restTimerInProgressDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        SetLogEntriesSyncRepository(
            dao = get<LiftLabDatabase>().setLogEntryDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        VolumeMetricChartsSyncRepository(
            dao = get<LiftLabDatabase>().volumeMetricChartsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        WorkoutInProgressSyncRepository(
            dao = get<LiftLabDatabase>().workoutInProgressDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        WorkoutLiftsSyncRepository(
            dao = get<LiftLabDatabase>().workoutLiftsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        WorkoutLogEntriesSyncRepository(
            dao = get<LiftLabDatabase>().workoutLogEntryDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        WorkoutsSyncRepository(
            dao = get<LiftLabDatabase>().workoutsDao(),
            firestore = get(),
            firebaseAuth = get<FirebaseAuth>(),
        )
    }
    single {
        SyncMetadataRepository(
            dao = get<LiftLabDatabase>().syncDao(),
        )
    }

    single {
        FirestoreSyncManager(
            firebaseAuth = get(),
            firestore = get(),
            customSetSyncRepository = get(),
            historicalWorkoutNameSyncRepository = get(),
            liftMetricChartSyncRepository = get(),
            liftsSyncRepository = get(),
            previousSetResultSyncRepository = get(),
            programSyncRepository = get(),
            restTimerInProgressSyncRepository = get(),
            setLogEntrySyncRepository = get(),
            volumeMetricChartSyncRepository = get(),
            workoutInProgressSyncRepository = get(),
            workoutLiftSyncRepository = get(),
            workoutLogEntrySyncRepository = get(),
            workoutSyncRepository = get(),
            syncRepository = get()
        )
    }
}
