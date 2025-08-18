package com.browntowndev.liftlab.ui.models.workoutLogging

data class LoggingMyoRepSetUiModel(
    override val position: Int,
    override val rpeTarget: Float,
    override val rpeTargetPlaceholder: String,
    override val repRangeBottom: Int?,
    override val repRangeTop: Int?,
    override val weightRecommendation: Float?,
    override val hadInitialWeightRecommendation: Boolean,
    override val previousSetResultLabel: String,
    override val repRangePlaceholder: String,
    override val setNumberLabel: String,
    override val complete: Boolean = false,
    override  val completedWeight: Float? = null,
    override  val completedReps: Int? = null,
    override val completedRpe: Float? = null,
    override val isNew: Boolean = true,
    val myoRepSetPosition: Int? = null,
    val setMatching: Boolean = false,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
): LoggingSetUiModel
