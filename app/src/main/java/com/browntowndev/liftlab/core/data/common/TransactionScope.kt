package com.browntowndev.liftlab.core.data.common

import androidx.room.withTransaction
import com.browntowndev.liftlab.core.data.local.LiftLabDatabase

class TransactionScope(private val db: LiftLabDatabase) {
    suspend fun <T> execute(block: suspend () -> T): T =
        db.withTransaction(block)
}