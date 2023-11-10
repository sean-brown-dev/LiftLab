package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.persistence.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@TypeConverters(Converters::class)
@Database(
    entities = [
        Lift::class,
        CustomLiftSet::class,
        HistoricalWorkoutName::class,
        PreviousSetResult::class,
        Program::class,
        WorkoutLogEntry::class,
        SetLogEntry::class,
        Workout::class,
        WorkoutLift::class,
        WorkoutInProgress::class,
        RestTimerInProgress::class,
        LiftMetricChart::class,
   ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
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
    abstract fun loggingDao(): LoggingDao
    abstract fun restTimerInProgressDao(): RestTimerInProgressDao
    abstract fun liftMetricChartsDao(): LiftMetricChartsDao

    companion object {
        private const val LIFTS_DATA_FILENAME = "lifts.json"
        private const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null
        private val _initialized = MutableStateFlow(false)
        val initialized = _initialized.asStateFlow()

        fun getInstance(context: Context): LiftLabDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LiftLabDatabase {
            val db: LiftLabDatabase = Room
                .databaseBuilder(context, LiftLabDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()

            submitDataInitializationJob(context)

            return db
        }

        private fun submitDataInitializationJob(context: Context) {
            val isDatabaseInitialized = SettingsManager.getSetting(DB_INITIALIZED, false)
            if (!isDatabaseInitialized) {
                val workManager = WorkManager.getInstance(context)
                val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                    .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                    .build()

                workManager.enqueueUniqueWork("init_db", ExistingWorkPolicy.KEEP, request)

                val liveWorkInfo = workManager.getWorkInfoByIdLiveData(request.id)
                var workInfoObserver: Observer<WorkInfo>? = null
                workInfoObserver = Observer { workInfo ->
                    Log.d(Log.DEBUG.toString(), "db worker finished: ${workInfo.state.isFinished}. state: ${workInfo.state}")
                    if (workInfo.state.isFinished) {
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
            SettingsManager.setSetting(DB_INITIALIZED, success)
            _initialized.update { true }
        }
    }
}
