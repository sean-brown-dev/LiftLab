package com.browntowndev.liftlab.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.data.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.data.dao.LiftsDao
import com.browntowndev.liftlab.core.data.entities.Lift

@Database(entities = [Lift::class], version = 1, exportSchema = false)
abstract class LiftLabDatabase : RoomDatabase() {
    abstract fun liftsDao(): LiftsDao

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
            val db: LiftLabDatabase = Room.databaseBuilder(context, LiftLabDatabase::class.java, DATABASE_NAME).build()
            submitDataInitializationJob(context)

            return db
        }

        private fun submitDataInitializationJob(context: Context) {
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
