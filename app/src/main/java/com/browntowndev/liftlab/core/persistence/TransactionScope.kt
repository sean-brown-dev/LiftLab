package com.browntowndev.liftlab.core.persistence

import androidx.room.withTransaction

class TransactionScope(private val database: LiftLabDatabase) {
    suspend fun execute(action: suspend () -> Unit) {
        database.withTransaction {
            action()
        }
    }
}