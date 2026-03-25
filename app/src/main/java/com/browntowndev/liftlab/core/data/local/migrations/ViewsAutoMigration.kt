package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Handles the manual migration from database version 16 to 17.
 *
 * This migration was created to resolve a crash where Room's AutoMigration
 * attempted to `DROP` a view that did not exist in version 16.
 *
 * This migration explicitly creates all the new views that were added in version 17,
 * using `IF NOT EXISTS` to ensure the process is safe and robust.
 */
val ViewsAutoMigration = object: Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create LiveProgramView
        db.execSQL(
            """
            CREATE VIEW IF NOT EXISTS `LiveProgramView` AS SELECT * FROM programs WHERE deleted = 0
        """
        )

        // Create LiveWorkoutView
        db.execSQL(
            """
            CREATE VIEW IF NOT EXISTS `LiveWorkoutView` AS SELECT * FROM workouts WHERE deleted = 0
        """
        )

        // Create LiveWorkoutLiftView
        db.execSQL(
            """
            CREATE VIEW IF NOT EXISTS `LiveWorkoutLiftView` AS SELECT * FROM workoutLifts WHERE deleted = 0
        """
        )

        // Create LiveCustomLiftSetView
        db.execSQL(
            """
            CREATE VIEW IF NOT EXISTS `LiveCustomLiftSetView` AS SELECT * FROM sets WHERE deleted = 0
        """
        )

        // Create LiveLiftView
        db.execSQL(
            """
            CREATE VIEW IF NOT EXISTS `LiveLiftView` AS SELECT * FROM lifts WHERE deleted = 0
        """
        )
    }
}