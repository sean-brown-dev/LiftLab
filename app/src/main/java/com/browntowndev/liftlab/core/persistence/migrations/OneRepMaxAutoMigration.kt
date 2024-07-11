package com.browntowndev.liftlab.core.persistence.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.browntowndev.liftlab.core.progression.CalculationEngine

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "workoutLifts",
        columnName = "note"
    )
)
class OneRepMaxAutoMigration: AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            val prevSetResultQuery = db.query(
                "SELECT previously_completed_set_id, reps, weight, rpe " +
                        "FROM previousSetResults")

            while (prevSetResultQuery.moveToNext()) {
                val id = prevSetResultQuery.getLong(0)
                val reps = prevSetResultQuery.getInt(1)
                val weight = prevSetResultQuery.getFloat(2)
                val rpe = prevSetResultQuery.getFloat(3)
                val oneRepMax = CalculationEngine.getOneRepMax(
                    weight = weight,
                    reps = reps,
                    rpe = rpe
                )

                db.execSQL("UPDATE previousSetResults " +
                        "SET oneRepMax = @oneRepMax " +
                        "WHERE previously_completed_set_id = @id",
                    arrayOf(oneRepMax, id))
            }

            val setLogQuery = db.query(
                "SELECT set_log_entry_id, reps, weight, rpe " +
                        "FROM setLogEntries")

            while (setLogQuery.moveToNext()) {
                val id = setLogQuery.getLong(0)
                val reps = setLogQuery.getInt(1)
                val weight = setLogQuery.getFloat(2)
                val rpe = setLogQuery.getFloat(3)
                val oneRepMax = CalculationEngine.getOneRepMax(
                    weight = weight,
                    reps = reps,
                    rpe = rpe
                )

                db.execSQL("UPDATE setLogEntries " +
                        "SET oneRepMax = @oneRepMax " +
                        "WHERE set_log_entry_id = @id",
                    arrayOf(oneRepMax, id))
            }

            db.setTransactionSuccessful()
        }
        finally {
            db.endTransaction()
        }
    }
}