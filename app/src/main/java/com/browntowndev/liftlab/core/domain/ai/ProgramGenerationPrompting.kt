package com.browntowndev.liftlab.core.domain.ai

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramGenerationRequest
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ValidationIssue
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.google.firebase.ai.type.Schema


fun programResponseSchema(): Schema {
    // Leaf: workout lift (all required except deloadWeek/stepSize)
    val workoutLift = Schema.obj(mapOf(
        "liftId" to Schema.integer(),
        "progressionScheme" to Schema.string(), // WAVE_LOADING | DOUBLE_PROGRESSION | DYNAMIC_DOUBLE_PROGRESSION
        "position" to Schema.integer(),
        "setCount" to Schema.integer(),
        "deloadWeek" to Schema.integer(nullable = true),
        "rpeTarget" to Schema.float(),          // non-null per ProgramPayload
        "repRangeBottom" to Schema.integer(),   // non-null per ProgramPayload
        "repRangeTop" to Schema.integer(),      // non-null per ProgramPayload
        "stepSize" to Schema.integer(nullable = true)
    ))

    // Mid: workout with nested lifts
    val workout = Schema.obj(mapOf(
        "name" to Schema.string(),
        "position" to Schema.integer(),
        "workoutLifts" to Schema.array(workoutLift)
    ))

    // Program core (trimmed to exactly your model)
    val programCore = Schema.obj(mapOf(
        "name" to Schema.string(),
        "deloadWeek" to Schema.integer()
    ))

    // Weekly volume items (Double, not Int)
    val weeklyItem = Schema.obj(mapOf(
        "muscle" to Schema.string(),
        "primarySets" to Schema.float(),
        "secondarySets" to Schema.float()
    ))

    return Schema.obj(mapOf(
        "program" to programCore,
        "workouts" to Schema.array(workout),
        "weeklyVolumeSummary" to Schema.array(weeklyItem)
    ))
}

/**
 * Heuristic: which movement patterns count as "big compounds"
 * These should default to WAVE_LOADING_PROGRESSION.
 */
fun MovementPattern.isBigCompound(): Boolean = when (this) {
    // Lower body compounds
    MovementPattern.LEG_PUSH,
    MovementPattern.HIP_HINGE,

    // Upper body compounds (presses & rows/pulls)
    MovementPattern.HORIZONTAL_PUSH,
    MovementPattern.INCLINE_PUSH,
    MovementPattern.VERTICAL_PUSH,
    MovementPattern.HORIZONTAL_PULL,
    MovementPattern.VERTICAL_PULL -> true

    // Everything else is typically accessory/isolation
    MovementPattern.QUAD_ISO,
    MovementPattern.HAMSTRING_ISO,
    MovementPattern.CALVES,
    MovementPattern.CHEST_ISO,
    MovementPattern.TRICEP_ISO,
    MovementPattern.BICEP_ISO,
    MovementPattern.DELT_ISO,
    MovementPattern.GLUTE_ISO,
    MovementPattern.FOREARM_ISO,
    MovementPattern.TRAP_ISO,
    MovementPattern.AB_ISO -> false
}

/**
 * Simple name-based heuristics until you formalize equipment type on lifts.
 * If/when you add equipment metadata, replace these with explicit flags.
 */
private fun Lift.isQuickChangeWeight(): Boolean {
    val n = name.lowercase()
    // cable stacks / selectorized machines are fast to change
    return listOf("cable", "machine", "selector", "pin", "stack").any { it in n }
}

private fun Lift.isPlateSwapPainful(): Boolean {
    val n = name.lowercase()
    // barbell/DB/plate-loaded often require fiddly changes
    return listOf("barbell", "ez", "e-z", "db", "dumbbell", "plate", "loaded").any { it in n }
}

/**
 * Recommend a progression scheme based on movement pattern + equipment-change speed.
 * Rules:
 *  - Big compounds => WAVE_LOADING_PROGRESSION
 *  - Quick-change (cable/selectorized) => DYNAMIC_DOUBLE_PROGRESSION
 *  - Plate-swap annoying => DOUBLE_PROGRESSION
 *  - Default => DOUBLE_PROGRESSION
 */
fun suggestProgressionScheme(
    pattern: MovementPattern,
    isQuickChange: Boolean,
    isPlateSwapPainful: Boolean
): ProgressionScheme {
    if (pattern.isBigCompound()) return ProgressionScheme.WAVE_LOADING_PROGRESSION
    if (isQuickChange) return ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION
    if (isPlateSwapPainful) return ProgressionScheme.DOUBLE_PROGRESSION
    return ProgressionScheme.DOUBLE_PROGRESSION
}

/**
 * Human-readable notes the model will see: which patterns are "big compound"
 * vs accessory. Keep this synced with MovementPattern.isBigCompound().
 */
