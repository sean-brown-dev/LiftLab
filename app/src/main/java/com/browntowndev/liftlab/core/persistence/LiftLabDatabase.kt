package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.BuildConfig
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.persistence.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.persistence.entities.room.CustomLiftSetEntity
import com.browntowndev.liftlab.core.persistence.entities.room.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.room.PreviousSetResultEntity
import com.browntowndev.liftlab.core.persistence.entities.room.ProgramEntity
import com.browntowndev.liftlab.core.persistence.entities.room.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.persistence.entities.room.SetLogEntryEntity
import com.browntowndev.liftlab.core.persistence.entities.room.SyncMetadataEntity
import com.browntowndev.liftlab.core.persistence.entities.room.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLiftEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.persistence.room.Converters
import com.browntowndev.liftlab.core.persistence.room.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.room.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.room.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.room.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.room.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.room.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.room.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.room.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.room.dao.SyncDao
import com.browntowndev.liftlab.core.persistence.room.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.room.migrations.LiftNoteMigration
import com.browntowndev.liftlab.core.persistence.room.migrations.OneRepMaxAutoMigration
import com.browntowndev.liftlab.core.persistence.room.migrations.StepSizeAutoMigration
import com.browntowndev.liftlab.core.persistence.room.migrations.WorkoutInProgressMigration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@TypeConverters(Converters::class)
@Database(
    entities = [
        LiftEntity::class,
        CustomLiftSetEntity::class,
        HistoricalWorkoutNameEntity::class,
        PreviousSetResultEntity::class,
        ProgramEntity::class,
        WorkoutLogEntryEntity::class,
        SetLogEntryEntity::class,
        WorkoutEntity::class,
        WorkoutLiftEntity::class,
        WorkoutInProgressEntity::class,
        RestTimerInProgressEntity::class,
        LiftMetricChartEntity::class,
        VolumeMetricChartEntity::class,
        SyncMetadataEntity::class,
   ],
    version = 15,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = StepSizeAutoMigration::class),
        AutoMigration(from = 10, to = 11, spec = OneRepMaxAutoMigration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
    ])
abstract class LiftLabDatabase : RoomDatabase() {
    abstract fun liftsDao(): LiftsDao
    abstract fun programsDao(): ProgramsDao
    abstract fun workoutsDao(): WorkoutsDao
    abstract fun workoutLiftsDao(): WorkoutLiftsDao
    abstract fun customSetsDao(): CustomSetsDao
    abstract fun previousSetResultsDao(): PreviousSetResultDao
    abstract fun workoutInProgressDao(): WorkoutInProgressDao
    abstract fun historicalWorkoutNamesDao(): HistoricalWorkoutNamesDao
    abstract fun workoutLogEntryDao(): WorkoutLogEntryDao
    abstract fun setLogEntryDao(): SetLogEntryDao
    abstract fun restTimerInProgressDao(): RestTimerInProgressDao
    abstract fun liftMetricChartsDao(): LiftMetricChartsDao
    abstract fun volumeMetricChartsDao(): VolumeMetricChartsDao
    abstract fun syncDao(): SyncDao

    companion object {
        private const val LIFTS_DATA_FILENAME = "lifts.json"
        const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null
        private val _initialized = MutableStateFlow(false)
        val initialized = _initialized.asStateFlow()

        val isOpen: Boolean
            get() = synchronized(this) {
                instance?.isOpen == true
            }

        fun getInstance(context: Context): LiftLabDatabase {
            Log.d("LiftLabDatabase", "getInstance called. Current instance: $instance")
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LiftLabDatabase {
            val dbName = if (BuildConfig.USE_SCRATCH_DB) "scratch_$DATABASE_NAME" else DATABASE_NAME
            val db: LiftLabDatabase = Room
                .databaseBuilder(context, LiftLabDatabase::class.java, dbName)
                .addMigrations(LiftNoteMigration(), WorkoutInProgressMigration())
                .fallbackToDestructiveMigration(false)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        Log.d("LiftLabDatabase", "DB opened at: $db, hash: ${db.hashCode()}")
                    }
                })
                .build()

            submitDataInitializationJob(context)

            return db
        }

        private fun submitDataInitializationJob(context: Context) {
            val dbInitializedSetting = if (BuildConfig.USE_SCRATCH_DB) "scratch_$DB_INITIALIZED" else DB_INITIALIZED
            val isDatabaseInitialized = SettingsManager.getSetting(dbInitializedSetting, false)
            if (!isDatabaseInitialized) {
                val workManager = WorkManager.getInstance(context)
                val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                    .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                    .build()

                workManager.enqueueUniqueWork("init_db", ExistingWorkPolicy.KEEP, request)

                val liveWorkInfo = workManager.getWorkInfoByIdLiveData(request.id)
                var workInfoObserver: Observer<WorkInfo?>? = null
                workInfoObserver = Observer { workInfo ->
                    Log.d(Log.DEBUG.toString(), "db worker finished: ${workInfo?.state?.isFinished}. state: ${workInfo?.state}")
                    if (workInfo?.state?.isFinished == true) {
                        val success = workInfo.state == WorkInfo.State.SUCCEEDED
                        setAsInitialized(success)

                        workInfoObserver?.let { observer ->
                            liveWorkInfo.removeObserver(observer)
                        }
                    }
                }

                liveWorkInfo.observeForever(workInfoObserver)

            } else {
                _initialized.update { true }
            }
        }

        private fun setAsInitialized(success: Boolean) {
            val dbInitializedSetting = if (BuildConfig.USE_SCRATCH_DB) "scratch_$DB_INITIALIZED" else DB_INITIALIZED
            SettingsManager.setSetting(dbInitializedSetting, success)
            _initialized.update { true }
        }
    }
}
