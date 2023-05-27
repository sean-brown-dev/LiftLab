package com.browntowndev.liftlab.ui.viewmodels

sealed class ViewModelResult<out T> {
    data class Success<out T>(val data: T) : ViewModelResult<T>()
    data class Error(val exception: Exception) : ViewModelResult<Nothing>()
}