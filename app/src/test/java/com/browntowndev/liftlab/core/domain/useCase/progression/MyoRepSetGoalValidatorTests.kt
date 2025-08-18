package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.useCase.utils.MyoRepSetGoalUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MyoRepSetGoalValidatorTests {
    @Test
    fun `returns false when rep floor is hit`() {
         val justCompletedSet = LoggingMyoRepSet(
             position = 0,
             myoRepSetPosition = 2,
             rpeTarget = 8f,
             repRangeBottom = 25,
             repRangeTop = 30,
             weightRecommendation = 100f,
             hadInitialWeightRecommendation = true,
             previousSetResultLabel = "",
             repRangePlaceholder = "",
             setNumberLabel = "",
             repFloor = 5,
             complete = true,
             completedReps = 5,
             completedRpe = 8f,
             completedWeight = 100f,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSet(
                position = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            justCompletedSet,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result.shouldContinueMyoReps)
    }

    @Test
    fun `returns true when rep floor not hit`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            myoRepSetPosition = 2,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
            completedReps = 6,
            completedRpe = 8f,
            completedWeight = 100f,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSet(
                position = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            justCompletedSet,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(true, result.shouldContinueMyoReps)
    }

    @Test
    fun `returns true to continue and true to activation set missed goal when RPE target is missed on activation set`() {
        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 25,
                completedRpe = 9f,
                completedWeight = 100f,
            ),
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = previouslyCompletedSets[0],
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(true, result.shouldContinueMyoReps)
        assertEquals(true, result.activationSetMissedGoal)
    }

    @Test
    fun `returns false when set matching myorep set goals are achieved`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            myoRepSetPosition = 2,
            setMatching = true,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
            completedReps = 10,
            completedRpe = 8f,
            completedWeight = 100f,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSet(
                position = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            justCompletedSet,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result.shouldContinueMyoReps)
    }

    @Test
    fun `returns true when set matching myorep set goals are not achieved`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            myoRepSetPosition = 1,
            setMatching = true,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
            completedReps = 8,
            completedRpe = 8f,
            completedWeight = 100f,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                setMatching = true,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSet(
                position = 0,
                myoRepSetPosition = 0,
                setMatching = true,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            justCompletedSet,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(true, result.shouldContinueMyoReps)
    }

    @Test
    fun `returns true when activation set goals are achieved`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
            completedReps = 30,
            completedRpe = 8f,
            completedWeight = 100f,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = listOf(justCompletedSet),
        )

        assertEquals(true, result.shouldContinueMyoReps)
        assertEquals(false, result.activationSetMissedGoal)
    }

    @Test
    fun `returns true for activation set failed goal and true for continue when activation set goals are not achieved`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
            completedReps = 24,
            completedRpe = 8f,
            completedWeight = 100f,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = listOf(justCompletedSet),
        )

        assertEquals(true, result.shouldContinueMyoReps)
        assertEquals(true, result.activationSetMissedGoal)
    }

    @Test
    fun `returns false when incomplete set is passed in even if complete is true`() {
        val justCompletedSet = LoggingMyoRepSet(
            position = 0,
            myoRepSetPosition = 2,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSet(
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSet(
                position = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                hadInitialWeightRecommendation = true,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            justCompletedSet,
        )

        val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
            lastMyoRepSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result.shouldContinueMyoReps)
    }
}