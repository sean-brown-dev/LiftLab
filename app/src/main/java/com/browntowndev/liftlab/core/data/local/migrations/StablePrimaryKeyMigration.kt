package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val StablePrimaryKeyMigration = object: Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ===============================
        // 1) restTimerInProgress -> stable PK (id=1)
        // ===============================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS restTimerInProgress_new (
                rest_timer_in_progress_id INTEGER NOT NULL PRIMARY KEY,
                timeStartedInMillis INTEGER NOT NULL,
                restTime INTEGER NOT NULL
            )
        """.trimIndent())

        // Copy newest record (if any) to id=1
        db.execSQL("""
            INSERT INTO restTimerInProgress_new (rest_timer_in_progress_id, timeStartedInMillis, restTime)
            SELECT 1, timeStartedInMillis, restTime
            FROM restTimerInProgress
            ORDER BY COALESCE(timeStartedInMillis, 0) DESC
            LIMIT 1
        """.trimIndent())

        db.execSQL("DROP TABLE restTimerInProgress")
        db.execSQL("ALTER TABLE restTimerInProgress_new RENAME TO restTimerInProgress")

        // ===============================
        // 2) workoutsInProgress -> stable PK (id=1)
        // Keep FK to workouts(workout_id) and indices
        // ===============================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workoutsInProgress_new (
                workout_in_progress_id INTEGER NOT NULL PRIMARY KEY,
                workoutId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                remoteId TEXT DEFAULT NULL,
                remoteLastUpdated INTEGER DEFAULT NULL,
                synced INTEGER NOT NULL DEFAULT false,
                deleted INTEGER NOT NULL DEFAULT false,
                FOREIGN KEY(workoutId) REFERENCES workouts(workout_id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Carry over the most recent in-progress workout (if any) into id=1
        db.execSQL("""
            INSERT INTO workoutsInProgress_new (
                workout_in_progress_id, workoutId, startTime, remoteId, remoteLastUpdated, synced, deleted
            )
            SELECT
                1,
                workoutId,
                startTime,
                remoteId,
                remoteLastUpdated,
                synced,
                deleted
            FROM workoutsInProgress
            ORDER BY COALESCE(startTime, 0) DESC
            LIMIT 1
        """.trimIndent())

        // Swap tables
        db.execSQL("DROP TABLE workoutsInProgress")
        db.execSQL("ALTER TABLE workoutsInProgress_new RENAME TO workoutsInProgress")

        // Recreate indices (match your @Entity)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutsInProgress_workoutId ON workoutsInProgress(workoutId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutsInProgress_synced ON workoutsInProgress(synced)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workoutsInProgress_remoteId ON workoutsInProgress(remoteId)")
    }
}