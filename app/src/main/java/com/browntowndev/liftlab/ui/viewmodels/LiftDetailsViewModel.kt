package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftDetailsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

class LiftDetailsViewModel(
    private val liftId: Long?,
    private val navHostController: NavHostController,
    private val liftsRepository: LiftsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
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
                    isHidden = false,
                    isBodyweight = false,
                )
            }

            val previousSetResults = if (liftId != null) {
                previousSetResultsRepository.getForLift(liftId)
            } else listOf()

            _state.update {
                it.copy(
                    lift = lift,
                    previousSetResults = previousSetResults,
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

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.NavigatedBack -> navHostController.popBackStack()
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
        val newVolumeTypeBitmask = _state.value.lift!!.volumeTypesBitmask
            .getVolumeTypes()
            .toMutableList()
            .apply {
                this[index] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        val newDisplayNames = _state.value.volumeTypeDisplayNames
            .toMutableList()
            .apply {
                this[index] = newVolumeType.displayName()
            }

        updateVolumeType(newVolumeTypeBitmask, newDisplayNames)
    }

    fun updateSecondaryVolumeType(index: Int, newVolumeType: VolumeType) {
        val newVolumeTypeBitmask = _state.value.lift!!.secondaryVolumeTypesBitmask!!
            .getVolumeTypes()
            .toMutableList()
            .apply {
                this[index] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        val newDisplayNames = _state.value.secondaryVolumeTypeDisplayNames
            .toMutableList()
            .apply {
                this[index] = newVolumeType.displayName()
            }

        updateSecondaryVolumeType(newVolumeTypeBitmask, newDisplayNames)
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
            liftsRepository.createLift(_state.value.lift!!)
            navHostController.popBackStack()
        }
    }
}