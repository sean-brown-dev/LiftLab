package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LabViewModel(private val programsRepository: ProgramsRepository): ViewModel() {
    private val _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getPrograms()
        }
    }

    private suspend fun getPrograms() {
        val programs: List<ProgramDto> = programsRepository.getAll()
        _state.update {
            it.copy(programs = programs)
        }
    }
}