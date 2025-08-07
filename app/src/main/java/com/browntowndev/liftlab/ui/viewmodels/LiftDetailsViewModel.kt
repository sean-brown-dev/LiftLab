package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.toMediumDateString
import com.browntowndev.liftlab.core.common.toTwoDecimalString
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.extensions.toFilterOptions
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.AddVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.CreateLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetLiftWithHistoryStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.RemoveVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateLiftNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateMovementPatternUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateVolumeTypeUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutHistoryMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.metrics.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.metrics.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.metrics.getPerWorkoutVolumeChartModel
import com.browntowndev.liftlab.ui.models.workout.OneRepMaxEntry
import com.browntowndev.liftlab.ui.viewmodels.states.LiftDetailsState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiftDetailsViewModel(
    liftId: Long?,
    private val updateLiftNameUseCase: UpdateLiftNameUseCase,
    private val updateMovementPatternUseCase: UpdateMovementPatternUseCase,
    private val updateVolumeTypeUseCase: UpdateVolumeTypeUseCase,
    private val addVolumeTypeUseCase: AddVolumeTypeUseCase,
    private val removeVolumeTypeUseCase: RemoveVolumeTypeUseCase,
    private val createLiftUseCase: CreateLiftUseCase,
    getLiftWithHistoryStateFlowUseCase: GetLiftWithHistoryStateFlowUseCase,
    private val onNavigateBack: () -> Unit,
    eventBus: EventBus
) : BaseViewModel(eventBus) {
    private var _state = MutableStateFlow(LiftDetailsState())
    val state = _state.asStateFlow()

    init {
        getLiftWithHistoryStateFlowUseCase(liftId)
            .distinctUntilChanged()
            .scan(LiftDetailsState()) { currentLiftDetailsState, liftWithHistoryState ->
                // Lift values can change, so always recalculate these on state change
                val newLiftDetailsState = currentLiftDetailsState.copy(
                    lift = liftWithHistoryState.lift.toUiModel(),
                )

                // Only recalculate when workout logs change (will only happen once, they can't change within this viewmodel)
                if (currentLiftDetailsState.workoutLogs != liftWithHistoryState.workoutLogEntries) {
                    val workoutLogUiModels = liftWithHistoryState.workoutLogEntries.fastMap { it.toUiModel() }
                    newLiftDetailsState.copy(
                        workoutLogs = workoutLogUiModels,
                        oneRepMax = liftWithHistoryState.topTenPerformances.firstOrNull()?.let { pr -> pr.first.toMediumDateString() to pr.second.oneRepMax.toString() },
                        maxVolume = liftWithHistoryState.maxVolume?.let { it.first.toMediumDateString() to it.second.toTwoDecimalString() },
                        maxWeight = liftWithHistoryState.maxWeight?.let { it.first.toMediumDateString() to it.second.toTwoDecimalString() },
                        topTenPerformances = liftWithHistoryState.topTenPerformances.map {
                            OneRepMaxEntry(
                                setsAndRepsLabel = "${it.second.weight.toTwoDecimalString()}x${it.second.reps} @${it.second.rpe}",
                                date = it.first.toMediumDateString(),
                                oneRepMax = it.second.oneRepMax.toString()
                            )
                        },
                        totalReps = liftWithHistoryState.totalReps.toString(),
                        totalVolume = liftWithHistoryState.totalVolume.toTwoDecimalString(),
                        workoutFilterOptions = liftWithHistoryState.workoutLogEntries.toFilterOptions(),
                        oneRepMaxChartModel = getOneRepMaxChartModel(workoutLogUiModels, setOf()),
                        volumeChartModel = getPerWorkoutVolumeChartModel(workoutLogUiModels, setOf()),
                        intensityChartModel = getIntensityChartModel(workoutLogUiModels, setOf()),
                    )
                } else newLiftDetailsState
            }.onEach { state ->
                _state.update {
                    state
                }
            }.catch {
                Log.e("LiftDetailsViewModel", "Error while loading lift details", it)
                FirebaseCrashlytics.getInstance().recordException(it)
                emitUserMessage("Failed to load lift details")
            }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.NavigatedBack -> onNavigateBack()
            TopAppBarAction.ConfirmCreateNewLift -> createNewLift()
            else -> {}
        }
    }

    fun updateName(newName: String) = executeWithErrorHandling("Failed to update lift name") {
        updateLiftNameUseCase(_state.value.lift!!.toDomainModel(), newName)
    }

    fun addVolumeType(newVolumeType: String) = executeWithErrorHandling("Failed to add lift volume type") {
        addVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            newVolumeType = VolumeType.fromDisplayName(newVolumeType),
            volumeTypeCategory = VolumeTypeCategory.PRIMARY
        )
    }

    fun addSecondaryVolumeType(newVolumeType: String) = executeWithErrorHandling("Failed to add lift secondary volume type") {
        addVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            newVolumeType = VolumeType.fromDisplayName(newVolumeType),
            volumeTypeCategory = VolumeTypeCategory.SECONDARY
        )
    }

    fun removeVolumeType(toRemove: String) = executeWithErrorHandling("Failed to remove lift volume type") {
        removeVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            volumeTypeToRemove = VolumeType.fromDisplayName(toRemove),
            volumeTypeCategory = VolumeTypeCategory.PRIMARY
        )
    }

    fun removeSecondaryVolumeType(toRemove: String) = executeWithErrorHandling("Failed to remove lift secondary volume type") {
        removeVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            volumeTypeToRemove = VolumeType.fromDisplayName(toRemove),
            volumeTypeCategory = VolumeTypeCategory.SECONDARY
        )
    }

    fun updateVolumeType(index: Int, newVolumeType: String) = executeWithErrorHandling("Failed to update lift volume types") {
        updateVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            index = index,
            newVolumeType = VolumeType.fromDisplayName(newVolumeType),
            volumeTypeCategory = VolumeTypeCategory.PRIMARY
        )
    }

    fun updateSecondaryVolumeType(index: Int, newVolumeType: String) = executeWithErrorHandling("Failed to update lift secondary volume types") {
        updateVolumeTypeUseCase(
            lift = _state.value.lift!!.toDomainModel(),
            index = index,
            newVolumeType = VolumeType.fromDisplayName(newVolumeType),
            volumeTypeCategory = VolumeTypeCategory.SECONDARY
        )
    }

    fun updateMovementPattern(newMovementPattern: MovementPattern) = executeWithErrorHandling("Failed to update lift movement pattern") {
        updateMovementPatternUseCase(_state.value.lift!!.toDomainModel(), newMovementPattern)
    }

    private fun createNewLift() = executeWithErrorHandling("Failed to create new lift") {
        val lift = _state.value.lift!!
        val liftEntityToCreate = if (state.value.lift!!.name.isEmpty()) {
            lift.copy(name = "New Lift")
        } else lift

        createLiftUseCase(liftEntityToCreate.toDomainModel())
        onNavigateBack()
    }

    fun filterOneRepMaxChart(selectedOneRepMaxWorkoutFilters: Set<Long>) = executeWithErrorHandling("Failed to filter one rep max chart") {
        Log.d("LiftDetailsViewModel", "filterOneRepMaxChart: $selectedOneRepMaxWorkoutFilters")
        Log.d("LiftDetailsViewModel", "filterOneRepMaxChart: ${_state.value.workoutLogs.fastMap { it.historicalWorkoutNameId }.distinct()}")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedOneRepMaxWorkoutFilters = selectedOneRepMaxWorkoutFilters,
                    oneRepMaxChartModel = getOneRepMaxChartModel(it.workoutLogs, selectedOneRepMaxWorkoutFilters)
                )
            }
        }
    }

    fun filterVolumeChart(selectedVolumeChartWorkoutFilters: Set<Long>) = executeWithErrorHandling("Failed to filter volume chart") {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedVolumeWorkoutFilters = selectedVolumeChartWorkoutFilters,
                    volumeChartModel = getPerWorkoutVolumeChartModel(it.workoutLogs, selectedVolumeChartWorkoutFilters)
                )
            }
        }
    }

    fun filterIntensityChart(selectedIntensityChartWorkoutFilters: Set<Long>) = executeWithErrorHandling("Failed to filter intensity chart") {
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