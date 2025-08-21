package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val RemoteSyncMigration = object: Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tablesToUpdate = listOf(
            "sets", "historicalWorkoutNames", "liftMetricCharts",
            "volumeMetricCharts", "previousSetResults", "programs", "setLogEntries",
            "workoutsInProgress", "workoutLogEntries", "workouts", "workoutLifts"
        )

        for (table in tablesToUpdate) {
            // Add the new 'deleted' column, which did not exist in version 15.
            // Defaulting to 0 (false) is crucial for existing rows.
            db.execSQL("ALTER TABLE $table ADD COLUMN deleted INTEGER NOT NULL DEFAULT false;")

            // Rename firestoreId to remoteId and lastUpdated to remoteLastUpdated
            db.execSQL("ALTER TABLE $table RENAME COLUMN firestoreId TO remoteId;")
            db.execSQL("ALTER TABLE $table RENAME COLUMN lastUpdated TO remoteLastUpdated;")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_synced` ON `$table` (`synced`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_remoteId` ON `$table` (`remoteId`)")
        }

        // --- Rebuild `lifts` table to remove `isHidden` and rename `firestoreId`, and `lastUpdated` ---
        db.execSQL("""
            CREATE TABLE lifts_new (
                lift_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                movementPattern TEXT NOT NULL,
                volumeTypesBitmask INTEGER NOT NULL,
                secondaryVolumeTypesBitmask INTEGER,
                restTime INTEGER,
                restTimerEnabled INTEGER NOT NULL DEFAULT true,
                incrementOverride REAL,
                isBodyweight INTEGER NOT NULL DEFAULT false,
                note TEXT,
                remoteId TEXT DEFAULT NULL,
                remoteLastUpdated INTEGER DEFAULT NULL,
                synced INTEGER NOT NULL DEFAULT false,
                deleted INTEGER NOT NULL DEFAULT false
            )
        """.trimIndent())

        // Copy data from the old table to the new one
        db.execSQL("""
            INSERT INTO lifts_new (lift_id, name, movementPattern, volumeTypesBitmask, secondaryVolumeTypesBitmask, restTime, restTimerEnabled, incrementOverride, isBodyweight, note, remoteId, remoteLastUpdated, synced, deleted)
            SELECT lift_id, name, movementPattern, volumeTypesBitmask, secondaryVolumeTypesBitmask, restTime, restTimerEnabled, incrementOverride, isBodyweight, note, firestoreId, lastUpdated, synced, isHidden FROM lifts
        """.trimIndent())

        // Drop the old table and rename the new one
        db.execSQL("DROP TABLE lifts")
        db.execSQL("ALTER TABLE lifts_new RENAME TO lifts")

        // Add indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lifts_movementPattern` ON `lifts` (`movementPattern`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lifts_lift_id_restTime` ON `lifts` (`lift_id`, `restTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lifts_synced` ON `lifts` (`synced`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lifts_remoteId` ON `lifts` (`remoteId`)")

        // --- Rebuild `restTimerInProgress` table to remove sync columns ---
        db.execSQL("""
            CREATE TABLE restTimerInProgress_new (
                rest_timer_in_progress_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timeStartedInMillis INTEGER NOT NULL,
                restTime INTEGER NOT NULL
            )
        """.trimIndent())

        // Copy data from the old table to the new one
        db.execSQL("""
            INSERT INTO restTimerInProgress_new (rest_timer_in_progress_id, timeStartedInMillis, restTime)
            SELECT rest_timer_in_progress_id, timeStartedInMillis, restTime FROM restTimerInProgress
        """.trimIndent())

        // Drop the old table and rename the new one
        db.execSQL("DROP TABLE restTimerInProgress")
        db.execSQL("ALTER TABLE restTimerInProgress_new RENAME TO restTimerInProgress")
    }
}