package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.core.data.dtos.LiftDTO
import com.browntowndev.liftlab.core.data.repositories.LiftsRepository

class LiftLibraryViewModel(private val liftsRepository: LiftsRepository): ViewModel() {
    suspend fun getAllLifts(): List<LiftDTO> {
        return liftsRepository.getAllLifts().sortedBy { it.lift.name }
    }
}