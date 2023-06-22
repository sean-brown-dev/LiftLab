package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.entities.Lift

class LiftsRepository(private val liftsDao: LiftsDao): Repository {
    suspend fun createLift(lift: Lift) {
        liftsDao.insert(lift)
    }

    suspend fun get(id: Long): LiftDto {
        val lift: Lift = liftsDao.get(id)
        return LiftDto(
            id = lift.id,
            name = lift.name,
            movementPattern = lift.movementPattern,
            volumeTypesBitmask = lift.volumeTypesBitmask,
            incrementOverride = lift.incrementOverride,
            isHidden = lift.isHidden,
            isBodyweight = lift.isBodyweight,
        )
    }

    suspend fun getAll(): List<LiftDto> {
        return liftsDao.getAll().map {
            LiftDto(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
            )
        }
    }
}