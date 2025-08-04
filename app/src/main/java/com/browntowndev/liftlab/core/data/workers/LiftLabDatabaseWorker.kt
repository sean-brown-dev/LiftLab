package com.browntowndev.liftlab.core.data.workers

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.MovementPatternDeserializer
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiftLabDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "LiftLabDatabaseWorker"
        private const val KEY_FILENAME = "LIFTS_DATA_FILENAME"
        private const val LIFTS_DATA_FILENAME = "lifts.json"

        fun startDatabaseInitialization(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Define the work request with its input data.
            val initRequest = OneTimeWorkRequestBuilder<LiftLabDatabaseWorker>()
                .setInputData(workDataOf(KEY_FILENAME to LIFTS_DATA_FILENAME))
                .build()

            // Enqueue the work uniquely. This is a "fire-and-forget" operation.
            // WorkManager handles retries and guarantees it will eventually run.
            workManager.enqueueUniqueWork(
                "init_db", // A unique name for this work
                ExistingWorkPolicy.KEEP, // If work already exists, do nothing
                initRequest
            )

            Log.d(TAG, "Database seed worker has been enqueued.")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val filename = inputData.getString(KEY_FILENAME)
            if (filename != null) {
                applicationContext.assets.open(filename).use { inputStream ->
                    inputStream.reader().use { reader ->
                        val liftEntityType = object : TypeToken<List<LiftEntity>>() {}.type
                        val liftEntities: List<LiftEntity> = (GsonBuilder()
                            .registerTypeAdapter(
                                MovementPattern::class.java,
                                MovementPatternDeserializer()
                            )
                            .create()
                            .fromJson(reader, liftEntityType) as List<LiftEntity>)
                            .fastMap { it.copy(restTimerEnabled = true) }

                        val database = LiftLabDatabase.Companion.getInstance(applicationContext)
                        database.withTransaction {
                            database.clearAllTables()
                            database.liftsDao().insertMany(liftEntities)
                            populateDefaultProgram(db = database)
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

    private suspend fun populateDefaultProgram(db: LiftLabDatabase) {
        val liftsByNameAndCategory = db.liftsDao().getAll().associateBy { "${it.name}-${it.movementPattern}" }
        val programEntityId: Long = db.programsDao().insert(ProgramEntity(name = "Intermediate Upper/Lower"))

        val lowerAId: Long = db.workoutsDao().insert(
            WorkoutEntity(
                name = "Lower A",
                position = 0,
                programId = programEntityId
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerAId,
                liftId = liftsByNameAndCategory["Hack Squat-LEG_PUSH"]?.id
                    ?: throw Exception("Couldn't find HackSquat-LEG_PUSH"),
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerAId,
                liftId = liftsByNameAndCategory["Deadlift (Romanian)-HIP_HINGE"]?.id
                    ?: throw Exception("Couldn't find Deadlift (Romanian)-HIP_HINGE"),
                position = 1,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerAId,
                liftId = liftsByNameAndCategory["Split Squat (Bulgarian)-QUAD_ISO"]?.id
                    ?: throw Exception("Couldn't find Split Squat (Bulgarian)-QUAD_ISO"),
                position = 2,
                rpeTarget = 7f,
                repRangeBottom = 10,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerAId,
                liftId = liftsByNameAndCategory["Leg Curl (Seated)-HAMSTRING_ISO"]?.id
                    ?: throw Exception("Couldn't find Leg Curl (Seated)-HAMSTRING_ISO"),
                position = 3,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerAId,
                liftId = liftsByNameAndCategory["Calf Raise (Smith Standing)-CALVES"]?.id
                    ?: throw Exception("Couldn't find Calf Raise (Smith Standing)-CALVES"),
                position = 4,
                rpeTarget = 8f,
                repRangeBottom = 12,
                repRangeTop = 15,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )

        val upperAId: Long = db.workoutsDao().insert(
            WorkoutEntity(
                name = "Upper A",
                position = 1,
                programId = programEntityId
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Bench Press (Dumbbell)-HORIZONTAL_PUSH"]?.id
                    ?: throw Exception("Couldn't find Bench Press (Dumbbell)-HORIZONTAL_PUSH"),
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Lat Pulldown (Hammer Machine)-VERTICAL_PULL"]?.id
                    ?: throw Exception("Couldn't find Lat Pulldown (Hammer Machine)-VERTICAL_PULL"),
                position = 1,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Flye (Cable)-CHEST_ISO"]?.id
                    ?: throw Exception("Couldn't find Flye (Cable)-CHEST_ISO"),
                position = 2,
                rpeTarget = 8f,
                repRangeBottom = 12,
                repRangeTop = 15,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Skullcrusher-TRICEP_ISO"]?.id
                    ?: throw Exception("Couldn't find Skullcrusher-TRICEP_ISO"),
                position = 3,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Row (Cable Neutral-Grip Seated)-HORIZONTAL_PULL"]?.id
                    ?: throw Exception("Couldn't find Row (Cable Neutral-Grip Seated)-HORIZONTAL_PULL"),
                position = 4,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Bicep Curl (Dumbbell Incline)-BICEP_ISO"]?.id
                    ?: throw Exception("Couldn't find Bicep Curl (Dumbbell Incline)-BICEP_ISO"),
                position = 5,
                rpeTarget = 8f,
                repRangeBottom = 12,
                repRangeTop = 15,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperAId,
                liftId = liftsByNameAndCategory["Lateral Raise (Dumbbell)-DELT_ISO"]?.id
                    ?: throw Exception("Couldn't find Lateral Raise (Dumbbell)-DELT_ISO"),
                position = 6,
                rpeTarget = 8f,
                repRangeBottom = 12,
                repRangeTop = 18,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )

        val lowerBId: Long = db.workoutsDao().insert(
            WorkoutEntity(
                name = "Lower B",
                position = 2,
                programId = programEntityId
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerBId,
                liftId = liftsByNameAndCategory["Leg Press-LEG_PUSH"]?.id
                    ?: throw Exception("Couldn't find Hack Squat-LEG_PUSH"),
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerBId,
                liftId = liftsByNameAndCategory["Deadlift (Stiff-Legged)-HIP_HINGE"]?.id
                    ?: throw Exception("Couldn't find Deadlift (Stiff-Legged)-HIP_HINGE"),
                position = 1,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerBId,
                liftId = liftsByNameAndCategory["Leg Extensions-QUAD_ISO"]?.id
                    ?: throw Exception("Couldn't find Leg Extensions-QUAD_ISO"),
                position = 2,
                rpeTarget = 7f,
                repRangeBottom = 10,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerBId,
                liftId = liftsByNameAndCategory["Back Extensions-HAMSTRING_ISO"]?.id
                    ?: throw Exception("Couldn't find Back Extensions-HAMSTRING_ISO"),
                position = 3,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = lowerBId,
                liftId = liftsByNameAndCategory["Calf Raise (Smith Seated)-CALVES"]?.id
                    ?: throw Exception("Couldn't find Calf Raise (Smith Seated)-CALVES"),
                position = 4,
                rpeTarget = 8f,
                repRangeBottom = 15,
                repRangeTop = 20,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )

        val upperBId: Long = db.workoutsDao().insert(
            WorkoutEntity(
                name = "Upper B",
                position = 3,
                programId = programEntityId
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Bench Press (Incline)-INCLINE_PUSH"]?.id
                    ?: throw Exception("Couldn't find Bench Press (Incline)-INCLINE_PUSH"),
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Lat Pulldown-VERTICAL_PULL"]?.id
                    ?: throw Exception("Couldn't find Lat Pulldown-VERTICAL_PULL"),
                position = 1,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Chest Press (Machine)-CHEST_ISO"]?.id
                    ?: throw Exception("Couldn't find Chest Press (Machine)-CHEST_ISO"),
                position = 2,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Tricep Pushdown (Rope)-TRICEP_ISO"]?.id
                    ?: throw Exception("Couldn't find Tricep Pushdown (Rope)-TRICEP_ISO"),
                position = 3,
                rpeTarget = 8f,
                repRangeBottom = 10,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Row (T-Bar)-HORIZONTAL_PULL"]?.id
                    ?: throw Exception("Couldn't find Row (T-Bar)-HORIZONTAL_PULL"),
                position = 4,
                rpeTarget = 8f,
                repRangeBottom = 6,
                repRangeTop = 8,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Hammer Curl-BICEP_ISO"]?.id
                    ?: throw Exception("Couldn't find Hammer Curl-BICEP_ISO"),
                position = 5,
                rpeTarget = 8f,
                repRangeBottom = 10,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
        db.workoutLiftsDao().insert(
            WorkoutLiftEntity(
                workoutId = upperBId,
                liftId = liftsByNameAndCategory["Face Pull-DELT_ISO"]?.id
                    ?: throw Exception("Couldn't find Face Pull-DELT_ISO"),
                position = 6,
                rpeTarget = 8f,
                repRangeBottom = 10,
                repRangeTop = 12,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                setCount = 3,
            )
        )
    }
}