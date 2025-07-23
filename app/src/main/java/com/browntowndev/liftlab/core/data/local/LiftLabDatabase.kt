package com.browntowndev.liftlab.core.data.local

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.browntowndev.liftlab.BuildConfig
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.data.local.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.data.local.dao.LiftsDao
import com.browntowndev.liftlab.core.data.local.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.local.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.local.dao.SyncMetadataDao
import com.browntowndev.liftlab.core.data.local.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.data.local.entities.PreviousSetResultEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity
import com.browntowndev.liftlab.core.data.local.entities.SyncMetadataEntity
import com.browntowndev.liftlab.core.data.local.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.data.local.migrations.LiftNoteMigration
import com.browntowndev.liftlab.core.data.local.migrations.OneRepMaxAutoMigration
import com.browntowndev.liftlab.core.data.local.migrations.RemoteSyncAutoMigration
import com.browntowndev.liftlab.core.data.local.migrations.StepSizeAutoMigration
import com.browntowndev.liftlab.core.data.local.migrations.WorkoutInProgressMigration

@TypeConverters(Converters::class)
@Database(
    entities = [
        LiftEntity::class,
        CustomLiftSetEntity::class,
        HistoricalWorkoutNameEntity::class,
        PreviousSetResultEntity::class,
        ProgramEntity::class,
        WorkoutLogEntryEntity::class,
        SetLogEntryEntity::class,
        WorkoutEntity::class,
        WorkoutLiftEntity::class,
        WorkoutInProgressEntity::class,
        RestTimerInProgressEntity::class,
        LiftMetricChartEntity::class,
        VolumeMetricChartEntity::class,
        SyncMetadataEntity::class,
   ],
    version = 16,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = StepSizeAutoMigration::class),
        AutoMigration(from = 10, to = 11, spec = OneRepMaxAutoMigration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16, spec = RemoteSyncAutoMigration::class),
    ])
abstract class LiftLabDatabase : RoomDatabase() {
    abstract fun liftsDao(): LiftsDao
    abstract fun programsDao(): ProgramsDao
    abstract fun workoutsDao(): WorkoutsDao
    abstract fun workoutLiftsDao(): WorkoutLiftsDao
    abstract fun customSetsDao(): CustomSetsDao
    abstract fun previousSetResultsDao(): PreviousSetResultDao
    abstract fun workoutInProgressDao(): WorkoutInProgressDao
    abstract fun historicalWorkoutNamesDao(): HistoricalWorkoutNamesDao
    abstract fun workoutLogEntryDao(): WorkoutLogEntryDao
    abstract fun setLogEntryDao(): SetLogEntryDao
    abstract fun restTimerInProgressDao(): RestTimerInProgressDao
    abstract fun liftMetricChartsDao(): LiftMetricChartsDao
    abstract fun volumeMetricChartsDao(): VolumeMetricChartsDao
    abstract fun syncDao(): SyncMetadataDao

    /**
     * A custom callback to decouple the database creation from the data seeding logic.
     * This callback accepts a lambda (`onFirstCreate`) which will be executed only when
     * the database is created for the very first time.
     */
    class PopulateInitialDataCallback(
        private val onOpen: () -> Unit
    ) : Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            onOpen()
        }
    }

    companion object {
        const val DATABASE_NAME = "liftlab_database"
        @Volatile private var instance: LiftLabDatabase? = null

        fun getInstance(context: Context, populateInitialData: PopulateInitialDataCallback? = null): LiftLabDatabase {
            Log.d("LiftLabDatabase", "getInstance called. Current instance: $instance")
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext, populateInitialData).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, populateInitialData: PopulateInitialDataCallback? = null): LiftLabDatabase {
            val dbName = if (BuildConfig.USE_SCRATCH_DB) "scratch_$DATABASE_NAME" else DATABASE_NAME
            val db: LiftLabDatabase = Room
                .databaseBuilder(context, LiftLabDatabase::class.java, dbName)
                .addMigrations(LiftNoteMigration(), WorkoutInProgressMigration())
                .fallbackToDestructiveMigration(false).let {
                    if (populateInitialData != null) {
                        it.addCallback(populateInitialData)
                    } else {
                        it
                    }
                }
                .build()

            return db
        }
    }
}