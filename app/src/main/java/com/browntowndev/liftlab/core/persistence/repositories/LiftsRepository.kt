package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

class LiftsRepository(
    private val liftsDao: LiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
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

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                entity = toInsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    liftsDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun update(lift: LiftDto) {
        val current = liftsDao.get(lift.id) ?: return
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
            ).applyFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        liftsDao.update(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    liftsDao.update(it.toEntity())
                }
            )
        }
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
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(restTimerEnabled = enabled, restTime = newRestTime).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(incrementOverride = newIncrement).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    suspend fun updateNote(id: Long, note: String?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(note = note).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    private suspend fun updateWithoutRefetch(lift: Lift) {
        liftsDao.update(lift)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                entity = lift.toFirestoreDto(),
                onSynced = {
                    liftsDao.update(it.toEntity())
                }
            )
        }
    }
}