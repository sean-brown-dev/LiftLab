package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

class LiftsRepository(
    private val liftsDao: LiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun createLift(lift: LiftDto) {
        val toInsert =
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
                note = lift.note,
            )
        val id = liftsDao.insert(toInsert)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.LIFTS_COLLECTION,
            entity = toInsert.toFirebaseDto().copy(id = id),
            onSynced = {
                liftsDao.update(it.toEntity())
            }
        )
    }

    suspend fun update(lift: LiftDto) {
        val current = liftsDao.get(lift.id)
        val toUpdate =
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
                note = lift.note,
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        liftsDao.update(toUpdate)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.LIFTS_COLLECTION,
            entity = toUpdate.toFirebaseDto(),
            onSynced = {
                liftsDao.update(it.toEntity())
            }
        )
    }

    suspend fun get(id: Long): LiftDto {
        val lift: Lift? = liftsDao.get(id)
        if (lift == null) throw Exception("Lift not found")
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
        liftsDao.get(id)?.let { toUpdate ->
            updateWithoutRefetch(toUpdate.copy(restTimerEnabled = enabled, restTime = newRestTime))
        }
    }

    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        liftsDao.get(id)?.let { toUpdate ->
            updateWithoutRefetch(toUpdate.copy(incrementOverride = newIncrement))
        }
    }

    suspend fun updateNote(id: Long, note: String?) {
        liftsDao.get(id)?.let { toUpdate ->
            updateWithoutRefetch(toUpdate.copy(note = note))
        }
    }

    private suspend fun updateWithoutRefetch(lift: Lift) {
        lift.synced = false
        liftsDao.update(lift)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.LIFTS_COLLECTION,
            entity = lift.toFirebaseDto(),
            onSynced = {
                liftsDao.update(it.toEntity())
            }
        )
    }
}