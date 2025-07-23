package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.data.local.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.data.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.data.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.domain.progression.CalculationEngine
import com.browntowndev.liftlab.core.domain.progression.ProgressionFactory
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.ui.models.LiftCompletionSummary
import com.browntowndev.liftlab.ui.models.WorkoutCompletionSummary
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration

class WorkoutViewModel(
    private val progressionFactory: ProgressionFactory,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepositoryImpl: WorkoutsRepositoryImpl,
    private val workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl,
    private val historicalWorkoutNamesRepositoryImpl: HistoricalWorkoutNamesRepositoryImpl,
    private val workoutLogRepository: WorkoutLogRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val liftsRepository: LiftsRepository,
    private val navigateToWorkoutHistory: () -> Unit,
    private val cancelRestTimer: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    init {
        initialize()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initialize() {
        val restTimerFlow = restTimerInProgressRepository.getFlow()
        programsRepository.getActiveProgramMetadataFlow()
            .flatMapLatest { programMetadata ->
                if (programMetadata == null) return@flatMapLatest flowOf(WorkoutState(initialized = true))
                val inProgressWorkoutFlow = workoutInProgressRepositoryImpl.getFlow(
                    programMetadata.currentMesocycle,
                    programMetadata.currentMicrocycle
                )
                val nextWorkoutToPerformFlow = getNextToPerformFlow(programMetadata)
                combine(
                    inProgressWorkoutFlow,
                    nextWorkoutToPerformFlow,
                ) { inProgressWorkout, nextWorkoutToPerform ->
                    val personalRecords = getPersonalRecords(
                        workoutId  = nextWorkoutToPerform?.id ?: 0L,
                        mesoCycle  = programMetadata.currentMesocycle,
                        microCycle = programMetadata.currentMicrocycle,
                        liftIds    = nextWorkoutToPerform?.lifts?.map { it.liftId }.orEmpty()
                    )
                    WorkoutState(
                        inProgressWorkout = inProgressWorkout,
                        programMetadata = programMetadata,
                        workout = nextWorkoutToPerform,
                        personalRecords = personalRecords,
                        initialized = true,
                    )
                }
            }.combine(restTimerFlow) { workoutState, restTimerInProgress ->
                workoutState.copy(
                    restTimerStartedAt = restTimerInProgress?.timeStartedInMillis?.toDate(),
                    restTime = restTimerInProgress?.restTime ?: 0L,
                )
            }
            .onEach { newState ->
                mutableWorkoutState.update { currentState ->
                    currentState.copy(
                        inProgressWorkout = newState.inProgressWorkout,
                        programMetadata = newState.programMetadata,
                        workout = newState.workout,
                        personalRecords = newState.personalRecords,
                        initialized = newState.initialized,
                        restTimerStartedAt = newState.restTimerStartedAt,
                        restTime = newState.restTime,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNextToPerformFlow(
        programMetadata: ActiveProgramMetadata
    ): Flow<LoggingWorkout?> {
        // alias your settings flows
        val useAllWorkoutDataFlow = SettingsManager.getSettingFlow(
            USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
        )
        val useOnlySamePositionFlow = SettingsManager.getSettingFlow(
            ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
            DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
        )
        val useLiftSpecificDeloadingFlow = SettingsManager.getSettingFlow(
            LIFT_SPECIFIC_DELOADING,
            DEFAULT_LIFT_SPECIFIC_DELOADING
        )

        return workoutsRepositoryImpl
            .getByMicrocyclePosition(
                programId = programMetadata.programId,
                microcyclePosition = programMetadata.currentMicrocyclePosition
            )
            .flatMapLatest { nullableWorkout ->
                if (nullableWorkout == null) {
                    flowOf(null)  // no workoutEntity, short‐circuit
                } else {
                    // 1) Build the two “result” flows for this workoutEntity
                    val previousResultsFlow = getSetResults(
                        workout = nullableWorkout,
                        programMetadata = programMetadata,
                        useAllData = /* we'll plug this in below */
                            /* dummy—will be overridden by combine */
                            false
                    )
                    val inProgressResultsFlow = setResultsRepository.getForWorkoutFlow(
                        workoutId = nullableWorkout.id,
                        mesoCycle = programMetadata.currentMesocycle,
                        microCycle = programMetadata.currentMicrocycle
                    )

                    // 2) Combine *all* five Flows
                    combine(
                        useAllWorkoutDataFlow,
                        useOnlySamePositionFlow,
                        useLiftSpecificDeloadingFlow,
                        previousResultsFlow,
                        inProgressResultsFlow
                    ) { useAll, onlySamePos, liftDeloadEnabled, previousResults, inProgressResults ->
                        val inProgressResultsMap = inProgressResults.associateBy { r ->
                            "${r.liftId}-${r.setPosition}-${(r as? MyoRepSetResult)?.myoRepSetPosition}"
                        }
                        val previousResultsForDisplay = getNewestResultsFromOtherWorkouts(
                            liftIdsToSearchFor = nullableWorkout.lifts.map { it.liftId },
                            workout = nullableWorkout,
                            existingResultsForOtherLifts = emptyList(),
                            includeDeload = true
                        )

                        // run your progression calculation
                        progressionFactory.calculate(
                            workout = nullableWorkout,
                            previousSetResults = previousResults,
                            previousResultsForDisplay = previousResultsForDisplay,
                            inProgressSetResults = inProgressResultsMap,
                            programDeloadWeek = programMetadata.deloadWeek,
                            useLiftSpecificDeloading = liftDeloadEnabled,
                            microCycle = programMetadata.currentMicrocycle,
                            onlyUseResultsForLiftsInSamePosition = onlySamePos
                        ).let { calc ->
                            getMergedWithPartiallyCompletedSets(workout = calc)
                        }
                    }
                }
            }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> mutableWorkoutState.update {
                if (mutableWorkoutState.value.isCompletionSummaryVisible) {
                    it.copy(isCompletionSummaryVisible = false)
                } else {
                    it.copy(workoutLogVisible = false)
                }
            }
            TopAppBarAction.RestTimerCompleted -> {
                executeInTransactionScope {
                    restTimerInProgressRepository.deleteAll()
                    mutableWorkoutState.update {
                        it.copy(restTimerStartedAt = null)
                    }
                }
            }
            TopAppBarAction.FinishWorkout -> if (mutableWorkoutState.value.isCompletionSummaryVisible) {
                finishWorkout()
            } else {
                toggleCompletionSummary()
            }
            TopAppBarAction.OpenWorkoutHistory -> navigateToWorkoutHistory()
            else -> {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getSetResults(
        workout: Workout?,
        programMetadata: ActiveProgramMetadata,
        useAllData: Boolean,
    ): Flow<List<SetResult>> {
        if (workout == null) return flowOf(emptyList())
        return setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(
            workoutId = workout.id,
            mesoCycle = programMetadata.currentMesocycle,
            microCycle = programMetadata.currentMicrocycle,
        ).mapLatest { resultsFromLastWorkout ->
            if (useAllData) {
                getResultsWithAllWorkoutDataAppended(
                    workout = workout,
                    resultsFromLastWorkout = resultsFromLastWorkout
                )
            } else resultsFromLastWorkout
        }
    }

    private suspend fun getResultsWithAllWorkoutDataAppended(
        workout: Workout,
        resultsFromLastWorkout: List<SetResult>,
    ): List<SetResult> {
        val liftIdsOfResults = resultsFromLastWorkout.map { it.liftId }.toHashSet()
        val liftIdsToSearchFor = workout.lifts
            .filter { !liftIdsOfResults.contains(it.liftId) }
            .map { workoutLift -> workoutLift.liftId }

        return getNewestResultsFromOtherWorkouts(
            workout = workout,
            liftIdsToSearchFor = liftIdsToSearchFor,
            existingResultsForOtherLifts = resultsFromLastWorkout,
            includeDeload = false,
        )
    }

    private suspend fun getNewestResultsFromOtherWorkouts(
        workout: Workout,
        liftIdsToSearchFor: List<Long>,
        existingResultsForOtherLifts: List<SetResult>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.liftId }
                .toHashSet()

            existingResultsForOtherLifts.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    workoutLogRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        linearProgressionLiftIds = linearProgressionLiftIds,
                        includeDeload = includeDeload,
                    )

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResultsForOtherLifts
    }

    private fun getMergedWithPartiallyCompletedSets(workout: LoggingWorkout): LoggingWorkout {
        val partiallyCompletedSets = mutableWorkoutState.value.workout?.lifts?.fastMapNotNull { lift ->
            val incompleteSetsForLift = lift.sets.filter { set ->
                !set.complete &&
                        (set.completedRpe != null ||
                                set.completedReps != null ||
                                set.completedWeight != null)
            }
            if (incompleteSetsForLift.isNotEmpty()) {
                Pair("${lift.id}-${lift.liftId}", incompleteSetsForLift)
            } else null
        }?.associate { it.first to it.second }

        var mergedWorkout: LoggingWorkout = workout
        if (partiallyCompletedSets?.isNotEmpty() == true) {
            mergedWorkout = workout.copy(
                lifts = workout.lifts.fastMap { lift ->
                    lift.copy(
                        sets = lift.sets.fastMap { set ->
                            val incompleteSet = partiallyCompletedSets["${lift.id}-${lift.liftId}"]
                                ?.find { incSet -> incSet.position == set.position }

                            if (incompleteSet != null) {
                                when (set) {
                                    is LoggingStandardSet -> set.copy(
                                        completedRpe = incompleteSet.completedRpe,
                                        completedReps = incompleteSet.completedReps,
                                        completedWeight = incompleteSet.completedWeight,
                                    )
                                    is LoggingDropSet -> set.copy(
                                        completedRpe = incompleteSet.completedRpe,
                                        completedReps = incompleteSet.completedReps,
                                        completedWeight = incompleteSet.completedWeight,
                                    )
                                    is LoggingMyoRepSet -> set.copy(
                                        completedRpe = incompleteSet.completedRpe,
                                        completedReps = incompleteSet.completedReps,
                                        completedWeight = incompleteSet.completedWeight,
                                    )
                                    else -> throw Exception("${set::class.simpleName} is not defined.")
                                }
                            } else set
                        }
                    )
                }
            )
        }

        return mergedWorkout
    }

    private suspend fun getPersonalRecords(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>
    ): Map<Long, PersonalRecordDto> {
        val prevWorkoutPersonalRecords = setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(
            workoutId = workoutId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftIds = liftIds,
        )
        return workoutLogRepository.getPersonalRecordsForLifts(liftIds)
            .associateBy { it.liftId }
            .toMutableMap()
            .apply {
                prevWorkoutPersonalRecords.fastForEach { prevWorkoutPr ->
                    get(prevWorkoutPr.liftId)?.let { allWorkoutsPr ->
                        if (allWorkoutsPr.personalRecord < prevWorkoutPr.personalRecord) {
                            put(prevWorkoutPr.liftId, prevWorkoutPr)
                        }
                    } ?: put(prevWorkoutPr.liftId, prevWorkoutPr)
                }
            }
    }

    fun toggleReorderLifts() {
        mutableWorkoutState.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) {
        executeInTransactionScope {
            val newWorkoutLiftIndices = newLiftOrder
                .mapIndexed { index, item -> item.key to index }
                .associate { it.first to it.second }

            val updatedWorkoutCopy = mutableWorkoutState.value.workout!!.copy(
                lifts = mutableWorkoutState.value.workout!!.lifts.map { lift ->
                    lift.copy(position = newWorkoutLiftIndices[lift.id]!!)
                }
            )

            val workoutId = mutableWorkoutState.value.workout!!.id
            val updatedLifts = workoutLiftsRepositoryImpl.getForWorkout(workoutId)
                .map {
                    when (it) {
                        is StandardWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                        is CustomWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                        else -> throw Exception("${it::class.simpleName} is not defined.")
                    }
                }
            workoutLiftsRepositoryImpl.updateMany(updatedLifts)

            val workoutLiftIdByLiftId = mutableWorkoutState.value.workout!!.lifts.associate { it.liftId to it.id }
            val updatedInProgressWorkoutCopy = mutableWorkoutState.value.inProgressWorkout!!.let { inProgressWorkout ->
                inProgressWorkout.copy(
                    completedSets = inProgressWorkout.completedSets.fastMap { completedSet ->
                        val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                        when (completedSet) {
                            is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                        }
                    }
                )
            }

            val updatedInProgressSetResults = setResultsRepository.getForWorkout(
                workoutId = workoutId,
                mesoCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
                microCycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle
            ).map { completedSet ->
                val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                when (completedSet) {
                    is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                }
            }
            setResultsRepository.upsertMany(updatedInProgressSetResults)

            mutableWorkoutState.update {
                it.copy(
                    workout = updatedWorkoutCopy,
                    inProgressWorkout = updatedInProgressWorkoutCopy,
                    isReordering = false
                )
            }
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }

    fun toggleDeloadPrompt() {
        mutableWorkoutState.update {
            it.copy(isDeloadPromptDialogShown = !it.isDeloadPromptDialogShown)
        }
    }

    fun skipDeloadMicrocycle(): Job {
        mutableWorkoutState.update {
            it.copy(isDeloadPromptDialogShown = false)
        }
        return executeInTransactionScope {
            val programMetadata = mutableWorkoutState.value.programMetadata!!
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = programMetadata.currentMesocycle + 1,
                microCycle = 0,
                microCyclePosition = 0,
            )
        }
    }

    fun showDeloadPromptOrStartWorkout() {
        if (mutableWorkoutState.value.isDeloadWeek &&
            mutableWorkoutState.value.programMetadata!!.currentMicrocyclePosition == 0) {
            mutableWorkoutState.update {
                it.copy(isDeloadPromptDialogShown = true)
            }
        } else {
            startWorkout()
        }
    }

    fun startWorkout() {
        executeInTransactionScope {
            val inProgressWorkout = WorkoutInProgress(
                startTime = getCurrentDate(),
                workoutId = mutableWorkoutState.value.workout!!.id,
                completedSets = listOf(),
            )
            workoutInProgressRepositoryImpl.insert(inProgressWorkout)
            mutableWorkoutState.update {
                it.copy(
                    inProgressWorkout = inProgressWorkout,
                    workoutLogVisible = true,
                    isDeloadPromptDialogShown = false,
                )
            }
        }
    }

    private fun getTempFileFromBitmap(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        return file
    }

    fun shareWorkoutSummary(context: Context, workoutSummaryBitmap: Bitmap) {
        val shareUri: Uri = FileProvider.getUriForFile(
            context,
            "com.browntowndev.liftlab.fileprovider",
            getTempFileFromBitmap(
                context = context,
                bitmap = workoutSummaryBitmap,
                fileName = "workoutSummary.png"
            )
        )
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share WorkoutEntity Results"))
    }

    fun toggleCompletionSummary() {
        mutableWorkoutState.update {
            it.copy(
                isCompletionSummaryVisible = !it.isCompletionSummaryVisible,
                workoutCompletionSummary = if (!it.isCompletionSummaryVisible) {
                    getWorkoutCompletionSummary()
                } else null
            )
        }
    }

    private fun getWorkoutCompletionSummary(): WorkoutCompletionSummary {
        val liftsById = mutableWorkoutState.value.workout!!.lifts.associateBy { it.liftId }
        val personalRecords = mutableWorkoutState.value.personalRecords
        val liftEntityCompletionSummaries = mutableWorkoutState.value.inProgressWorkout?.completedSets
            ?.groupBy { "${it.liftId}-${it.liftPosition}" }
            ?.values?.map { resultsForLift ->
                val lift = liftsById[resultsForLift[0].liftId]
                val setsCompleted = resultsForLift.size
                val totalSets = (lift?.setCount ?: setsCompleted)
                    .let { total ->
                        // Myo can meet this condition
                        if (setsCompleted > total) setsCompleted else total
                    }
                var bestSet1RM: Int? = null
                var bestSet: SetResult? = null
                resultsForLift.fastForEach { result ->
                    val oneRepMax = CalculationEngine.getOneRepMax(
                        weight = if (result.weight > 0) result.weight else 1f,
                        reps = result.reps,
                        rpe = result.rpe
                    )
                    if (bestSet1RM == null || oneRepMax > bestSet1RM!!) {
                        bestSet = result
                        bestSet1RM = oneRepMax
                    }
                }

                LiftCompletionSummary(
                    liftName = lift?.liftName ?: "Unknown LiftEntity",
                    liftId = lift?.liftId ?: -1,
                    liftPosition = lift?.position ?: -1,
                    setsCompleted = setsCompleted,
                    totalSets = totalSets,
                    bestSetReps = bestSet?.reps ?: 0,
                    bestSetWeight = bestSet?.weight ?: 0f,
                    bestSetRpe = bestSet?.rpe ?: 0f,
                    bestSet1RM = bestSet1RM ?: 0,
                    isNewPersonalRecord = personalRecords[lift?.liftId]?.let {
                        it.personalRecord < (bestSet1RM ?: -1)
                    } ?: false
                )
            }?.toMutableList()?.apply {
                val liftsWithNoCompletedSets = liftsById.values.filter { loggingLift ->
                    !this.fastAny { summaryLift ->
                        summaryLift.liftId == loggingLift.liftId && summaryLift.liftPosition == loggingLift.position
                    }
                }

                addAll(
                    liftsWithNoCompletedSets.map { incompleteLift ->
                        LiftCompletionSummary(
                            liftName = incompleteLift.liftName,
                            liftId = incompleteLift.liftId,
                            liftPosition = incompleteLift.position,
                            setsCompleted = 0,
                            totalSets = incompleteLift.setCount,
                            bestSetReps = 0,
                            bestSetWeight = 0f,
                            bestSetRpe = 0f,
                            bestSet1RM = 0,
                            isNewPersonalRecord = false,
                        )
                    }
                )
            }?.sortedBy { it.liftPosition } ?: listOf()

        return WorkoutCompletionSummary(
            workoutName = mutableWorkoutState.value.workout!!.name,
            liftCompletionSummaries = liftEntityCompletionSummaries
        )
    }

    fun finishWorkout() {
        toggleCompletionSummary()

        executeInTransactionScope {
            val startTimeInMillis = mutableWorkoutState.value.inProgressWorkout!!.startTime.time
            val durationInMillis = (getCurrentDate().time - startTimeInMillis)
            val programMetadata = mutableWorkoutState.value.programMetadata!!
            val workout = mutableWorkoutState.value.workout!!

            // Remove the workoutEntity from in progress
            workoutInProgressRepositoryImpl.delete()
            restTimerInProgressRepository.deleteAll()

            // Increment the mesocycle and microcycle
            val microCycleComplete =  (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
            val liftLevelDeloadsEnabled = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
            val deloadWeekComplete = !liftLevelDeloadsEnabled && microCycleComplete && mutableWorkoutState.value.isDeloadWeek
            val newMesoCycle = if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
            val newMicroCycle = if (deloadWeekComplete) 0 else if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
            val newMicroCyclePosition = if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = newMesoCycle,
                microCycle = newMicroCycle,
                microCyclePosition = newMicroCyclePosition,
            )

            // Get/create the historical workoutEntity name entry then use it to insert a workoutEntity log entry
            var historicalWorkoutNameId =
                historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                )
            if (historicalWorkoutNameId == null) {
                historicalWorkoutNameId = historicalWorkoutNamesRepositoryImpl.insert(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                    programName = programMetadata.name,
                    workoutName = workout.name,
                )
            }
            val workoutLogEntryId = workoutLogRepository.insertWorkoutLogEntry(
                historicalWorkoutNameId = historicalWorkoutNameId,
                programDeloadWeek = programMetadata.deloadWeek,
                programWorkoutCount = programMetadata.workoutCount,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
                microcyclePosition = programMetadata.currentMicrocyclePosition,
                date = getCurrentDate(),
                durationInMillis = durationInMillis,
            )

            moveSetResultsToLogHistory(
                workoutLogEntryId = workoutLogEntryId,
                programMetadata = programMetadata,
                workout = workout
            )

            // Update any Linear Progression failures
            // The reason this is done when the workoutEntity is completed is because if it were done on the fly
            // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
            // and then you get double increment. Or any variation of them going between success/failure by
            // modifying results.
            updateLinearProgressionFailures()

            stopRestTimer()

            mutableWorkoutState.update {
                it.copy(workoutLogVisible = false)
            }
        }
    }

    private suspend fun moveSetResultsToLogHistory(
        workoutLogEntryId: Long,
        programMetadata: ActiveProgramMetadata,
        workout: LoggingWorkout
    ) {
        val liftsAndPositions = mutableWorkoutState.value.workout!!.lifts.associate {
            it.liftId to it.position
        }

        // If any lifts were changed and had completed results do not copy them
        val excludeFromCopy =
            mutableWorkoutState.value.inProgressWorkout!!.completedSets.filter { result ->
                val liftPosition = liftsAndPositions[result.liftId]
                liftPosition != result.liftPosition
            }.map {
                it.id
            }

        // Copy all of the set results from this workoutEntity into the set history table
        workoutLogRepository.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = mutableWorkoutState.value.workout!!.id,
            mesocycle = programMetadata.currentMesocycle,
            microcycle = programMetadata.currentMicrocycle,
            excludeFromCopy = excludeFromCopy,
        )

        // Get all the set results for deloaded lifts
        val deloadSetResults = mutableWorkoutState.value.workout!!.lifts
            .filter { workoutLift ->
                // workoutEntity lifts whose deload week it is
                val deloadWeek = (workoutLift.deloadWeek ?: programMetadata.deloadWeek) - 1
                deloadWeek == programMetadata.currentMicrocycle
            }.fastMap {
                // key that can be used to match set results
                "${it.liftId}-${it.position}"
            }.toHashSet().let { deloadedWorkoutLiftIds ->
                // set results for deloaded workoutEntity lifts
                mutableWorkoutState.value.inProgressWorkout!!.completedSets
                    .filter { deloadedWorkoutLiftIds.contains("${it.liftId}-${it.liftPosition}") }
                    .fastMap { it.id }
            }

        // Delete all set results from the previous workoutEntity OR ones that were deloaded. Deloaded
        // ones are deleted so next progressions are calculated using most recent non-deload results
        setResultsRepository.deleteAllForPreviousWorkout(
            workoutId = workout.id,
            currentMesocycle = programMetadata.currentMesocycle,
            currentMicrocycle = programMetadata.currentMicrocycle,
            currentResultsToDeleteInstead = deloadSetResults,
        )
    }

    fun toggleConfirmCancelWorkoutModal() {
        mutableWorkoutState.update {
            it.copy(
                isConfirmCancelWorkoutDialogShown = !it.isConfirmCancelWorkoutDialogShown
            )
        }
    }

    fun cancelWorkout() {
        if (mutableWorkoutState.value.isConfirmCancelWorkoutDialogShown)
            toggleConfirmCancelWorkoutModal()

        executeInTransactionScope {
            // Remove the workoutEntity from in progress
            workoutInProgressRepositoryImpl.delete()

            // Delete all set results from the workoutEntity
            val programMetadata = mutableWorkoutState.value.programMetadata!!
            setResultsRepository.deleteAllForWorkout(
                workoutId = mutableWorkoutState.value.workout!!.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            mutableWorkoutState.update {
                it.copy(
                    workoutLogVisible = false,
                    inProgressWorkout = null,
                    workout = it.workout?.copy(
                        lifts = it.workout.lifts.fastMap { workoutLift ->
                            workoutLift.copy(
                                sets = workoutLift.sets
                                    .filter { set ->
                                        (set as? LoggingMyoRepSet)?.myoRepSetPosition == null
                                    }.fastMap { set ->
                                        when (set) {
                                            is LoggingStandardSet -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                                complete = false,
                                            )

                                            is LoggingDropSet -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                                complete = false,
                                            )

                                            is LoggingMyoRepSet -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                                complete = false,
                                            )

                                            else -> throw Exception("${set::class.simpleName} is not defined.")
                                        }
                                    }
                            )
                        }
                    )
                )
            }
        }
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) {
        executeInTransactionScope {
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) {
                                val workoutLiftCopy = lift.copy(restTime = newRestTime, restTimerEnabled = enabled)
                                liftsRepository.updateRestTime(
                                    id = lift.liftId,
                                    enabled = enabled,
                                    newRestTime = newRestTime
                                )
                                workoutLiftCopy
                            } else lift
                        }
                    )
                )
            }
        }
    }

    fun updateNote(workoutLiftId: Long, note: String) {
        executeInTransactionScope {
            liftsRepository.updateNote(workoutLiftId, note.ifEmpty { null })

            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { workoutLift ->
                            if (workoutLift.id == workoutLiftId) {
                                workoutLift.copy(
                                    note = note.ifEmpty { null },
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    fun saveRestTimerInProgress(restTime: Long) {
        executeInTransactionScope {
            insertRestTimerInProgress(restTime)

            mutableWorkoutState.update {
                it.copy(
                    restTime = restTime,
                    restTimerStartedAt = getCurrentDate(),
                )
            }
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        return setResultsRepository.upsertMany(updatedResults)
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        return setResultsRepository.upsert(updatedResult)
    }

    override suspend fun deleteSetResult(id: Long) {
        setResultsRepository.deleteById(id)
    }

    override suspend fun insertRestTimerInProgress(restTime: Long) {
        restTimerInProgressRepository.insert(restTime)
    }

    override fun stopRestTimer() {
        cancelRestTimer()
    }
}
