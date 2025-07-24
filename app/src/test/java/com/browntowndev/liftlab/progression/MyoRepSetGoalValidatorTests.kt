package com.browntowndev.liftlab.progression

import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.progression.MyoRepSetGoalValidator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result)
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(true, result)
    }

    @Test
    fun `returns false when RPE target is missed on activation set`() {
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = previouslyCompletedSets[0],
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result)
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result)
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(true, result)
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = listOf(justCompletedSet),
        )

        assertEquals(true, result)
    }

    @Test
    fun `returns false when activation set goals are not achieved`() {
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = listOf(justCompletedSet),
        )

        assertEquals(false, result)
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

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            myoRepSetResults = previouslyCompletedSets,
        )

        assertEquals(false, result)
    }
}