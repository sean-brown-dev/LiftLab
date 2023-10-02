package com.browntowndev.liftlab.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import org.junit.Assert
import org.junit.Test

class MyoRepSetGoalValidatorTests {
    @Test
    fun `returns false when myorep set goals are not achieved`() {
         val justCompletedSet = LoggingMyoRepSetDto(
             setPosition = 0,
             myoRepSetPosition = 2,
             rpeTarget = 8f,
             repRangeBottom = 25,
             repRangeTop = 30,
             weightRecommendation = 100f,
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
            LoggingMyoRepSetDto(
                setPosition = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSetDto(
                setPosition = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            )
        )

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            previousMyoRepSets = previouslyCompletedSets,
        )

        Assert.assertEquals(false, result)
    }

    @Test
    fun `returns false when set matching myorep set goals are achieved`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            myoRepSetPosition = 2,
            setMatching = true,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
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
            LoggingMyoRepSetDto(
                setPosition = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSetDto(
                setPosition = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            )
        )

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            previousMyoRepSets = previouslyCompletedSets,
        )

        Assert.assertEquals(false, result)
    }

    @Test
    fun `returns true when set matching myorep set goals are not achieved`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            myoRepSetPosition = 2,
            setMatching = true,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
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
            LoggingMyoRepSetDto(
                setPosition = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSetDto(
                setPosition = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            )
        )

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            previousMyoRepSets = previouslyCompletedSets,
        )

        Assert.assertEquals(true, result)
    }

    @Test
    fun `returns true when activation set goals are achieved`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
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
            previousMyoRepSets = listOf(),
        )

        Assert.assertEquals(true, result)
    }

    @Test
    fun `returns false when activation set goals are not achieved`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
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
            previousMyoRepSets = listOf(),
        )

        Assert.assertEquals(false, result)
    }

    @Test
    fun `returns false when myorep set goals are achieved`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            myoRepSetPosition = 2,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
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
            LoggingMyoRepSetDto(
                setPosition = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSetDto(
                setPosition = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            )
        )

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            previousMyoRepSets = previouslyCompletedSets,
        )

        Assert.assertEquals(false, result)
    }

    @Test
    fun `returns false when incomplete set is passed in even if complete is true`() {
        val justCompletedSet = LoggingMyoRepSetDto(
            setPosition = 0,
            myoRepSetPosition = 2,
            rpeTarget = 8f,
            repRangeBottom = 25,
            repRangeTop = 30,
            weightRecommendation = 100f,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            repFloor = 5,
            complete = true,
        )

        val previouslyCompletedSets = listOf(
            LoggingMyoRepSetDto(
                setPosition = 0,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 30,
                completedRpe = 8f,
                completedWeight = 100f,
            ),
            LoggingMyoRepSetDto(
                setPosition = 0,
                myoRepSetPosition = 1,
                rpeTarget = 8f,
                repRangeBottom = 25,
                repRangeTop = 30,
                weightRecommendation = 100f,
                previousSetResultLabel = "",
                repRangePlaceholder = "",
                setNumberLabel = "",
                repFloor = 5,
                complete = true,
                completedReps = 20,
                completedRpe = 8f,
                completedWeight = 100f,
            )
        )

        val result = MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = justCompletedSet,
            previousMyoRepSets = previouslyCompletedSets,
        )

        Assert.assertEquals(false, result)
    }
}