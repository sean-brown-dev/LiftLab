package com.browntowndev.liftlab.core.persistence

import androidx.room.withTransaction
import com.browntowndev.liftlab.core.persistence.room.LiftLabDatabase

class TransactionScope(private val database: LiftLabDatabase) {
    suspend fun execute(action: suspend () -> Unit) {
        database.withTransaction {
            action()
        }
    }
}