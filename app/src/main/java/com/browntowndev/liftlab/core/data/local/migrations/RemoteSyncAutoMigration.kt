package com.browntowndev.liftlab.core.data.local.migrations

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@RenameColumn(tableName = "sets", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "historicalWorkoutNames", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "lifts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "liftMetricCharts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "volumeMetricCharts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "previousSetResults", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "programs", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "setLogEntries", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "workoutsInProgress", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "workoutLogEntries", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "workouts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "lifts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "workoutLifts", fromColumnName = "firestoreId", toColumnName = "remoteId")
@RenameColumn(tableName = "sets", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "historicalWorkoutNames", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "lifts", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "liftMetricCharts", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "volumeMetricCharts", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "previousSetResults", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "programs", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "setLogEntries", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "workoutsInProgress", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "workoutLogEntries", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "workouts", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@RenameColumn(tableName = "workoutLifts", fromColumnName = "lastUpdated", toColumnName = "remoteLastUpdated")
@DeleteColumn(tableName = "restTimerInProgress", columnName = "remoteId")
@DeleteColumn(tableName = "restTimerInProgress", columnName = "lastUpdated")
@DeleteColumn(tableName = "restTimerInProgress", columnName = "synced")
@DeleteColumn(tableName = "restTimerInProgress", columnName = "deleted")
class RemoteSyncAutoMigration: AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        db.execSQL("ALTER TABLE lifts DROP COLUMN isHidden")
    }
}