private fun buildMovementPatternHeuristicsContext(): String = """
    MOVEMENT PATTERN NOTES:
    - BIG COMPOUNDS include: LEG_PUSH, HIP_HINGE, HORIZONTAL_PUSH, INCLINE_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL.
    - Isolation/accessory patterns include: QUAD_ISO, HAMSTRING_ISO, CALVES, CHEST_ISO, TRICEP_ISO, BICEP_ISO, DELT_ISO, GLUTE_ISO, FOREARM_ISO, TRAP_ISO, AB_ISO.
    """.trim()

/**
 * Clear guidance for which scheme to choose and when, using your enum identifiers exactly.
 * We explicitly prohibit LINEAR_PROGRESSION here.
 */
private fun buildProgressionSchemeHeuristicsContext(): String = """
    PROGRESSION SCHEME HEURISTICS (use ONLY these enum values):
    - For large compound lifts (bench/squat-related, deadlift/hinge, overhead press, heavy rows/pulls): use WAVE_LOADING_PROGRESSION.
    - For accessories where swapping plates is annoying (EZ-bar skullcrushers, dumbbells, plate-loaded machines): use DOUBLE_PROGRESSION.
    - For exercises with very fast weight changes (cable stack, selectorized machines): use DYNAMIC_DOUBLE_PROGRESSION.
    - Do NOT use LINEAR_PROGRESSION.
    - If unsure: compounds → WAVE_LOADING_PROGRESSION; otherwise choose between DOUBLE_PROGRESSION (slow to change) and DYNAMIC_DOUBLE_PROGRESSION (fast to change).
    """.trim()

/**
 * Per-lift equipment hints by id, so the model can bias scheme choice for non-compounds.
 */
private fun buildEquipmentHintsContext(lifts: List<Lift>): String {
    val quick = lifts.filter { it.isQuickChangeWeight() }.map { it.id }.joinToString(",")
    val painful = lifts.filter { it.isPlateSwapPainful() }.map { it.id }.joinToString(",")

    return buildString {
        appendLine("EQUIPMENT HINTS:")
        appendLine("QUICK_WEIGHT_CHANGE_LIFTS=$quick")
        appendLine("PAINFUL_WEIGHT_CHANGE_LIFTS=$painful")
        appendLine("Rule: Prefer DYNAMIC_DOUBLE_PROGRESSION for QUICK, and DOUBLE_PROGRESSION for PAINFUL when the lift is not a big compound.")
    }
}

/**
 * Serialize the lift catalog into a compact context for the model.
 * Only enum names are used; volume types are shown as CSV for readability.
 */
private fun buildLiftCatalogContext(lifts: List<Lift>): String {
    return buildString {
        appendLine("LIFT CATALOG (use only these ids)")
        lifts.fastForEach { lift ->
            val primaryCsv = lift.volumeTypesBitmask.toVolumeTypes().joinToString(",") { it.name }
            val secondaryCsv = lift.secondaryVolumeTypesBitmask?.toVolumeTypes()?.joinToString(",") { it.name } ?: "None"
            appendLine(
                "id=${lift.id}; name=${lift.name}; movementPattern=${lift.movementPattern}; " +
                        "primaryVolume=$primaryCsv; secondaryVolume=$secondaryCsv"
            )
        }
    }
}

private fun buildVolumeTypeContext(): String {
    return buildString {
        appendLine("VOLUME TYPES (volume types to use for fractional set counting)")
        VolumeType.entries.fastForEach {
            appendLine("• ${it.name}")
        }
    }
}

/**
 * Main instruction block fed to the model (excluding the catalog text).
 * IMPORTANT: we require nested workoutLifts under each workout.
 *
 * The response schema is enforced separately via programResponseSchema().
 */
