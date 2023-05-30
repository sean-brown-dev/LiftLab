package com.browntowndev.liftlab.core.data.repositories

import com.browntowndev.liftlab.core.data.dao.LiftsDao
import com.browntowndev.liftlab.core.data.dtos.LiftDto
import com.browntowndev.liftlab.core.data.entities.Lift

class LiftsRepository(private val liftsDao: LiftsDao): Repository {
    suspend fun createLift(lift: Lift) {
        liftsDao.insert(lift)
    }

    suspend fun getAll(): List<LiftDto> {
        return liftsDao.getAll()
    }
}