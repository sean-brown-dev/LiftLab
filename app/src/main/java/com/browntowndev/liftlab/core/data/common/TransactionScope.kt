package com.browntowndev.liftlab.core.data.common

import androidx.room.withTransaction
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase

class TransactionScope(private val database: LiftLabDatabase) {
    suspend fun execute(action: suspend () -> Unit) {
        database.withTransaction {
            action()
        }
    }

    suspend fun<T> executeWithResult(action: suspend () -> T): T {
        return database.withTransaction {
            action()
        }
    }
}