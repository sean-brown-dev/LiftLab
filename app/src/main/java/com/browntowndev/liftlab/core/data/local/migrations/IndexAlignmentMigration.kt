package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object IndexAlignmentMigration : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- setLogEntries ---

        // Drop the old wide, write-expensive composite if present
        db.execSQL("DROP INDEX IF EXISTS idx_sle_lift_pos_flags_entry")

        // Create new, read-aligned indexes (idempotent)
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_sle_wle_deleted
            ON setLogEntries (workoutLogEntryId, deleted)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_sle_lift_pos_flags
            ON setLogEntries (liftId, setPosition, deleted, isDeload)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_sle_lift_deleted_orm
            ON setLogEntries (liftId, deleted, oneRepMax)
        """.trimIndent())

        // --- workoutLogEntries ---

        // You already have idx_wle_deleted_date; add meso/micro composite too.
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_wle_deleted_meso_micro
            ON workoutLogEntries (deleted, mesoCycle, microCycle)
        """.trimIndent())

        // Optional but helpful after index churn.
        db.query("PRAGMA analysis_limit=400").use { /* ignore rows */ }
        db.execSQL("ANALYZE")
        db.query("PRAGMA optimize").use { /* ignore rows */ }

    }
}
