package com.browntowndev.liftlab.core.data.local.dao

import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.LiveWorkoutCompletedSetEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import java.util.Date

fun program(id: Long): ProgramEntity =
    ProgramEntity(id = id, name = "P$id").apply {
        remoteId = "RID-P-$id"; deleted = false; synced = true
    }

fun workout(id: Long, programId: Long): WorkoutEntity =
    WorkoutEntity(id = id, programId = programId, name = "W$id", position = 0).apply {
        remoteId = "RID-W-$id"; deleted = false; synced = true
    }

fun lift(id: Long): LiftEntity =
    LiftEntity(id = id, name = "L$id", movementPattern = MovementPattern.LEG_PUSH, volumeTypesBitmask = 1).apply {
        remoteId = "RID-L-$id"; deleted = false; synced = true
    }


fun program(
    id: Long,
    remoteId: String? = "RID-PROG-$id",
    name: String = "Program $id",
): ProgramEntity = ProgramEntity(
    id = id,
    name = name,
).apply {
    this.remoteId = remoteId
    this.deleted = false
    this.synced = true
}

fun workout(
    id: Long,
    programId: Long,
    name: String = "W$id",
    position: Int = 0,
    remoteId: String? = "RID-W-$id",
    deleted: Boolean = false,
    synced: Boolean = true,
): WorkoutEntity = WorkoutEntity(
    id = id,
    programId = programId,
    name = name,
    position = position,
).apply {
    this.remoteId = remoteId
    this.deleted = deleted
    this.synced = synced
}


fun liftMetricChart(
    id: Long,
    remoteId: String = "RID-LMC-$id",
    liftId: Long? = null,
    deleted: Boolean = false,
    synced: Boolean = true
): com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity {
    val e = com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity(
        id = id,
        liftId = liftId,
        chartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
    )
    return e.apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }
}

fun liveWorkoutCompletedSetEntity(
    id: Long,
    remoteId: String = "RID-LWCS-$id",
    workoutId: Long = 999L,
    liftId: Long = 1L,
    liftPosition: Int = 0,
    setPosition: Int = 0,
    deleted: Boolean = false,
    synced: Boolean = true
): LiveWorkoutCompletedSetEntity {
    val e = LiveWorkoutCompletedSetEntity(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        setType = SetType.STANDARD,
        liftPosition = liftPosition,
        setPosition = setPosition,
        myoRepSetPosition = null,
        weight = 100f,
        reps = 8,
        rpe = 8.0f,
        oneRepMax = 200,
        missedLpGoals = null,
        isDeload = false,
    )
    return e.apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }
}

fun workoutLogEntry(
    id: Long,
    remoteId: String = "RID-WLE-$id",
    dateEpoch: Long = System.currentTimeMillis(),
    deleted: Boolean = false,
    synced: Boolean = true
): WorkoutLogEntryEntity {
    val e = WorkoutLogEntryEntity(
        id = id,
        historicalWorkoutNameId = 1L,
        programWorkoutCount = 1,
        programDeloadWeek = 0,
        mesoCycle = 1,
        microCycle = 1,
        microcyclePosition = 1,
        date = Date(dateEpoch),
        durationInMillis = 60_000L,
    )
    return e.apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }
}
fun histWorkoutName(
    id: Long,
    programId: Long = 11L,
    workoutId: Long = 22L,
    deleted: Boolean = false,
    synced: Boolean = true
): com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity {
    val e = com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity(
        id = id,
        programId = programId,
        workoutId = workoutId,
        programName = "Program $programId",
        workoutName = "Workout $workoutId",
    )
    return e.apply {
        this.remoteId = "RID-HWN-$id"
        this.deleted = deleted
        this.synced = synced
    }
}

fun setLogEntry(
    id: Long,
    remoteId: String = "RID-SLE-$id",
    workoutLogEntryId: Long = 1000L,
    deleted: Boolean = false,
    synced: Boolean = true
): SetLogEntryEntity {
    val e = SetLogEntryEntity(
        id = id,
        workoutLogEntryId = workoutLogEntryId,
        liftId = 1L,
        workoutLiftDeloadWeek = null,
        liftName = "Lift $id",
        liftMovementPattern = MovementPattern.LEG_PUSH,
        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        setType = SetType.STANDARD,
        liftPosition = 0,
        setPosition = 0,
        myoRepSetPosition = null,
        repRangeTop = 10,
        repRangeBottom = 8,
        rpeTarget = 8.0f,
        weightRecommendation = null,
        weight = 100f,
        reps = 8,
        rpe = 8.0f,
        oneRepMax = 200,
        setMatching = null,
        maxSets = null,
        repFloor = null,
        dropPercentage = null,
        isDeload = false,
    )
    return e.apply {
        this.remoteId = remoteId
        this.deleted = deleted
        this.synced = synced
    }
}
