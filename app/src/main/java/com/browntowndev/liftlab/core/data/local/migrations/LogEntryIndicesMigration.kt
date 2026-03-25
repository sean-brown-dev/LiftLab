package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val LogIndicesMigration = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- setLogEntries ----------------------------------------------------

        // 1) Drop the old single-column index on liftId if it exists
        //    (Room’s auto name for Index("liftId") is index_setLogEntries_liftId)
        db.execSQL("DROP INDEX IF EXISTS index_setLogEntries_liftId")

        // 2) Create the new composite index that matches your query filters
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_sle_lift_pos_flags_entry
            ON setLogEntries(
                liftId,
                setPosition,
                deleted,
                isDeload,
                workoutLogEntryId
            )
        """.trimIndent())

        // 3) Ensure the other declared indices exist (no-ops if already there)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_setLogEntries_workoutLogEntryId ON setLogEntries(workoutLogEntryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_setLogEntries_synced ON setLogEntries(synced)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_setLogEntries_remoteId ON setLogEntries(remoteId)")

        // --- workoutLogEntries ------------------------------------------------

        // 4) New helper index to make MAX(date)/date filters fast when deleted=0
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_wle_deleted_date
            ON workoutLogEntries(deleted, date)
        """.trimIndent())

        // 5) Ensure the other declared indices exist (no-ops if already there)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutLogEntries_historicalWorkoutNameId ON workoutLogEntries(historicalWorkoutNameId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workoutLogEntries_synced ON workoutLogEntries(synced)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workoutLogEntries_remoteId ON workoutLogEntries(remoteId)")
    }
}
