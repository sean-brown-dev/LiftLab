package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.persistence.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.PreviouslyCompletedSet
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry

@TypeConverters(Converters::class)
@Database(
    entities = [
        Lift::class, CustomLiftSet::class,
        HistoricalWorkoutName::class,
        PreviouslyCompletedSet::class,
        Program::class,
        SetLogEntry::class,
        Workout::class,
        WorkoutLift::class,
        WorkoutLogEntry::class,
   ],
    version = 16,
    exportSchema = false)
abstract class LiftLabDatabase : RoomDatabase() {
    abstract fun liftsDao(): LiftsDao
    abstract fun programsDao(): ProgramsDao
    abstract fun workoutsDao(): WorkoutsDao
    abstract fun workoutLiftsDao(): WorkoutLiftsDao
    abstract fun customSetsDao(): CustomSetsDao
    abstract fun loggingDao(): LoggingDao

    companion object {
        private const val LIFTS_DATA_FILENAME = "lifts.json"
        private const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null

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

            val sharedPreferences = context.getSharedPreferences("LiftLabPreferences", Context.MODE_PRIVATE)
            val isDatabaseInitialized = sharedPreferences.getBoolean("database_initialized", false)

            if (!isDatabaseInitialized) {
                val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                    .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                    .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("init_db", ExistingWorkPolicy.KEEP, request)
            }
        }
    }
}
