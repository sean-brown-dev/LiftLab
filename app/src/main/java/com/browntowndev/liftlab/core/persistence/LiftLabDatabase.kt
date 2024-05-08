package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.lifecycle.Observer
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.persistence.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
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
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
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
        VolumeMetricChart::class,
   ],
    version = 10,
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
        AutoMigration(from = 9, to = 10, spec = LiftLabDatabase.Companion.StepSizeAutoMigration::class),
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
    abstract fun volumeMetricChartsDao(): VolumeMetricChartsDao

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

        class StepSizeAutoMigration: AutoMigrationSpec {
            override fun onPostMigrate(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    val query = db.query(
                        "SELECT workout_lift_id, repRangeTop, repRangeBottom, wl.deloadWeek as 'liftDeloadWeek', p.deloadWeek as 'programDeloadWeek' " +
                            "FROM workoutLifts wl " +
                            "JOIN workouts w ON wl.workoutId = w.workout_id " +
                            "JOIN programs p ON w.programId = p.program_id " +
                            "WHERE wl.progressionScheme = 'WAVE_LOADING_PROGRESSION'")

                    while (query.moveToNext()) {
                        val workoutLiftId = query.getLong(0)
                        val repRangeTop = query.getInt(1)
                        val repRangeBottom = query.getInt(2)
                        val workoutLiftDeloadWeek = query.getIntOrNull(3)
                        val programDeloadWeek = query.getInt(4)

                        val stepSize = Utils.getPossibleStepSizes(
                            repRangeTop = repRangeTop,
                            repRangeBottom = repRangeBottom,
                            stepCount = (workoutLiftDeloadWeek ?: programDeloadWeek) - 2
                        ).firstOrNull()

                        db.execSQL("UPDATE workoutLifts SET stepSize = @stepSize WHERE workout_lift_id = @workoutLiftId", arrayOf(stepSize, workoutLiftId))
                    }
                    db.setTransactionSuccessful()
                }
                finally {
                    db.endTransaction()
                }
            }
        }
    }
}
