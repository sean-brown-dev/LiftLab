package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

class LiftsRepository(private val liftsDao: LiftsDao): Repository {
    suspend fun createLift(lift: LiftDto) {
        liftsDao.insert(
            Lift(
            id = lift.id,
            name = lift.name,
            movementPattern = lift.movementPattern,
            volumeTypesBitmask = lift.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = lift.secondaryVolumeTypesBitmask,
            incrementOverride = lift.incrementOverride,
            restTime = lift.restTime,
            isHidden = lift.isHidden,
            isBodyweight = lift.isBodyweight,
        ))
    }

    suspend fun update(lift: LiftDto) {
        liftsDao.update(
            Lift(
                id = lift.id,
                name = lift.name,
                movementPattern = lift.movementPattern,
                volumeTypesBitmask = lift.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = lift.secondaryVolumeTypesBitmask,
                incrementOverride = lift.incrementOverride,
                restTime = lift.restTime,
                isHidden = lift.isHidden,
                isBodyweight = lift.isBodyweight,
            ))
    }

    suspend fun get(id: Long): LiftDto {
        val lift: Lift = liftsDao.get(id)
        return LiftDto(
            id = lift.id,
            name = lift.name,
            movementPattern = lift.movementPattern,
            volumeTypesBitmask = lift.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = lift.secondaryVolumeTypesBitmask,
            incrementOverride = lift.incrementOverride,
            restTime = lift.restTime,
            isHidden = lift.isHidden,
            isBodyweight = lift.isBodyweight,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getAll(): LiveData<List<LiftDto>> {
        return liftsDao.getAll().flatMapLatest{ lifts ->
            flowOf(
                lifts.fastMap {
                    LiftDto(
                        id = it.id,
                        name = it.name,
                        movementPattern = it.movementPattern,
                        volumeTypesBitmask = it.volumeTypesBitmask,
                        secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                        incrementOverride = it.incrementOverride,
                        restTime = it.restTime,
                        isHidden = it.isHidden,
                        isBodyweight = it.isBodyweight,
                    )
                }
            )
        }.asLiveData()
    }

    suspend fun updateRestTime(id: Long, newRestTime: Duration?) {
        liftsDao.updateRestTime(id, newRestTime)
    }

    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        liftsDao.updateIncrementOverride(id, newIncrement)
    }
}