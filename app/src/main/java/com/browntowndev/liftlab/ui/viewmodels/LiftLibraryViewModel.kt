package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.core.data.repositories.LiftsRepository

class LiftLibraryViewModel(private val liftsRepository: LiftsRepository): ViewModel() {
    suspend fun getAllLifts(): List<Lift> {
        return liftsRepository.getAllLifts()
    }
}