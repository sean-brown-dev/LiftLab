package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class WorkoutInProgressMigration: Migration(14,15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new table with FK + index
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workoutsInProgress_tmp (
                workout_in_progress_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                firestoreId TEXT DEFAULT NULL,
                lastUpdated INTEGER DEFAULT NULL,
                synced INTEGER NOT NULL DEFAULT false,
                FOREIGN KEY(workoutId) REFERENCES workouts(workout_id) ON DELETE CASCADE
            )
        """.trimIndent()
        )

        // 2. Create index on workoutId
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutsInProgress_workoutId ON workoutsInProgress_tmp(workoutId)")

        // 3. Copy data from old table to new table
        db.execSQL(
            """
            INSERT INTO workoutsInProgress_tmp (
                workout_in_progress_id,
                workoutId,
                startTime,
                firestoreId,
                lastUpdated,
                synced
            )
            SELECT
                workout_in_progress_id,
                workoutId,
                startTime,
                firestoreId,
                lastUpdated,
                synced
            FROM workoutsInProgress
        """.trimIndent()
        )

        // 4. Drop old table
        db.execSQL("DROP TABLE workoutsInProgress")

        // 5. Rename new table to original name
        db.execSQL("ALTER TABLE workoutsInProgress_tmp RENAME TO workoutsInProgress")
    }
}