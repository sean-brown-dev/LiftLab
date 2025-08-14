package com.browntowndev.liftlab.core.domain.useCase.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

class WeightCalculationUtilsTest {

    private val delta = 1e-3f

    /** Mirrors constants from the prod class for expectations. */
    private val pctAtOne = WeightCalculationUtils.PERCENTAGE_AT_ONE_REP
    private val pctAtTen = WeightCalculationUtils.PERCENTAGE_AT_TEN_REPS
    private val decay: Float = (ln((pctAtTen / pctAtOne).toDouble()) / (10.0 - 1.0)).toFloat()

    private fun expPct(repsAdj: Float): Float {
        // expected endurance %1RM with correct anchor
        return (pctAtOne * exp(decay * (repsAdj - 1f))).toFloat()
    }

    @Test
    fun `getOneRepMax - NSCA exact at integer reps`() {
        // 100 x 5 @10 (no RIR): NSCA 5 = 0.87 → e1RM ≈ 114.943 → rounds to 115
        val e1rm = WeightCalculationUtils.getOneRepMax(weight = 100f, reps = 5, rpe = 10f)
        assertEquals(115, e1rm)
    }

    @Test
    fun `getOneRepMax - NSCA interpolation at half reps`() {
        // 100 x 5 @9_5 (RIR 0_5): adjusted reps = 5.5
        // NSCA(5)=0.87, NSCA(6)=0.85 → midpoint 0.86 → e1RM ≈ 116.279 → rounds to 116
        val e1rm = WeightCalculationUtils.getOneRepMax(weight = 100f, reps = 5, rpe = 9.5f)
        assertEquals(116, e1rm)
    }

    @Test
    fun `getOneRepMax - exponential used for high reps and matches anchors`() {
        // Anchor checks via calculateSuggestedWeight path:
        // At 1 rep, expPct(1) == 1.0 → e1RM = weight / 1.0 = weight
        val e1rm1 = WeightCalculationUtils.getOneRepMax(weight = 150f, reps = 1, rpe = 10f)
        assertEquals(150, e1rm1)

        // At 10 reps, expPct(10) == 0.75 (but API uses NSCA for 10 exactly)
        // So 100 x 10 @10 → e1RM = 100 / 0.75 ≈ 133.333 → rounds to 133
        val e1rm10 = WeightCalculationUtils.getOneRepMax(weight = 100f, reps = 10, rpe = 10f)
        assertEquals(133, e1rm10)

        // At 10.5 reps, NSCA is not used; exponential kicks in.
        // e1RM = 100 / expPct(10.5)
        val repsAdj = 10.5f
        val expectedPct = expPct(repsAdj)
        val expectedE1rm = (100f / expectedPct).roundToInt()
        val e1rm105 = WeightCalculationUtils.getOneRepMax(weight = 100f, reps = 10, rpe = 9.5f) // 10 + (10 - 9.5) = 10.5
        assertEquals(expectedE1rm, e1rm105)
    }

    @Test
    fun `getOneRepMax - epsilon near 10 uses NSCA not exponential`() {
        // Simulate 10.0000001 after RPE math—your code rounds to 1 decimal first (→ 10.0)
        // Expect NSCA (0.75), not exponential.
        val reps = 10
        val rpe = 10f - 0.00001f // adjusted ≈ 10.00001 → rounds to 10.0
        val e1rm = WeightCalculationUtils.getOneRepMax(weight = 90f, reps = reps, rpe = rpe)
        // 90 / 0.75 = 120 → rounds to 120
        assertEquals(120, e1rm)
    }

    @Test
    fun `getOneRepMax - RPE adjustment works`() {
        // 100 x 5 @9 → adjusted reps = 5 + (10 - 9) = 6 → NSCA 6 = 0.85 → e1RM = 117.647 → 118
        val e1rm = WeightCalculationUtils.getOneRepMax(weight = 100f, reps = 5, rpe = 9f)
        assertEquals(118, e1rm)
    }

    @Test
    fun `calculateSuggestedWeight - NSCA target reps`() {
        // Prior set: 100 x 5 @10 → e1RM ≈ 114.943
        // Target: 5 @10 → target pct = 0.87 → suggested ≈ 100 (exactly original)
        val suggested = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 100f,
            completedReps = 5,
            completedRpe = 10f,
            repGoal = 5,
            rpeGoal = 10f,
            roundingFactor = 2.5f
        )
        // Should be 100, rounded down to nearest 2.5 stays 100
        assertEquals(100f, suggested, delta)
    }

    @Test
    fun `calculateSuggestedWeight - exponential target reps high`() {
        // Prior set: 60 x 12 @10 → e1RM via NSCA? 12 is >10 so exponential for source e1RM
        val completedWeight = 60f
        val completedReps = 12
        val completedRpe = 10f
        val e1rmExpected = completedWeight / expPct(12f)

        // Target: 20 reps @10 → use exponential percentage at 20
        val targetPct = expPct(20f)
        val rawSuggested = e1rmExpected * targetPct

        val suggested = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe,
            repGoal = 20,
            rpeGoal = 10f,
            roundingFactor = 2.5f
        )

        // Expect rounding down to nearest 2.5
        val expectedRoundedDown = (floor(rawSuggested / 2.5f) * 2.5f)
        assertEquals(expectedRoundedDown, suggested, 1e-2f)
    }

    @Test
    fun `calculateSuggestedWeight - zero reps`() {
        assertEquals(0f, WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 0f,
            completedReps = 10,
            completedRpe = 10f,
            repGoal = 10,
            rpeGoal = 10f,
            roundingFactor = 2.5f
        ), delta)

        assertEquals(0, WeightCalculationUtils.getOneRepMax(
            weight = 100f,
            reps = 0,
            rpe = 10f
        ))
    }

    @Test
    fun `calculateSuggestedWeight - zero weight`() {
        assertEquals(0f, WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 0f,
            completedReps = 10,
            completedRpe = 10f,
            repGoal = 10,
            rpeGoal = 10f,
            roundingFactor = 2.5f
        ), delta)

        assertEquals(0, WeightCalculationUtils.getOneRepMax(
            weight = 0f,
            reps = 10,
            rpe = 10f
        ))
    }
}
