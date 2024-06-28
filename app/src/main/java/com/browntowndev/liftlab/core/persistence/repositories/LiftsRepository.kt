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
            restTimerEnabled = lift.restTimerEnabled,
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
                restTimerEnabled = lift.restTimerEnabled,
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
            restTimerEnabled = lift.restTimerEnabled,
            isHidden = lift.isHidden,
            isBodyweight = lift.isBodyweight,
            note = lift.note,
        )
    }

    suspend fun getAll(): List<LiftDto> {
        return liftsDao.getAll().fastMap { lift ->
            LiftDto(
                id = lift.id,
                name = lift.name,
                movementPattern = lift.movementPattern,
                volumeTypesBitmask = lift.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = lift.secondaryVolumeTypesBitmask,
                incrementOverride = lift.incrementOverride,
                restTime = lift.restTime,
                restTimerEnabled = lift.restTimerEnabled,
                isHidden = lift.isHidden,
                isBodyweight = lift.isBodyweight,
                note = lift.note,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllAsLiveData(): LiveData<List<LiftDto>> {
        return liftsDao.getAllAsFlow().flatMapLatest{ lifts ->
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
                        restTimerEnabled = it.restTimerEnabled,
                        isHidden = it.isHidden,
                        isBodyweight = it.isBodyweight,
                        note = it.note,
                    )
                }
            )
        }.asLiveData()
    }

    suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?) {
        liftsDao.updateRestTime(id, enabled, newRestTime)
    }

    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        liftsDao.updateIncrementOverride(id, newIncrement)
    }

    suspend fun updateNote(id: Long, note: String?) {
        liftsDao.updateNote(id, note)
    }
}