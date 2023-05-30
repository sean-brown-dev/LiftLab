package com.browntowndev.liftlab.core.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.browntowndev.liftlab.core.common.enums.LiftCategory
import com.browntowndev.liftlab.core.common.enums.LiftCategoryDeserializer
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.core.data.entities.Program
import com.browntowndev.liftlab.core.data.entities.Workout
import com.browntowndev.liftlab.core.data.entities.WorkoutLift
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
                            database.clearAllTables()
                            database.liftsDao().insertAll(lifts)
                            populateDefaultProgram(db = database)

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

    private suspend fun populateDefaultProgram(db: LiftLabDatabase) {
        val liftsByNameAndCategory = db.liftsDao().getAll().associateBy { "${it.lift.name}-${it.lift.category}" }

        val programId: Long = db.programsDao().insert(Program(name = "Intermediate Upper/Lower"))

        val lowerAId: Long = db.workoutsDao().insert(Workout(name = "Lower A", position = 0, programId = programId))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerAId,
            liftId = liftsByNameAndCategory["Hack Squat-LEG_PUSH"]?.lift?.id ?: throw Exception("Couldn't find HackSquat-LEG_PUSH"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerAId,
            liftId = liftsByNameAndCategory["Deadlift (Romanian)-HIP_HINGE"]?.lift?.id ?: throw Exception("Couldn't find Deadlift (Romanian)-HIP_HINGE"),
            position = 1,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerAId,
            liftId = liftsByNameAndCategory["Split Squat (Bulgarian)-QUAD_ISO"]?.lift?.id ?: throw Exception("Couldn't find Split Squat (Bulgarian)-QUAD_ISO"),
            position = 2,
            rpeTarget = 7,
            repRangeBottom = 10,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerAId,
            liftId = liftsByNameAndCategory["Leg Curl (Seated)-HAMSTRING_ISO"]?.lift?.id ?: throw Exception("Couldn't find Leg Curl (Seated)-HAMSTRING_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 10,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerAId,
            liftId = liftsByNameAndCategory["Calf Raise (Smith Standing)-CALVES"]?.lift?.id ?: throw Exception("Couldn't find Calf Raise (Smith Standing)-CALVES"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 12,
            repRangeTop = 15,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))

        val upperAId: Long = db.workoutsDao().insert(Workout(name = "Upper A", position = 0, programId = programId))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Bench Press (Dumbbell)-HORIZONTAL_PUSH"]?.lift?.id ?: throw Exception("Couldn't find Bench Press (Dumbbell)-HORIZONTAL_PUSH"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Lat Pulldown (Hammer Machine)-VERTICAL_PULL"]?.lift?.id ?: throw Exception("Couldn't find Lat Pulldown (Hammer Machine)-VERTICAL_PULL"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Flye (Cable)-CHEST_ISO"]?.lift?.id ?: throw Exception("Couldn't find Flye (Cable)-CHEST_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 12,
            repRangeTop = 15,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Skullcrusher-TRICEP_ISO"]?.lift?.id ?: throw Exception("Couldn't find Skullcrusher-TRICEP_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 10,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Row (Cable Neutral-Grip Seated)-HORIZONTAL_PULL"]?.lift?.id ?: throw Exception("Couldn't find Row (Cable Neutral-Grip Seated)-HORIZONTAL_PULL"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 10,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Bicep Curl (Dumbbell Incline)-BICEP_ISO"]?.lift?.id ?: throw Exception("Couldn't find Bicep Curl (Dumbbell Incline)-BICEP_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 12,
            repRangeTop = 15,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperAId,
            liftId = liftsByNameAndCategory["Lateral Raise (Dumbbell)-DELT_ISO"]?.lift?.id ?: throw Exception("Couldn't find Lateral Raise (Dumbbell)-DELT_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 12,
            repRangeTop = 18,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))

        val lowerBId: Long = db.workoutsDao().insert(Workout(name = "Lower B", position = 0, programId = programId))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerBId,
            liftId = liftsByNameAndCategory["Leg Press-LEG_PUSH"]?.lift?.id ?: throw Exception("Couldn't find Hack Squat-LEG_PUSH"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerBId,
            liftId = liftsByNameAndCategory["Deadlift (Stiff-Legged)-HIP_HINGE"]?.lift?.id ?: throw Exception("Couldn't find Deadlift (Stiff-Legged)-HIP_HINGE"),
            position = 1,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerBId,
            liftId = liftsByNameAndCategory["Leg Extensions-QUAD_ISO"]?.lift?.id ?: throw Exception("Couldn't find Leg Extensions-QUAD_ISO"),
            position = 2,
            rpeTarget = 7,
            repRangeBottom = 10,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerBId,
            liftId = liftsByNameAndCategory["Back Extensions-HAMSTRING_ISO"]?.lift?.id ?: throw Exception("Couldn't find Back Extensions-HAMSTRING_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = lowerBId,
            liftId = liftsByNameAndCategory["Calf Raise (Smith Seated)-CALVES"]?.lift?.id ?: throw Exception("Couldn't find Calf Raise (Smith Seated)-CALVES"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 15,
            repRangeTop = 20,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))

        val upperBId: Long = db.workoutsDao().insert(Workout(name = "Upper B", position = 0, programId = programId))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Bench Press (Incline)-INCLINE_PUSH"]?.lift?.id ?: throw Exception("Couldn't find Bench Press (Incline)-INCLINE_PUSH"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Lat Pulldown-VERTICAL_PULL"]?.lift?.id ?: throw Exception("Couldn't find Lat Pulldown-VERTICAL_PULL"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 10,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Chest Press (Machine)-CHEST_ISO"]?.lift?.id ?: throw Exception("Couldn't find Chest Press (Machine)-CHEST_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 8,
            repRangeTop = 10,
            progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Tricep Pushdown (Rope)-TRICEP_ISO"]?.lift?.id ?: throw Exception("Couldn't find Tricep Pushdown (Rope)-TRICEP_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 10,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Row (T-Bar)-HORIZONTAL_PULL"]?.lift?.id ?: throw Exception("Couldn't find Row (T-Bar)-HORIZONTAL_PULL"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 6,
            repRangeTop = 8,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Hammer Curl-BICEP_ISO"]?.lift?.id ?: throw Exception("Couldn't find Hammer Curl-BICEP_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 10,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
        db.workoutLiftsDao().insert(WorkoutLift(
            workoutId = upperBId,
            liftId = liftsByNameAndCategory["Face Pull-DELT_ISO"]?.lift?.id ?: throw Exception("Couldn't find Face Pull-DELT_ISO"),
            position = 0,
            rpeTarget = 8,
            repRangeBottom = 10,
            repRangeTop = 12,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setCount = 3,
        ))
    }

    companion object {
        private const val TAG = "LiftLabDatabaseWorker"
        const val KEY_FILENAME = "LIFTS_DATA_FILENAME"
    }
}
