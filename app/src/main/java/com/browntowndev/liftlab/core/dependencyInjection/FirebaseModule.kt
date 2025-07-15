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
        CustomLiftSetsSyncRepository(
            dao = get<LiftLabDatabase>().customSetsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        HistoricalWorkoutNamesSyncRepository(
            dao = get<LiftLabDatabase>().historicalWorkoutNamesDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        LiftMetricChartsSyncRepository(
            dao = get<LiftLabDatabase>().liftMetricChartsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        LiftsSyncRepository(
            dao = get<LiftLabDatabase>().liftsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        PreviousSetResultsSyncRepository(
            dao = get<LiftLabDatabase>().previousSetResultsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        ProgramsSyncRepository(
            dao = get<LiftLabDatabase>().programsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        RestTimerInProgressSyncRepository(
            dao = get<LiftLabDatabase>().restTimerInProgressDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        SetLogEntriesSyncRepository(
            dao = get<LiftLabDatabase>().setLogEntryDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        VolumeMetricChartsSyncRepository(
            dao = get<LiftLabDatabase>().volumeMetricChartsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutInProgressSyncRepository(
            dao = get<LiftLabDatabase>().workoutInProgressDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutLiftsSyncRepository(
            dao = get<LiftLabDatabase>().workoutLiftsDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutLogEntriesSyncRepository(
            dao = get<LiftLabDatabase>().workoutLogEntryDao(),
            firestore = get(),
            userId = get<FirebaseAuth>().currentUser?.uid ?: ""
        )
    }
    single {
        WorkoutsSyncRepository(
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
