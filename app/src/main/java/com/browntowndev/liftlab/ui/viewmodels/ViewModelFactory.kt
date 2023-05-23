package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.browntowndev.liftlab.core.data.repositories.LiftsRepository

class ViewModelFactory constructor(private val liftsRepository: LiftsRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LiftLibraryViewModel::class.java) -> {
                LiftLibraryViewModel(liftsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}