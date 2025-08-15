package com.browntowndev.liftlab.core.common

sealed interface Patch<out T> {
    data object Unset : Patch<Nothing>
    data class Set<T>(val value: T) : Patch<T>
}
