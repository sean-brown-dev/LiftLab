package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeUtils
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toMediumDateString
import com.browntowndev.liftlab.core.common.toShortTimeString
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetLiftWithHistoryStateFlowUseCase
import com.browntowndev.liftlab.ui.models.workout.OneRepMaxEntry
import com.browntowndev.liftlab.ui.models.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.getPerWorkoutVolumeChartModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftDetailsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.NumberFormat
import kotlin.math.roundToInt

class LiftDetailsViewModel(
    private val onNavigateBack: () -> Unit,
    liftId: Long?,
    private val liftsRepository: LiftsRepository,
    getLiftWithHistoryStateFlowUseCase: GetLiftWithHistoryStateFlowUseCase,
    transactionScope: TransactionScope,
    eventBus: EventBus
) : LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(LiftDetailsState())
    val state = _state.asStateFlow()

    init {
        getLiftWithHistoryStateFlowUseCase(liftId)
            .map { liftWithHistoryState ->
                LiftDetailsState(
                    lift = liftWithHistoryState.lift,
                    workoutLogs = liftWithHistoryState.workoutLogEntries,
                    oneRepMax = liftWithHistoryState.topTenPerformances.firstOrNull()?.let { pr -> pr.first.toMediumDateString() to pr.second.oneRepMax.toString() },
                    maxVolume = liftWithHistoryState.maxVolume?.let { it.first.toMediumDateString() to formatFloatString(it.second) },
                    maxWeight = liftWithHistoryState.maxWeight?.let { it.first.toMediumDateString() to formatFloatString(it.second) },
                    topTenPerformances = liftWithHistoryState.topTenPerformances.map {
                        OneRepMaxEntry(
                            setsAndRepsLabel = "${formatFloatString(it.second.weight)}x${it.second.reps} @${it.second.rpe}",
                            date = it.first.toMediumDateString(),
                            oneRepMax = it.second.oneRepMax.toString()
                        )
                    },
                    totalReps = liftWithHistoryState.totalReps.toString(),
                    totalVolume = formatFloatString(liftWithHistoryState.totalVolume),
                    workoutFilterOptions = getWorkoutFilterOptions(liftWithHistoryState.workoutLogEntries),
                    oneRepMaxChartModel = getOneRepMaxChartModel(liftWithHistoryState.workoutLogEntries, setOf()),
                    volumeChartModel = getPerWorkoutVolumeChartModel(liftWithHistoryState.workoutLogEntries, setOf()),
                    intensityChartModel = getIntensityChartModel(liftWithHistoryState.workoutLogEntries, setOf()),
                    volumeTypeDisplayNames = liftWithHistoryState.lift?.volumeTypesBitmask?.getVolumeTypes()
                        ?.fastMap { volumeType ->
                            volumeType.displayName()
                        }?.sorted() ?: listOf(),
                    secondaryVolumeTypeDisplayNames = liftWithHistoryState.lift?.secondaryVolumeTypesBitmask?.getVolumeTypes()
                        ?.fastMap { volumeType ->
                            volumeType.displayName()
                        }?.sorted() ?: listOf(),
                )
            }.onEach { state ->
                _state.update {
                    state
                }
            }.launchIn(viewModelScope)
    }

    private fun getMaxVolume(workoutLogs: List<WorkoutLogEntry>): Pair<String, String>? {
        val maxVolume = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toMediumDateString() to
                    workoutLog.setResults.maxOf {
                        it.reps * it.weight
                    }
        }.maxByOrNull { it.second }

        return if (maxVolume != null) {
            Pair(maxVolume.first, formatFloatString(maxVolume.second))
        } else null
    }


    private fun getMaxWeight(workoutLogs: List<WorkoutLogEntry>): Pair<String, String>? {
        val maxWeight = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toMediumDateString() to
                    workoutLog.setResults.maxOf {
                        it.weight
                    }
        }.maxByOrNull { it.second }

        return if (maxWeight != null) {
            Pair(maxWeight.first, formatFloatString(maxWeight.second))
        } else null
    }

    private fun getTopTenPerformances(workoutLogs: List<WorkoutLogEntry>): List<OneRepMaxEntry> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                OneRepMaxEntry(
                    setsAndRepsLabel = "${formatFloatString(setLog.weight)}x${setLog.reps} @${setLog.rpe}",
                    date = workoutLog.date.toMediumDateString(),
                    oneRepMax = WeightCalculationUtils.getOneRepMax(setLog.weight, setLog.reps, setLog.rpe).toString()
                )
            }
        }.sortedByDescending { it.oneRepMax }.take(10)
    }

    private fun getTotalReps(workoutLogs: List<WorkoutLogEntry>): String {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum().toString()
    }

    private fun getTotalVolume(workoutLogs: List<WorkoutLogEntry>): String {
        return formatFloatString(workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum())
    }

    private fun getWorkoutFilterOptions(workoutLogs: List<WorkoutLogEntry>): Map<Long, String> {
        return workoutLogs.associate {
            it.historicalWorkoutNameId to it.workoutName
        }
    }

    private fun formatFloatString(float: Float): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 2

        return if (float.isWholeNumber()) numberFormat.format(float.roundToInt())
        else numberFormat.format(float)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.NavigatedBack -> onNavigateBack()
            TopAppBarAction.ConfirmCreateNewLift -> createNewLift()
            else -> {}
        }
    }

    fun updateName(newName: String) = executeWithErrorHandling("Error updating lift name") {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(name = newName)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift
                )
            }
        }
    }

    fun addVolumeType(newVolumeType: VolumeType) {
        val newVolumeTypeBitmask = _state.value.lift!!.volumeTypesBitmask + newVolumeType.bitMask
        val newDisplayNames = _state.value.volumeTypeDisplayNames
            .toMutableList()
            .apply {
                add(newVolumeType.displayName())
            }

        updateVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    fun addSecondaryVolumeType(newVolumeType: VolumeType) {
        val newVolumeTypeBitmask = (_state.value.lift!!.secondaryVolumeTypesBitmask ?: 0) + newVolumeType.bitMask
        val newDisplayNames = _state.value.secondaryVolumeTypeDisplayNames
            .toMutableList()
            .apply {
                add(newVolumeType.displayName())
            }

        updateSecondaryVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    fun removeVolumeType(toRemove: VolumeType) {
        val newVolumeTypeBitmask = _state.value.lift!!.volumeTypesBitmask - toRemove.bitMask
        val newDisplayNames = _state.value.volumeTypeDisplayNames
            .toMutableList()
            .apply {
                remove(toRemove.displayName())
            }

        updateVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    fun removeSecondaryVolumeType(toRemove: VolumeType) {
        val newVolumeTypeBitmask = _state.value.lift!!.secondaryVolumeTypesBitmask!! - toRemove.bitMask
        val newDisplayNames = _state.value.secondaryVolumeTypeDisplayNames
            .toMutableList()
            .apply {
                remove(toRemove.displayName())
            }

        updateSecondaryVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    fun updateVolumeType(index: Int, newVolumeType: VolumeType) {
        val oldVolumeTypeDisplayName: String
        val newDisplayNames = _state.value.volumeTypeDisplayNames
            .toMutableList()
            .apply {
                oldVolumeTypeDisplayName = this[index]
                this[index] = newVolumeType.displayName()
            }

        val newVolumeTypeBitmask = _state.value.lift!!.volumeTypesBitmask
            .getVolumeTypes()
            .toMutableList()
            .apply {
                val volumeTypeIndex = indexOfFirst { v -> v.displayName() == oldVolumeTypeDisplayName }
                this[volumeTypeIndex] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        updateVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    private fun updateVolumeType(newVolumeTypeBitmask: Int, newDisplayNames: List<String>) = executeWithErrorHandling("Error updating lift volume types") {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(volumeTypesBitmask = newVolumeTypeBitmask)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift,
                    volumeTypeDisplayNames = newDisplayNames,
                )
            }
        }
    }

    fun updateSecondaryVolumeType(index: Int, newVolumeType: VolumeType) {
        val oldVolumeTypeDisplayName: String
        val newDisplayNames = _state.value.secondaryVolumeTypeDisplayNames
            .toMutableList()
            .apply {
                oldVolumeTypeDisplayName = this[index]
                this[index] = newVolumeType.displayName()
            }

        val newVolumeTypeBitmask = _state.value.lift!!.secondaryVolumeTypesBitmask!!
            .getVolumeTypes()
            .toMutableList()
            .apply {
                val volumeTypeIndex = indexOfFirst { v -> v.displayName() == oldVolumeTypeDisplayName }
                this[volumeTypeIndex] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        updateSecondaryVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    private fun updateSecondaryVolumeType(newSecondaryVolumeTypeBitmask: Int?, newDisplayNames: List<String>) = executeWithErrorHandling("Error updating lift secondary volume types") {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(secondaryVolumeTypesBitmask = newSecondaryVolumeTypeBitmask)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift,
                    secondaryVolumeTypeDisplayNames = newDisplayNames,
                )
            }
        }
    }

    fun updateMovementPattern(newMovementPattern: MovementPattern) = executeWithErrorHandling("Error updating lift movement pattern") {
        executeInTransactionScope {
            val volumeTypes = VolumeTypeUtils.getDefaultVolumeTypes(newMovementPattern)
            val secondaryVolumeTypes = VolumeTypeUtils.getDefaultSecondaryVolumeTypes(newMovementPattern)
            val updatedLift = _state.value.lift!!.copy(
                movementPattern = newMovementPattern,
                volumeTypesBitmask = volumeTypes.sumOf { it.bitMask },
                secondaryVolumeTypesBitmask = secondaryVolumeTypes?.sumOf { it.bitMask }
            )
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift,
                    volumeTypeDisplayNames = volumeTypes.map { vt -> vt.displayName() },
                    secondaryVolumeTypeDisplayNames = secondaryVolumeTypes?.map { vt -> vt.displayName() } ?: listOf(),
                )
            }
        }
    }

    private fun createNewLift() {
        viewModelScope.launch {
            val lift = _state.value.lift!!
            val liftEntityToCreate = if (state.value.lift!!.name.isEmpty()) {
                lift.copy(name = "New Lift")
            } else lift

            liftsRepository.insert(liftEntityToCreate)
            onNavigateBack()
        }
    }

    fun filterOneRepMaxChart(selectedOneRepMaxWorkoutFilters: Set<Long>) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedOneRepMaxWorkoutFilters = selectedOneRepMaxWorkoutFilters,
                    oneRepMaxChartModel = getOneRepMaxChartModel(it.workoutLogs, selectedOneRepMaxWorkoutFilters)
                )
            }
        }
    }

    fun filterVolumeChart(selectedVolumeChartWorkoutFilters: Set<Long>) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedVolumeWorkoutFilters = selectedVolumeChartWorkoutFilters,
                    volumeChartModel = getPerWorkoutVolumeChartModel(it.workoutLogs, selectedVolumeChartWorkoutFilters)
                )
            }
        }
    }

    fun filterIntensityChart(selectedIntensityChartWorkoutFilters: Set<Long>) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedIntensityWorkoutFilters = selectedIntensityChartWorkoutFilters,
                    intensityChartModel = getIntensityChartModel(it.workoutLogs, selectedIntensityChartWorkoutFilters)
                )
            }
        }
    }
}