package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val OneRepMaxIndexMigration = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Descending + partial index:
        // - Keys: (liftId, oneRepMax DESC)
        // - Predicate: WHERE deleted = 0
        // This lets SQLite read the max oneRepMax for each liftId
        // by taking the first index entry in that partition.
        db.execSQL("""
      CREATE INDEX IF NOT EXISTS idx_sle_lift_orm_desc_alive
      ON setLogEntries(liftId, oneRepMax DESC)
      WHERE deleted = 0
    """.trimIndent())
    }
}
