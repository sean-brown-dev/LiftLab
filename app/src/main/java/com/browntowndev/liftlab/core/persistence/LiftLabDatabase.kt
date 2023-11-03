package com.browntowndev.liftlab.core.persistence

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.AutoMigration
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
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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
   ],
    version = 1,
    exportSchema = true,
    /*autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]*/)
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
            if (!isDatabaseInitialized) {
                val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                    .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                    .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("init_db", ExistingWorkPolicy.KEEP, request)
            } else {
                initialized = true
            }
        }

        fun exportDatabase(context: Context, uri: Uri) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbFileInputStream = FileInputStream(dbFile)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                copy(dbFileInputStream, outputStream)
            }
        }

        fun importDatabase(context: Context, uri: Uri) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbFileOutputStream = FileOutputStream(dbFile)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                copy(inputStream, dbFileOutputStream)
            }

            instance = null
            getInstance(context)
        }

        @Throws(IOException::class)
        private fun copy(inputStream: InputStream, outputStream: OutputStream) {
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
    }
}
