package com.browntowndev.liftlab.core.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


class LiftNoteMigration: Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lifts ADD COLUMN note TEXT")

        db.execSQL("""
                    UPDATE lifts 
                    SET note = (
                        SELECT wl.note 
                        FROM workoutLifts wl 
                        WHERE wl.liftId = lifts.lift_id
                    )
                """.trimIndent())

        db.execSQL("""
            CREATE TABLE tmp_workoutLifts (
                workout_lift_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                workoutId INTEGER NOT NULL,
                liftId INTEGER NOT NULL,
                progressionScheme TEXT NOT NULL,
                position INTEGER NOT NULL,
                setCount INTEGER NOT NULL,
                deloadWeek INTEGER,
                rpeTarget REAL,
                repRangeBottom INTEGER,
                repRangeTop INTEGER,
                stepSize INTEGER,
                FOREIGN KEY(workoutId) REFERENCES workouts(workout_id) ON DELETE CASCADE,
                FOREIGN KEY(liftId) REFERENCES lifts(lift_id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO tmp_workoutLifts (
                workout_lift_id, 
                workoutId, 
                liftId, 
                progressionScheme, 
                position, 
                setCount, 
                deloadWeek, 
                rpeTarget, 
                repRangeBottom, 
                repRangeTop, 
                stepSize
            )
            SELECT 
                workout_lift_id, 
                workoutId, 
                liftId, 
                progressionScheme, 
                position, 
                setCount, 
                deloadWeek, 
                rpeTarget, 
                repRangeBottom, 
                repRangeTop, 
                stepSize
            FROM workoutLifts
        """.trimIndent())

        db.execSQL("DROP TABLE workoutLifts")

        // Rename the new 'workoutLifts' table to the old one
        db.execSQL("ALTER TABLE tmp_workoutLifts RENAME TO workoutLifts")
        db.execSQL("CREATE INDEX index_workoutLifts_liftId ON workoutLifts(liftId)")
        db.execSQL("CREATE INDEX index_workoutLifts_workoutId ON workoutLifts(workoutId)")
    }
}