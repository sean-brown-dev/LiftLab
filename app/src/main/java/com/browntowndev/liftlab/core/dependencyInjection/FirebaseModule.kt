package com.browntowndev.liftlab.core.dependencyInjection

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.sync.*
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.firestore.*
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    singleOf(::LiftLabFirestoreClient)

    single {
        CustomLiftSetsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        HistoricalWorkoutNamesSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        LiftMetricChartsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        LiftsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        PreviousSetResultsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        ProgramsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        SetLogEntriesSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        VolumeMetricChartsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        WorkoutInProgressSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        WorkoutLiftsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        WorkoutLogEntriesSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        WorkoutsSyncRepository(
            dao = get(),
            firestore = get(),
            firebaseAuth = get(),
        )
    }
    single {
        SyncMetadataRepository(
            dao = get(),
        )
    }

    single {
        FirestoreSyncManager(
            firestoreClient = get<LiftLabFirestoreClient>(),
            syncScope = get(named("FirestoreSyncScope")),
            customLiftSetsSyncRepository = get(),
            historicalWorkoutNamesSyncRepository = get(),
            liftMetricChartsSyncRepository = get(),
            liftsSyncRepository = get(),
            previousSetResultsSyncRepository = get(),
            programsSyncRepository = get(),
            setLogEntriesSyncRepository = get(),
            volumeMetricChartsSyncRepository = get(),
            workoutInProgressSyncRepository = get(),
            workoutLiftsSyncRepository = get(),
            workoutLogEntriesSyncRepository = get(),
            workoutsSyncRepository = get(),
            syncRepository = get(),
        )
    }
}
