package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.persistence.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry

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
   ],
    version = 22,
    exportSchema = false)
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

    companion object {
        private const val LIFTS_DATA_FILENAME = "lifts.json"
        private const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null
        var initialized by mutableStateOf(false)

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
            Log.w("TRACE", "Entered submitDataInitializationJob()")

            val isDatabaseInitialized = SettingsManager.getSetting(DB_INITIALIZED, false)
            if (isDatabaseInitialized) {
                val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                    .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                    .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("init_db", ExistingWorkPolicy.KEEP, request)

                WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id)
            } else {
                initialized = true
            }
        }
    }
}
