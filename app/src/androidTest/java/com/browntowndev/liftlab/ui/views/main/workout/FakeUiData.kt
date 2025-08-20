package com.browntowndev.liftlab.ui.views.main.workout

import com.browntowndev.liftlab.ui.models.workoutLogging.*
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import kotlin.time.Duration.Companion.seconds

object FakeUiData {
    fun standardSet(position: Int, label: String) = LoggingStandardSetUiModel(
        position = position,
        rpeTarget = 8f,
        rpeTargetPlaceholder = "8",
        repRangeBottom = 8,
        repRangeTop = 10,
        weightRecommendation = 100f,
        hadInitialWeightRecommendation = true,
        previousSetResultLabel = "",
        repRangePlaceholder = "8-10",
        setNumberLabel = label,
        complete = false,
        completedWeight = null,
        completedReps = null,
        completedRpe = null,
        isNew = false,
    )

    fun myoSet(position: Int, myoPos: Int) = LoggingMyoRepSetUiModel(
        position = position,
        rpeTarget = 9f,
        rpeTargetPlaceholder = "9",
        repRangeBottom = null,
        repRangeTop = null,
        weightRecommendation = 80f,
        hadInitialWeightRecommendation = false,
        previousSetResultLabel = "",
        repRangePlaceholder = "3-5",
        setNumberLabel = "Myo ${myoPos + 1}",
        complete = false,
        completedWeight = null,
        completedReps = null,
        completedRpe = null,
        isNew = false,
        myoRepSetPosition = myoPos,
        setMatching = false,
        maxSets = null,
        repFloor = null,
    )

    fun lift(id: Long, name: String, position: Int, sets: List<LoggingSetUiModel>) =
        LoggingWorkoutLiftUiModel(
            id = id,
            liftId = id,
            liftName = name,
            liftMovementPattern = MovementPattern.CHEST_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            note = null,
            position = position,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = 90.seconds,
            restTimerEnabled = false,
            sets = sets
        )

    fun twoLiftsWithSets(): List<LoggingWorkoutLiftUiModel> =
        listOf(
            lift(101, "Incline DB Press", 0, listOf(standardSet(0, "Set 1"), standardSet(1, "Set 2"))),
            lift(202, "Lat Pulldown", 1, listOf(standardSet(0, "Set 1")))
        )

    fun singleLiftWithOneSet(id: Long): List<LoggingWorkoutLiftUiModel> =
        listOf(lift(id, "Bench Press", 0, listOf(standardSet(0, "Set 1"))))
}
