package com.browntowndev.liftlab.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.data.LiftLabDatabaseWorker.Companion.KEY_FILENAME
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.core.data.dao.LiftsDao
import com.browntowndev.liftlab.core.utils.LIFTS_DATA_FILENAME

@Database(entities = [Lift::class], version = 1, exportSchema = false)
abstract class LiftLabDatabase : RoomDatabase() {
    abstract fun liftsDao(): LiftsDao

    companion object {
        private const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null

        fun getInstance(context: Context): LiftLabDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LiftLabDatabase {
            return Room.databaseBuilder(context, LiftLabDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        val request = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                            .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                            .build()
                        WorkManager.getInstance(context).enqueue(request)
                    }
                })
                .build()
        }
    }
}
