package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toSimpleDateString
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry
import com.browntowndev.liftlab.ui.models.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.getPerWorkoutVolumeChartModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftDetailsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.NumberFormat
import kotlin.math.roundToInt
import kotlin.time.Duration

class LiftDetailsViewModel(
    private val onNavigateBack: () -> Unit,
    private val liftId: Long?,
    private val liftsRepository: LiftsRepository,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus
) : LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(LiftDetailsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lift = if (liftId != null) {
                liftsRepository.get(liftId)
            } else {
                LiftDto(
                    id = 0L,
                    name = "",
                    movementPattern = MovementPattern.AB_ISO,
                    volumeTypesBitmask = VolumeType.AB.bitMask,
                    secondaryVolumeTypesBitmask = null,
                    incrementOverride = null,
                    restTime = null,
                    restTimerEnabled = true,
                    isHidden = false,
                    isBodyweight = false,
                )
            }

            val workoutLogs = if (liftId != null) {
                loggingRepository.getWorkoutLogsForLift(liftId)
            } else listOf()

            _state.update {
                it.copy(
                    lift = lift,
                    workoutLogs = workoutLogs,
                    oneRepMax = getOneRepMax(workoutLogs),
                    maxVolume = getMaxVolume(workoutLogs),
                    maxWeight = getMaxWeight(workoutLogs),
                    topTenPerformances = getTopTenPerformances(workoutLogs),
                    totalReps = getTotalReps(workoutLogs),
                    totalVolume = getTotalVolume(workoutLogs),
                    workoutFilterOptions = getWorkoutFilterOptions(workoutLogs),
                    oneRepMaxChartModel = getOneRepMaxChartModel(workoutLogs, setOf()),
                    volumeChartModel = getPerWorkoutVolumeChartModel(workoutLogs, setOf()),
                    intensityChartModel = getIntensityChartModel(workoutLogs, setOf()),
                    volumeTypeDisplayNames = lift.volumeTypesBitmask.getVolumeTypes()
                        .fastMap { volumeType ->
                            volumeType.displayName()
                        }.sorted(),
                    secondaryVolumeTypeDisplayNames = lift.secondaryVolumeTypesBitmask?.getVolumeTypes()
                        ?.fastMap { volumeType ->
                            volumeType.displayName()
                        }?.sorted() ?: listOf(),
                )
            }
        }
    }

    private fun getOneRepMax(workoutLogs: List<WorkoutLogEntryDto>): Pair<String, String>? {
        val oneRepMax = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    }
        }.maxByOrNull { it.second }

        return if (oneRepMax != null) {
            Pair(oneRepMax.first, oneRepMax.second.toString())
        } else null
    }

    private fun getMaxVolume(workoutLogs: List<WorkoutLogEntryDto>): Pair<String, String>? {
        val maxVolume = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        it.reps * it.weight
                    }
        }.maxByOrNull { it.second }

        return if (maxVolume != null) {
            Pair(maxVolume.first, formatFloatString(maxVolume.second))
        } else null
    }


    private fun getMaxWeight(workoutLogs: List<WorkoutLogEntryDto>): Pair<String, String>? {
        val maxWeight = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        it.weight
                    }
        }.maxByOrNull { it.second }

        return if (maxWeight != null) {
            Pair(maxWeight.first, formatFloatString(maxWeight.second))
        } else null
    }

    private fun getTopTenPerformances(workoutLogs: List<WorkoutLogEntryDto>): List<OneRepMaxEntry> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                OneRepMaxEntry(
                    setsAndRepsLabel = "${formatFloatString(setLog.weight)}x${setLog.reps} @${setLog.rpe}",
                    date = workoutLog.date.toSimpleDateString(),
                    oneRepMax = CalculationEngine.getOneRepMax(setLog.weight, setLog.reps, setLog.rpe).toString()
                )
            }
        }.sortedByDescending { it.oneRepMax }.take(10)
    }

    private fun getTotalReps(workoutLogs: List<WorkoutLogEntryDto>): String {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum().toString()
    }

    private fun getTotalVolume(workoutLogs: List<WorkoutLogEntryDto>): String {
        return formatFloatString(workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum())
    }

    private fun getWorkoutFilterOptions(workoutLogs: List<WorkoutLogEntryDto>): Map<Long, String> {
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

    fun updateName(newName: String) {
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

    private fun updateVolumeType(newVolumeTypeBitmask: Int, newDisplayNames: List<String>) {
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

    private fun updateSecondaryVolumeType(newSecondaryVolumeTypeBitmask: Int?, newDisplayNames: List<String>) {
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

    fun updateRestTime(newRestTime: Duration) {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(restTime = newRestTime)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift
                )
            }
        }
    }

    fun updateMovementPattern(newMovementPattern: MovementPattern) {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(movementPattern = newMovementPattern)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift
                )
            }
        }
    }

    private fun createNewLift() {
        viewModelScope.launch {
            val lift = _state.value.lift!!
            val liftToCreate = if (state.value.lift!!.name.isEmpty()) {
                lift.copy(name = "New Lift")
            } else lift

            liftsRepository.createLift(liftToCreate)
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