fun buildProgramPrompt(
    req: ProgramGenerationRequest,
): String {
    val specCsv = req.specializationMuscles.joinToString(",") { it.name }

    return """
        ${buildLiftCatalogContext(req.liftCatalog)}
        ${buildVolumeTypeContext()}
        
        SYSTEM:
        You are designing a hypertrophy program for an experienced lifter.
        
        Constraints:
        - Use ONLY lifts that appear in the LIFT CATALOG by their numeric id. Never invent new lifts or names.
        - Respect the WORKOUT_COUNT and distribute work across the microcycle accordingly.
          • For 3 workouts, favor full body split
          • For 4 workouts, favor upper/lower split
          • For 5 workouts, favor upper/lower/push/pull/legs split
          • For 6 workouts, favor push/pull/legs split
          • For any other workout count, do what you think is best.
        - Maximum of 8 sets for any given muscle group in a single workout
        - Weekly volume targets (no explicit dictionary provided):
          • Baseline: each muscle group, except for FOREARM, LOWER_BACK and AB, should receive 10–20 fractional sets per week.
          • FOREARM, LOWER_BACK and AB can go below 10 fractional sets per week.
          • Consider POSTERIOR_DELTOID, ANTERIOR_DELTOID and LATERAL_DELTOID as a single muscle group for volume accounting.
          • SPECIALIZE groups: bias exercise selection/order toward them and target ~20 sets/week.
          • Antagonists of a specialized group: intelligently reduce toward maintenance (~10 sets/week), not lower.
        - Progression schemes (use enum strings exactly): WAVE_LOADING_PROGRESSION, DOUBLE_PROGRESSION, DYNAMIC_DOUBLE_PROGRESSION. Do NOT use LINEAR_PROGRESSION.
        - Movement-pattern rule for big compounds:
          • LEG_PUSH, HIP_HINGE, HORIZONTAL_PUSH, INCLINE_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL → default to WAVE_LOADING_PROGRESSION unless there is a strong reason otherwise.
        - Weight-change ergonomics:
          • Quick to change (cable stack / selectorized) → prefer DYNAMIC_DOUBLE_PROGRESSION.
          • Annoying to change (EZ-bar, dumbbells, plate-loaded) → prefer DOUBLE_PROGRESSION.
        - Sets/reps/RPE:
          • Compounds: 6–10 reps; RPE ~8 (if 4–5 sets, set RPE 7).
          • Accessories/isolations: 8–15 reps; RPE 8 for DOUBLE_PROGRESSION, RPE 9 for DYNAMIC_DOUBLE_PROGRESSION.
        - Distribution:
          • Spread volume across the week; do not cluster a muscle’s entire weekly volume into one day unless the split requires it.
        - Deload frequency: every ${req.deloadEvery} weeks.
        - Output format:
          • Must be strict JSON matching the response schema. No markdown, no commentary.
          • Each workout must contain a workoutLifts array. Do NOT return a top-level workoutLifts.
          • Use enum names exactly as shown in the catalog and constraints.
        
        ${buildMovementPatternHeuristicsContext()}
        ${buildProgressionSchemeHeuristicsContext()}
        ${buildEquipmentHintsContext(lifts = req.liftCatalog)}
        
        USER INPUT:
        WORKOUT_COUNT=${req.microcycleWorkoutCount}
        SPECIALIZE=$specCsv
        """.trimIndent()
}

/**
 * Build a surgical correction prompt for a smaller model:
 * - Shows the original JSON (the model must minimally edit it),
 * - Lists issues to fix,
 * - Restates the key constraints, including nesting and enum requirements.
 */
fun buildCorrectionPrompt(
    req: ProgramGenerationRequest,
    issues: List<ValidationIssue>,
    originalJson: String
): String {
    val issuesBullet = issues.joinToString("\n") { "• [${it.kind}] ${it.message}" }

    // We restate your core rules here to avoid drift. Keep these in sync with your main prompt. (Ref: ProgramGenerationPrompting.kt) :contentReference[oaicite:1]{index=1}
    return """
        You are a correctness assistant. You will receive a JSON program and a list of validation issues.
        Your task is to MINIMALLY EDIT the JSON to resolve ALL issues while preserving the original intent.
        Do not invent lifts or fields. Keep all enum strings exactly as defined. Keep nested workoutLifts inside each workout.
        Return ONLY valid JSON matching the response schema. No markdown.
        
        Key constraints to satisfy:
        - Use ONLY lifts by id from the provided catalog. Do not introduce new lifts.
        - Respect the WORKOUT_COUNT and distribute work across the microcycle accordingly.
          • For 3 workouts, favor full body split
          • For 4 workouts, favor upper/lower split
          • For 5 workouts, favor upper/lower/push/pull/legs split
          • For 6 workouts, favor push/pull/legs split
          • For any other workout count, do what you think is best.
        - Weekly volume:
          • Baseline groups should be in the 10–20 fractional-set range.
          • Specialized groups should be ≈20 (≥18 acceptable).
          • FOREARM, LOWER_BACK, AB may be below 10 (but avoid 0 unless necessary).
          • Count ANTERIOR/LATERAL/POSTERIOR deltoids together as one "DELTOID" group for 10–20 validation.
        - Schemes allowed: WAVE_LOADING_PROGRESSION, DOUBLE_PROGRESSION, DYNAMIC_DOUBLE_PROGRESSION (NO LINEAR_PROGRESSION).
        - Big compounds (LEG_PUSH, HIP_HINGE, HORIZONTAL_PUSH, INCLINE_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL) should default to WAVE_LOADING_PROGRESSION.
        - Rep/RPE:
          • Compounds: 6–10 reps; if ≤5 reps appear, use RPE 7.
          • Accessories: 8–15 reps; DOUBLE_PROGRESSION ≈ RPE 8; DYNAMIC_DOUBLE_PROGRESSION ≈ RPE 9.
        - Distribution: Avoid clustering a muscle’s weekly volume into a single day unless split requires it.
        
        User input:
        WORKOUT_COUNT=${req.microcycleWorkoutCount}
        SPECIALIZE=${req.specializationMuscles.joinToString(",") { it.name }}
        
        Issues to fix:
        $issuesBullet
        
        Original JSON to correct:
        $originalJson
        """.trimIndent()
}
