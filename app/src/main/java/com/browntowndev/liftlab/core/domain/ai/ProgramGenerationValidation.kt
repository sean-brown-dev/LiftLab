package com.browntowndev.liftlab.core.domain.ai

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramGenerationRequest
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramPayload
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ValidationIssue
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ValidationResult
import com.google.gson.Gson

private val DELT_VARIANTS = setOf(
    VolumeType.ANTERIOR_DELTOID,
    VolumeType.LATERAL_DELTOID,
    VolumeType.POSTERIOR_DELTOID
)

/**
 * Convert a set of VolumeTypes into validation group labels.
 * If any delt variants are present, collapse them into a single "DELTOID" label.
 */
private fun Set<VolumeType>.toSpecializedGroupLabels(): Set<String> {
    val labels = this.filterNot { it in DELT_VARIANTS }.map { it.name }.toMutableSet()
    if (this.any { it in DELT_VARIANTS }) {
        labels += "DELTOID"
    }
    return labels
}

/**
 * Validate a ProgramPayload. If invalid and a fixer is provided, use it to attempt an auto-correction.
 *
 * @param request ProgramGenerationRequest used to generate the plan.
 * @param expectedDays If non-null, workout count must match.
 * @param maxDayClusterRatio If set (0..1], flags “too much in one day” per group.
 * @param aiClient AI client for auto-correction.
 * @param gson Gson for JSON serialization.
 *
 * @return Pair<finalPayload, finalValidationResult>
 */
suspend fun ProgramPayload.validateAndTryCorrect(
    request: ProgramGenerationRequest,
    aiClient: AiClient,
    expectedDays: Int? = null,
    maxDayClusterRatio: Double? = 0.7,
    gson: Gson = Gson(),
): ProgramPayload {
    val initial = validateAgainst(request, expectedDays, maxDayClusterRatio)
    if (initial.isValid) {
        return this
    }

    // Build a surgical correction prompt for the small model
    val correctionPrompt = buildCorrectionPrompt(
        req = request,
        issues = initial.issues,
        originalJson = gson.toJson(this)
    )

    // Let the fixer return corrected JSON; then re-validate once.
    val corrected = aiClient.fix(gson.toJson(this), correctionPrompt)
    val correctedResult = corrected.validateAgainst(request, expectedDays, maxDayClusterRatio)
    if (!correctedResult.isValid) error("Fixer returned invalid ProgramPayload")

    return corrected
}

// ------------------------------ Core validation ------------------------------

