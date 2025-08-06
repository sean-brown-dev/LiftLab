package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class SetResultsMigration: Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS liveWorkoutCompletedSets (
                live_workout_completed_set_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId INTEGER NOT NULL,
                liftId INTEGER NOT NULL,
                setType TEXT NOT NULL,
                liftPosition INTEGER NOT NULL,
                setPosition INTEGER NOT NULL,
                myoRepSetPosition INTEGER,
                weight REAL NOT NULL,
                reps INTEGER NOT NULL,
                rpe REAL NOT NULL,
                oneRepMax INTEGER NOT NULL,
                missedLpGoals INTEGER,
                isDeload INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT false,
                deleted INTEGER NOT NULL DEFAULT false,
                remoteLastUpdated INTEGER DEFAULT NULL,
                remoteId TEXT DEFAULT NULL,
                FOREIGN KEY(workoutId) REFERENCES workouts(workout_id) ON DELETE CASCADE,
                FOREIGN KEY(liftId) REFERENCES lifts(lift_id) ON DELETE CASCADE
            )
        """.trimIndent())

        // 2. Indices (order matters for unique index)
        db.execSQL("""CREATE INDEX IF NOT EXISTS index_liveWorkoutCompletedSets_workoutId ON liveWorkoutCompletedSets(workoutId)""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS index_liveWorkoutCompletedSets_liftId ON liveWorkoutCompletedSets(liftId)""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS index_liveWorkoutCompletedSets_synced ON liveWorkoutCompletedSets(synced)""")
        db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS index_liveWorkoutCompletedSets_remoteId ON liveWorkoutCompletedSets(remoteId)""")

        // 3. Copy data from previousSetResults
        db.execSQL("""
            INSERT INTO liveWorkoutCompletedSets (
                live_workout_completed_set_id,
                workoutId,
                liftId,
                setType,
                liftPosition,
                setPosition,
                myoRepSetPosition,
                weight,
                reps,
                rpe,
                oneRepMax,
                missedLpGoals,
                isDeload,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            )
            SELECT
                previously_completed_set_id,
                workoutId,
                liftId,
                setType,
                liftPosition,
                setPosition,
                myoRepSetPosition,
                weight,
                reps,
                rpe,
                oneRepMax,
                missedLpGoals,
                isDeload,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            FROM previousSetResults
        """.trimIndent())

        // 4. Drop old table
        db.execSQL("DROP TABLE IF EXISTS previousSetResults")

        // 1. Create the new table (with removed columns)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS setLogEntries_tmp (
                set_log_entry_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutLogEntryId INTEGER NOT NULL,
                liftId INTEGER NOT NULL,
                workoutLiftDeloadWeek INTEGER,
                liftName TEXT NOT NULL,
                liftMovementPattern TEXT NOT NULL,
                progressionScheme TEXT NOT NULL,
                setType TEXT NOT NULL,
                liftPosition INTEGER NOT NULL,
                setPosition INTEGER NOT NULL,
                myoRepSetPosition INTEGER,
                repRangeTop INTEGER,
                repRangeBottom INTEGER,
                rpeTarget REAL NOT NULL,
                weightRecommendation REAL,
                weight REAL NOT NULL,
                reps INTEGER NOT NULL,
                rpe REAL NOT NULL,
                oneRepMax INTEGER NOT NULL DEFAULT 0,
                setMatching INTEGER,
                maxSets INTEGER,
                repFloor INTEGER,
                dropPercentage REAL,
                isDeload INTEGER NOT NULL DEFAULT 0,
                synced INTEGER NOT NULL DEFAULT false,
                deleted INTEGER NOT NULL DEFAULT false,
                remoteLastUpdated INTEGER DEFAULT NULL,
                remoteId TEXT DEFAULT NULL,
                FOREIGN KEY(workoutLogEntryId) REFERENCES workoutLogEntries(workout_log_entry_id) ON DELETE RESTRICT,
                FOREIGN KEY(liftId) REFERENCES lifts(lift_id) ON DELETE RESTRICT
            )
        """.trimIndent())

        // 2. Copy data (only the columns you want to keep, in the same order as above)
        db.execSQL("""
            INSERT INTO setLogEntries_tmp (
                set_log_entry_id,
                workoutLogEntryId,
                liftId,
                workoutLiftDeloadWeek,
                liftName,
                liftMovementPattern,
                progressionScheme,
                setType,
                liftPosition,
                setPosition,
                myoRepSetPosition,
                repRangeTop,
                repRangeBottom,
                rpeTarget,
                weightRecommendation,
                weight,
                reps,
                rpe,
                oneRepMax,
                setMatching,
                maxSets,
                repFloor,
                dropPercentage,
                isDeload,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            )
            SELECT
                set_log_entry_id,
                workoutLogEntryId,
                liftId,
                workoutLiftDeloadWeek,
                liftName,
                liftMovementPattern,
                progressionScheme,
                setType,
                liftPosition,
                setPosition,
                myoRepSetPosition,
                repRangeTop,
                repRangeBottom,
                rpeTarget,
                weightRecommendation,
                weight,
                reps,
                rpe,
                oneRepMax,
                setMatching,
                maxSets,
                repFloor,
                dropPercentage,
                isDeload,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            FROM setLogEntries
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE setLogEntries")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE setLogEntries_tmp RENAME TO setLogEntries")

        // 5. Recreate indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_setLogEntries_liftId ON setLogEntries(liftId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_setLogEntries_workoutLogEntryId ON setLogEntries(workoutLogEntryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_setLogEntries_synced ON setLogEntries(synced)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_setLogEntries_remoteId ON setLogEntries(remoteId)")

        // 1. Create new table with correct column names
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workoutLogEntries_tmp (
                workout_log_entry_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                historicalWorkoutNameId INTEGER NOT NULL,
                programWorkoutCount INTEGER NOT NULL,
                programDeloadWeek INTEGER NOT NULL,
                mesoCycle INTEGER NOT NULL,
                microCycle INTEGER NOT NULL,
                microcyclePosition INTEGER NOT NULL,
                date INTEGER NOT NULL,
                durationInMillis INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT false,
                deleted INTEGER NOT NULL DEFAULT false,
                remoteLastUpdated INTEGER DEFAULT NULL,
                remoteId TEXT DEFAULT NULL,
                FOREIGN KEY(historicalWorkoutNameId) REFERENCES historicalWorkoutNames(historical_workout_name_id) ON DELETE RESTRICT
            )
        """.trimIndent())

        // 2. Copy all data, mapping old column names to new ones
        db.execSQL("""
            INSERT INTO workoutLogEntries_tmp (
                workout_log_entry_id,
                historicalWorkoutNameId,
                programWorkoutCount,
                programDeloadWeek,
                mesoCycle,
                microCycle,
                microcyclePosition,
                date,
                durationInMillis,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            )
            SELECT
                workout_log_entry_id,
                historicalWorkoutNameId,
                programWorkoutCount,
                programDeloadWeek,
                mesocycle,           -- old name
                microcycle,          -- old name
                microcyclePosition,
                date,
                durationInMillis,
                synced,
                deleted,
                remoteId,
                remoteLastUpdated
            FROM workoutLogEntries
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE workoutLogEntries")

        // 4. Rename new table
        db.execSQL("ALTER TABLE workoutLogEntries_tmp RENAME TO workoutLogEntries")

        // 5. Recreate indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutLogEntries_historicalWorkoutNameId ON workoutLogEntries(historicalWorkoutNameId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutLogEntries_synced ON workoutLogEntries(synced)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workoutLogEntries_remoteId ON workoutLogEntries(remoteId)")
    }
}