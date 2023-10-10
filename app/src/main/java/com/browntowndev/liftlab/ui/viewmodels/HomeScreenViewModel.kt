package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toStartOfDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.HomeScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Date

class HomeScreenViewModel(
    private val programsRepository: ProgramsRepository,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            programsRepository.getActive().observeForever { activeProgram ->
                _state.update {
                    it.copy(
                        program = activeProgram
                    )
                }
            }

            loggingRepository.getAll().observeForever { workoutLogs ->
                _state.update {
                    it.copy(
                        workoutLogs = workoutLogs
                    )
                }
            }
        }
    }
}