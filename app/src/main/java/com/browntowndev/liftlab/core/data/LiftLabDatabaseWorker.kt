package com.browntowndev.liftlab.core.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.browntowndev.liftlab.core.common.enums.LiftCategory
import com.browntowndev.liftlab.core.common.enums.LiftCategoryDeserializer
import com.browntowndev.liftlab.core.data.entities.Lift
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiftLabDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val filename = inputData.getString(KEY_FILENAME)
            if (filename != null) {
                applicationContext.assets.open(filename).use { inputStream ->
                    inputStream.reader().use { reader ->
                        val liftType = object : TypeToken<List<Lift>>() {}.type
                        val lifts: List<Lift> = GsonBuilder()
                            .registerTypeAdapter(LiftCategory::class.java, LiftCategoryDeserializer())
                            .create()
                            .fromJson(reader, liftType)

                        val database = LiftLabDatabase.getInstance(applicationContext)
                        database.withTransaction {
                            database.liftsDao().insertAll(lifts)

                            val sharedPreferences = applicationContext.getSharedPreferences("LiftLabPreferences", Context.MODE_PRIVATE)
                            sharedPreferences.edit().putBoolean("database_initialized", true).apply()
                        }

                        Result.success()
                    }
                }
            } else {
                Log.e(TAG, "Error seeding database - no valid filename")
                Result.failure()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error seeding database", ex)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "LiftLabDatabaseWorker"
        const val KEY_FILENAME = "LIFTS_DATA_FILENAME"
    }
}
