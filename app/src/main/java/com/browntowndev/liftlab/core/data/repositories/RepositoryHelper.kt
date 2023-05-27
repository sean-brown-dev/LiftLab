package com.browntowndev.liftlab.core.data.repositories

import android.content.Context
import com.browntowndev.liftlab.core.data.LiftLabDatabase
import org.koin.core.context.GlobalContext

class RepositoryHelper() {
    fun getRepositories(): List<Repository> {
        val context: Context = GlobalContext.get().get()
        val database = LiftLabDatabase.getInstance(context)
        val liftsDao = database.liftsDao()

        return listOf(LiftsRepository(liftsDao))
    }
}