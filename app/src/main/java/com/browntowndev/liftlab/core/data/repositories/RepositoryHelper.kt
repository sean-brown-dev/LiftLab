package com.browntowndev.liftlab.core.data.repositories

import android.content.Context
import androidx.room.Room
import com.browntowndev.liftlab.core.data.LiftLabDatabase
import com.browntowndev.liftlab.core.utils.DATABASE_NAME
import org.koin.core.context.GlobalContext

class RepositoryHelper() {
    fun getRepositories(): List<Repository> {
        val context: Context = GlobalContext.get().get()
        val database = LiftLabDatabase.getInstance(context)
        val liftsDao = database.liftsDao()

        return listOf(LiftsRepository(liftsDao))
    }
}