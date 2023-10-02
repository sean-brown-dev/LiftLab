package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftDetailsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.time.Duration

class LiftDetailsViewModel(
    private val liftId: Long,
    private val liftsRepository: LiftsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus
) : LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(LiftDetailsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    lift = liftsRepository.get(liftId),
                    previousSetResults = previousSetResultsRepository.getForLift(liftId),
                )
            }
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

    fun updateVolumeType(newVolumeType: VolumeType) {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(volumeTypesBitmask = newVolumeType.bitMask)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift
                )
            }
        }
    }

    fun updateSecondaryVolumeType(newSecondaryVolumeType: VolumeType?) {
        executeInTransactionScope {
            val updatedLift = _state.value.lift!!.copy(secondaryVolumeTypesBitmask = newSecondaryVolumeType?.bitMask)
            liftsRepository.update(updatedLift)

            _state.update {
                it.copy(
                    lift = updatedLift
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
}