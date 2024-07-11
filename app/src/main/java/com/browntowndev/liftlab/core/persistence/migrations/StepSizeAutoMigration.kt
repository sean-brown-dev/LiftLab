package com.browntowndev.liftlab.core.persistence.migrations

import androidx.core.database.getIntOrNull
import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "workoutLifts",
        columnName = "note"
    )
)
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

                val stepSize = getPossibleStepSizes(
                    repRangeTop = repRangeTop,
                    repRangeBottom = repRangeBottom,
                    stepCount = (workoutLiftDeloadWeek ?: programDeloadWeek) - 2
                ).firstOrNull()

                db.execSQL("UPDATE workoutLifts " +
                        "SET stepSize = @stepSize " +
                        "WHERE workout_lift_id = @workoutLiftId",
                    arrayOf(stepSize, workoutLiftId))
            }
            db.setTransactionSuccessful()
        }
        finally {
            db.endTransaction()
        }
    }
}