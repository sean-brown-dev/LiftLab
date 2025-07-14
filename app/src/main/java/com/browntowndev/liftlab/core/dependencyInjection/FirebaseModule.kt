package com.browntowndev.liftlab.core.dependencyInjection

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.sync.*
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.firebase.*
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single {
        CustomSetBaseSyncRepository(
            dao = get<LiftLabDatabase>().customSetsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        HistoricalWorkoutNameBaseSyncRepository(
            dao = get<LiftLabDatabase>().historicalWorkoutNamesDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        LiftMetricChartBaseSyncRepository(
            dao = get<LiftLabDatabase>().liftMetricChartsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        LiftSyncRepository(
            dao = get<LiftLabDatabase>().liftsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        PreviousSetResultBaseSyncRepository(
            dao = get<LiftLabDatabase>().previousSetResultsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        ProgramBaseSyncRepository(
            dao = get<LiftLabDatabase>().programsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        RestTimerInProgressBaseSyncRepository(
            dao = get<LiftLabDatabase>().restTimerInProgressDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        SetLogEntryBaseSyncRepository(
            dao = get<LiftLabDatabase>().setLogEntryDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        VolumeMetricChartBaseSyncRepository(
            dao = get<LiftLabDatabase>().volumeMetricChartsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutInProgressBaseSyncRepository(
            dao = get<LiftLabDatabase>().workoutInProgressDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutLiftBaseSyncRepository(
            dao = get<LiftLabDatabase>().workoutLiftsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutLogEntryBaseSyncRepository(
            dao = get<LiftLabDatabase>().workoutLogEntryDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutBaseSyncRepository(
            dao = get<LiftLabDatabase>().workoutsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
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
            liftSyncRepository = get(),
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
