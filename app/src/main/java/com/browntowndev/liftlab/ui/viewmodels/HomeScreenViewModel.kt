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
    private var _state = MutableStateFlow(
        HomeScreenState(dateRange = getWeekRange()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    program = programsRepository.getActive(),
                    workoutLogs = loggingRepository.getWorkoutLogsForDateRange(
                        range = it.dateRange
                    )
                )
            }
        }
    }

    private fun getWeekRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
    }
}