package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DoubleProgressionCalculatorTests {

    private lateinit var calculator: DoubleProgressionCalculator

    @BeforeEach
    fun setup() {
        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT) } returns DEFAULT_INCREMENT_AMOUNT
        every { SettingsManager.getSetting(REST_TIME, DEFAULT_REST_TIME) } returns DEFAULT_REST_TIME

        calculator = DoubleProgressionCalculator()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SettingsManager)
    }

    // -------------------- STANDARD (DEFAULT RULES) --------------------

    @Test
    fun `standard - increment when all goals are met (multi-set)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Back Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(80f, p.weightRecommendation) }
    }

    @Test
    fun `standard - duplicates ignored (de-dupe) and still increment when goals met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Hack Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
            // duplicate last entry should be ignored by distinctBy(setPosition)
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(80f, p.weightRecommendation) }
    }

    @Test
    fun `standard - NO increment when last set is missing (contiguity required)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 4, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Leg Press",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
            // missing setPosition == 3 -> must block progression
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(75f, p.weightRecommendation) }
    }

    @Test
    fun `standard - NO increment when contiguity broken (missing intermediate)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Machine Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            // missing setPosition == 1
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(75f, p.weightRecommendation) }
    }

    @Test
    fun `standard - increment when single-set passes ONLY top-set rule (final skipped by design)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 1, rpeTarget = 8f, repRangeTop = 10
            ),
            liftEntity = LiftEntity(
                name = "DB Curl",
                movementPattern = MovementPattern.BICEP_ISO,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            // RPE-adjusted meets the top-set goal
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 9, rpe = 7f, setType = SetType.STANDARD, isDeload = false)
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(80f, p.weightRecommendation) }
    }

    @Test
    fun `standard - NO increment when final set under top (multi-set)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Smith Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 7, rpe = 10f, setType = SetType.STANDARD, isDeload = false), // final < top
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(75f, p.weightRecommendation) }
    }

    @Test
    fun `standard - weight null when there is no set data`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 4, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Split Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            )
        )

        val data = emptyList<StandardSetResult>()
        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(null, p.weightRecommendation) }
    }

    // -------------------- CUSTOM (CONFIGURABLE PER-SET) --------------------
    // In the new model, custom sets are evaluated by each set’s own rule.

    @Test
    fun `custom - increment when all STANDARD custom set goals are met (per-set rules)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Press",
                movementPattern = MovementPattern.VERTICAL_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 1, rpeTarget = 9f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 2, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(80f, p.weightRecommendation) }
    }

    @Test
    fun `custom - increment when standard and DROP sets meet goals (drop math retained)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Bench",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.DROP_SET, position = 1, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8, dropPercentage = .1f),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 2, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 100f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 90f, reps = 8, rpe = 8f, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 100f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p ->
            if (p is LoggingStandardSet) assertEquals(105f, p.weightRecommendation) else assertEquals(95f, p.weightRecommendation)
        }
    }

    @Test
    fun `custom - increment with double DROP sets when all goals met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Bench",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.DROP_SET, position = 1, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8, dropPercentage = .1f),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.DROP_SET, position = 2, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8, dropPercentage = .1f),
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 100f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 90f, reps = 8, rpe = 8f, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 80f, reps = 8, rpe = 8f, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.forEachIndexed { i, p ->
            when (i) {
                0 -> assertEquals(105f, p.weightRecommendation)
                1 -> assertEquals(95f, p.weightRecommendation)
                2 -> assertEquals(85f, p.weightRecommendation)
            }
        }
    }

    @Test
    fun `custom - NO increment when last set fails (mixed standard+drop)`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Bench",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.DROP_SET, position = 1, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8, dropPercentage = .1f),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 2, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 100f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 90f, reps = 8, rpe = 8f, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 100f, reps = 7, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p ->
            if (p is LoggingStandardSet) assertEquals(100f, p.weightRecommendation) else assertEquals(90f, p.weightRecommendation)
        }
    }

    @Test
    fun `custom - NO increment when not all standard custom goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Press",
                movementPattern = MovementPattern.VERTICAL_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 1, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 2, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
            )
        )

        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 8, rpe = 9f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 7, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(75f, p.weightRecommendation) }
    }

    // ---------- MYO-REP: set matching (minis total reps >= activation reps) ----------

    @Test
    fun `custom myorep - increment when set matching enabled and minis sum greater or equal to activation reps`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 1, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Pulldown Myo",
                movementPattern = MovementPattern.VERTICAL_PULL,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 12,
                    repRangeTop = 15,
                    setGoal = 3,
                    setMatching = true
                )
            )
        )

        val data = listOf(
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 15, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 6,  rpe = 8f, isDeload = false, myoRepSetPosition = 0),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 5,  rpe = 8f, isDeload = false, myoRepSetPosition = 1),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 4,  rpe = 8f, isDeload = false, myoRepSetPosition = 2), // minis total = 15
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { r -> assertEquals(80f, r.weightRecommendation) }
    }

    @Test
    fun `custom myorep - NO increment when set matching enabled and minis sum less than activation reps`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 1, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Row Myo",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 12,
                    repRangeTop = 15,
                    setGoal = 3,
                    setMatching = true
                )
            )
        )

        val data = listOf(
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 60f, reps = 14, rpe = 8f, isDeload = false),
            // minis total = 13 < 14 -> fail
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 60f, reps = 6, rpe = 8f, isDeload = false, myoRepSetPosition = 0),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 60f, reps = 4, rpe = 8f, isDeload = false, myoRepSetPosition = 1),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 60f, reps = 3, rpe = 8f, isDeload = false, myoRepSetPosition = 2),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { r -> assertEquals(60f, r.weightRecommendation) }
    }

    @Test
    fun `custom myorep - de-dupes per (setPosition, miniPosition) so duplicate minis do not distort totals`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 1, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Curl Myo",
                movementPattern = MovementPattern.BICEP_ISO,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 12,
                    repRangeTop = 15,
                    setGoal = 3,
                    setMatching = true
                )
            )
        )

        val data = listOf(
            // Activation: 12 reps @8 RPE
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 40f, reps = 15, rpe = 8f, isDeload = false),
            // Minis: 6 + 6 = 12; add a duplicate of mini pos 1 which should be ignored by distinct key
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 40f, reps = 9, rpe = 8f, isDeload = false, myoRepSetPosition = 0),
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 40f, reps = 5, rpe = 8f, isDeload = false, myoRepSetPosition = 1),
            // duplicate of mini pos 1 (should be de-duped)
            MyoRepSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 40f, reps = 10, rpe = 8f, isDeload = false, myoRepSetPosition = 1),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { r -> assertEquals(40f, r.weightRecommendation) }
    }

    @Test
    fun `custom - weight null when there is no set data`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 2, repRangeTop = 8
            ),
            liftEntity = LiftEntity(
                name = "Anything",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.STANDARD, position = 0, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8),
                CustomLiftSetEntity(workoutLiftId = 0, type = SetType.MYOREP, position = 1, rpeTarget = 8f, repRangeBottom = 12, repRangeTop = 15, setGoal = 3, repFloor = 5),
            )
        )

        val data = emptyList<StandardSetResult>()
        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p -> assertEquals(null, p.weightRecommendation) }
    }

    // -------------- Recalculation ------------------ //
    @Test
    fun `standard - recalc when bottom set exceeds rep range top within allowed RPEs across the scheme`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(name = "Bench", movementPattern = MovementPattern.HORIZONTAL_PUSH, volumeTypesBitmask = 1)
        )
        // Exceed on top set at RPE 8, others meet their caps (9, 10)
        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 100f, reps = 8, rpe = 6f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 100f, reps = 8, rpe = 7f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 100f, reps = 8, rpe = 7.5f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)

        // Expect a recalculated value greater than 105
        result.fastForEach { p ->
            assert(p.weightRecommendation!! > 105f)
        }
    }

    @Test
    fun `standard - no recalc when intermediate exceeds top but violates RPE cap`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 3, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(name = "Row", movementPattern = MovementPattern.HORIZONTAL_PULL, volumeTypesBitmask = 1)
        )
        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 75f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
            // Exceeds reps but RPE 9.5 breaks the intermediate cap => should NOT trigger recalc
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 1, weight = 75f, reps = 9, rpe = 9.5f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 2, weight = 75f, reps = 8, rpe = 10f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        // Should remain the usual increment when goals are otherwise met
        result.fastForEach { p -> assertEquals(80f, p.weightRecommendation) }
    }

    @Test
    fun `single-set - exceeding top within cap triggers recalculation`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0, setCount = 1, rpeTarget = 8f, repRangeBottom = 6, repRangeTop = 8
            ),
            liftEntity = LiftEntity(name = "Curl", movementPattern = MovementPattern.BICEP_ISO, volumeTypesBitmask = 1)
        )
        val data = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, liftPosition = 0, setPosition = 0, weight = 50f, reps = 20, rpe = 8f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(lift.toCalculationDomainModel(), data, data, false)
        result.fastForEach { p ->
            assert(p.weightRecommendation!! > 55f)
        }
    }
}