fun ProgramPayload.validateAgainst(
    req: ProgramGenerationRequest,
    expectedDays: Int? = null,
    maxDayClusterRatio: Double? = 0.7
): ValidationResult {
    val catalog = req.liftCatalog
    val issues = mutableListOf<ValidationIssue>()

    // 1) Day count
    expectedDays?.let { days ->
        if (workouts.size != days) {
            issues += ValidationIssue("DAYS", "Expected $days training days but found ${workouts.size}.")
        }
    }

    // 2) Build lookups and aggregate fractional sets
    val liftById = catalog.associateBy { it.id }
    val volumeByType = mutableMapOf<VolumeType, Double>().withDefault { 0.0 }
    val dayVolumesByType = mutableMapOf<Int, MutableMap<VolumeType, Double>>() // for clustering

    workouts.forEachIndexed { dayIdx, w ->
        val dayMap = dayVolumesByType.getOrPut(dayIdx) { mutableMapOf<VolumeType, Double>().withDefault { 0.0 } }
        w.workoutLifts.forEach { wl ->
            val cat = liftById[wl.liftId]
            if (cat == null) {
                issues += ValidationIssue("CATALOG", "Workout '${w.name}' references unknown liftId=${wl.liftId}.")
                return@forEach
            }
            // Big compound should default to Wave (soft warning)
            val progressScheme = ProgressionScheme.valueOf(wl.progressionScheme)
            if (cat.movementPattern.isBigCompound() && progressScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                issues += ValidationIssue("SCHEME_HINT", "Compound lift id=${wl.liftId} (${cat.movementPattern}) should prefer WAVE_LOADING_PROGRESSION.")
            }

            // Rep / RPE rules
            val low = wl.repRangeBottom
            val high = wl.repRangeTop
            val rpe = wl.rpeTarget

            if (cat.movementPattern.isBigCompound()) {
                if (low < 6 || high > 10) {
                    issues += ValidationIssue("REPS", "Compound id=${wl.liftId} has reps $low–$high; expected 6–10.")
                }
                if ((low <= 5 || high <= 5) && rpe != 7f) {
                    issues += ValidationIssue("RPE", "Compound id=${wl.liftId} has ≤5 reps but RPE=$rpe; expected ~7.")
                }
            } else {
                if (low < 8 || high > 15) {
                    issues += ValidationIssue("REPS", "Accessory id=${wl.liftId} has reps $low–$high; expected 8–15.")
                }
                when (ProgressionScheme.valueOf(wl.progressionScheme)) {
                    ProgressionScheme.DOUBLE_PROGRESSION ->
                        if (rpe.toDouble() !in 7.5..8.5) issues += ValidationIssue("RPE", "Accessory + DOUBLE_PROGRESSION id=${wl.liftId} should be ~RPE 8; got $rpe.")
                    ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION ->
                        if (rpe.toDouble() !in 8.5..9.5) issues += ValidationIssue("RPE", "Accessory + DYNAMIC_DOUBLE_PROGRESSION id=${wl.liftId} should be ~RPE 9; got $rpe.")
                    else -> { /* Wave on accessories is rare but allowed */ }
                }
            }

            val sets = wl.setCount.toDouble()

            // primary volume = 1.0 per set
            cat.volumeTypesBitmask.toVolumeTypes().forEach { vt ->
                volumeByType[vt] = volumeByType.getValue(vt) + sets
                dayMap[vt] = dayMap.getOrDefault(vt, 0.0) + sets
            }
            // secondary volume = 0.5 per set
            cat.secondaryVolumeTypesBitmask?.toVolumeTypes()?.forEach { vt ->
                volumeByType[vt] = volumeByType.getValue(vt) + (sets * 0.5)
                dayMap[vt] = dayMap.getOrDefault(vt, 0.0) + (sets * 0.5)
            }
        }
    }

    // 3) Merge delts → DELTOID bucket
    val deltoidTotal = DELT_VARIANTS.sumOf { volumeByType.getOrDefault(it, 0.0) }

    val volumesByGroup = buildMap {
        VolumeType.entries.forEach { vt ->
            if (vt !in DELT_VARIANTS) put(vt.name, volumeByType.getOrDefault(vt, 0.0))
        }
        put("DELTOID", deltoidTotal)
    }

    // 4) Volume rules
    val canLag = setOf("FOREARM", "LOWER_BACK", "AB")
    val specializedGroups = req.specializationMuscles.toSpecializedGroupLabels()

    fun within10to20(x: Double) = x in 10.0..20.0
    fun atLeast18(x: Double) = x >= 18.0

    volumesByGroup.forEach { (group, sets) ->
        val isLagOk = group in canLag
        if (group in specializedGroups) {
            if (!atLeast18(sets)) issues += ValidationIssue("VOLUME_SPECIALIZE", "Specialized group $group is $sets; expected ≥18 (≈20 target).")
        } else {
            if (!isLagOk && !within10to20(sets)) {
                issues += ValidationIssue("VOLUME_BASELINE", "Group $group is $sets; expected 10–20.")
            }
            if (isLagOk && sets < 5.0) {
                issues += ValidationIssue("VOLUME_LAG_LOW", "Group $group is very low at $sets; allowed to lag but consider ≥5.")
            }
        }
    }

    // 5) Distribution / clustering (optional)
    if (maxDayClusterRatio != null) {
        val perGroupPerDay = mutableMapOf<String, MutableMap<Int, Double>>()
        dayVolumesByType.forEach { (dayIdx, mapByType) ->
            mapByType.forEach { (vt, v) ->
                val key = if (vt in DELT_VARIANTS) "DELTOID" else vt.name
                perGroupPerDay.getOrPut(key) { mutableMapOf() }[dayIdx] =
                    (perGroupPerDay.getOrPut(key) { mutableMapOf() }[dayIdx] ?: 0.0) + v
            }
        }
        perGroupPerDay.forEach { (group, byDay) ->
            val total = volumesByGroup[group] ?: 0.0
            if (total <= 0.0) return@forEach
            val maxDay = byDay.values.maxOrNull() ?: 0.0
            val ratio = maxDay / total
            if (ratio > maxDayClusterRatio) {
                issues += ValidationIssue(
                    "DISTRIBUTION",
                    "Group $group volume is clustered (max-day ${"%.1f".format(maxDay)} of ${"%.1f".format(total)} = ${"%.0f".format(ratio * 100)}%)."
                )
            }
        }
    }

    val hardFailKinds = setOf("DAYS", "CATALOG", "SCHEME", "VOLUME_SPECIALIZE", "VOLUME_BASELINE")
    return ValidationResult(isValid = issues.none { it.kind in hardFailKinds }, issues = issues, volumesByGroup = volumesByGroup)
}