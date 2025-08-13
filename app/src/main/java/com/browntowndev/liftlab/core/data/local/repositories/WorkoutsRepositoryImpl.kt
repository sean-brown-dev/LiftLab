package com.browntowndev.liftlab.core.data.local.repositories

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.common.CommandType
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.extensions.copyId
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutsRepositoryImpl(
    private val workoutsDao: WorkoutsDao,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val customSetsDao: CustomSetsDao,
    private val liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao,
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val syncScheduler: SyncScheduler,
): WorkoutsRepository {
    override fun getMetadataFlow(id: Long): Flow<WorkoutMetadata> =
        workoutsDao.getMetadataFlow(id).map { it.toDomainModel() }

    override suspend fun getAll(): List<Workout> {
        return workoutsDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<Workout>> {
        return workoutsDao.getAllFlow().map { workouts ->
            workouts.map { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: Long): Workout? {
        return workoutsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Workout> {
        return workoutsDao.getMany(ids).map { it.toDomainModel() }
    }

    override fun getFlow(workoutId: Long): Flow<Workout?> {
        return workoutsDao.getByIdFlow(workoutId).map { workout ->
            workout?.toDomainModel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getByMicrocyclePositionForCalculation(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<CalculationWorkout?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).map { workoutEntity ->
            workoutEntity?.toCalculationDomainModel()
        }
    }

    override suspend fun getAllForProgramWithoutLiftsPopulated(programId: Long): List<Workout> {
        return workoutsDao.getAllForProgramWithoutRelationships(programId).map { it.toDomainModel() }
    }

    override suspend fun updateName(id: Long, newName: String) {
        val current = workoutsDao.getWithoutRelationships(id) ?: return
        val toUpdate = current.copy(name = newName).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun update(model: Workout) {
        val current = workoutsDao.getWithoutRelationships(model.id) ?: return
        val updWorkout = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )

        workoutsDao.update(updWorkout)
        performCommandForChildren(model, CommandType.UPDATE)

        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<Workout>) {
        val currentEntities = workoutsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = models.fastMapNotNull { workout ->
            val current = currentEntities[workout.id] ?: return@fastMapNotNull null
            workout.toEntity().applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.remoteLastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutsDao.updateMany(toUpdate)
        models.fastForEach { workout ->
            performCommandForChildren(workout, CommandType.UPDATE)
        }

        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: Workout): Long {
        val current = workoutsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false,
        )
        val id = workoutsDao.upsert(toUpsert).let { upsertId ->
            if (upsertId == -1L) toUpsert.id else upsertId
        }
        performCommandForChildren(model.copy(id = id), CommandType.UPSERT)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun upsertMany(models: List<Workout>): List<Long> {
        val currentEntities = workoutsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        val toUpsert = models.map { workout ->
            val current = currentEntities[workout.id]
            workout.toEntity().applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false,
            )
        }
        val ids = workoutsDao.upsertMany(toUpsert)
        val upsertedModels = models.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }
        upsertedModels.fastForEach { workout ->
            performCommandForChildren(workout, CommandType.UPSERT)
        }

        syncScheduler.scheduleSync()

        return upsertedModels.fastMap { it.id }
    }

    override suspend fun insert(model: Workout): Long {
        val toInsert = model.toEntity()
        val id = workoutsDao.insert(toInsert)
        performCommandForChildren(model.copy(id = id), CommandType.INSERT)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<Workout>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = workoutsDao.insertMany(toInsert)
        models.zip(ids).fastForEach { (model, id) ->
            performCommandForChildren(model.copy(id = id), CommandType.INSERT)
        }
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: Workout): Int {
        val deleteCount = workoutsDao.softDelete(model.id)
        if (deleteCount > 0) {
            workoutLiftsDao.softDeleteByWorkoutId(model.id)
            customSetsDao.softDeleteByWorkoutId(model.id)
            workoutInProgressDao.softDeleteByWorkoutId(model.id)
            liveWorkoutCompletedSetsDao.softDeleteAllByWorkoutId(model.id)
            syncScheduler.scheduleSync()
        }

        return deleteCount
    }

    override suspend fun deleteMany(models: List<Workout>): Int {
        val toDeleteIds = models.map { it.id }
        if (toDeleteIds.isEmpty()) return 0

        val deletedCount = workoutsDao.softDeleteMany(toDeleteIds)
        if (deletedCount > 0) {
            workoutLiftsDao.softDeleteByWorkoutIds(toDeleteIds)
            customSetsDao.softDeleteByWorkoutIds(toDeleteIds)
            workoutInProgressDao.softDeleteByWorkoutIds(toDeleteIds)
            liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(toDeleteIds)

            syncScheduler.scheduleSync()
        }

        return deletedCount
    }

    override suspend fun deleteById(id: Long): Int {
        val model = getById(id) ?: return 0
        return delete(model)
    }

    private suspend fun performCommandForChildren(workout: Workout, commandType: CommandType) {
        // ---- Lifts: persist ----
        val existingLiftById = loadExistingLiftsById(workout)
        val updatedLifts = persistLifts(workout, commandType, existingLiftById)

        // ---- Lifts: soft-delete removed (and cascade sets) ----
        softDeleteRemovedLifts(workout.id, updatedLifts)

        // ---- Sets: persist + diff-delete ----
        val customLifts = updatedLifts.filterIsInstance<CustomWorkoutLift>()
        val standardLifts = updatedLifts.filterIsInstance<StandardWorkoutLift>()

        persistCustomSets(customLifts, commandType, existingLiftById)
        diffDeleteRemovedCustomSets(customLifts)
        purgeStandardLiftSets(standardLifts)
    }

    /* ---------------------------------------------------------
     *                      HELPERS
     * --------------------------------------------------------- */

    private suspend fun loadExistingLiftsById(
        workout: Workout
    ): Map<Long, WorkoutLiftWithRelationships> {
        val ids = workout.lifts.mapNotNull { it.id.takeIf { id -> id != 0L } }
        return workoutLiftsDao
            .getMany(ids)
            .associateBy { it.workoutLiftEntity.id }
    }

    private suspend fun persistLifts(
        workout: Workout,
        commandType: CommandType,
        existingLiftById: Map<Long, WorkoutLiftWithRelationships>
    ): List<GenericWorkoutLift> {
        val liftEntities = workout.lifts.map { l ->
            l.toEntity()
                .copy(workoutId = workout.id)
                .applyRemoteStorageMetadata(
                    remoteId = existingLiftById[l.id]?.workoutLiftEntity?.remoteId,
                    remoteLastUpdated = existingLiftById[l.id]?.workoutLiftEntity?.remoteLastUpdated,
                    synced = false
                )
        }

        val workoutLiftIds = when (commandType) {
            CommandType.UPSERT -> workoutLiftsDao.upsertMany(liftEntities)
            CommandType.UPDATE -> {
                workoutLiftsDao.updateMany(liftEntities.filter { it.id != 0L })
                liftEntities.map { it.id }
            }
            CommandType.INSERT -> workoutLiftsDao.insertMany(liftEntities)
        }

        return workout.lifts
            .zip(workoutLiftIds)
            .map { (l, maybeId) -> l.copyId(if (maybeId == -1L) l.id else maybeId) }
    }

    private suspend fun softDeleteRemovedLifts(
        workoutId: Long,
        updatedLifts: List<GenericWorkoutLift>
    ) {
        val existing = workoutLiftsDao.getForWorkout(workoutId).fastMap { it.workoutLiftEntity.id }.toSet()
        val keep = updatedLifts.map { it.id }.toSet()
        val removedLiftIds = (existing - keep).toList()
        if (removedLiftIds.isNotEmpty()) {
            workoutLiftsDao.softDeleteMany(removedLiftIds)
            customSetsDao.softDeleteByWorkoutLiftIds(removedLiftIds)
        }
    }

    private suspend fun persistCustomSets(
        customLifts: List<CustomWorkoutLift>,
        commandType: CommandType,
        existingLiftById: Map<Long, WorkoutLiftWithRelationships>
    ) {
        if (customLifts.none { it.customLiftSets.isNotEmpty() }) return

        val existingSetById = existingLiftById.values
            .flatMap { it.customLiftSetEntities }
            .associateBy { it.id }

        val setEntities = customLifts.flatMap { l ->
            l.customLiftSets.map { s ->
                s.toEntity()
                    .copy(workoutLiftId = l.id)
                    .applyRemoteStorageMetadata(
                        remoteId = existingSetById[s.id]?.remoteId,
                        remoteLastUpdated = existingSetById[s.id]?.remoteLastUpdated,
                        synced = false
                    )
            }
        }

        when (commandType) {
            CommandType.UPSERT -> customSetsDao.upsertMany(setEntities)
            CommandType.UPDATE -> customSetsDao.updateMany(setEntities.filter { it.id != 0L })
            CommandType.INSERT -> customSetsDao.insertMany(setEntities)
        }
    }

    private suspend fun diffDeleteRemovedCustomSets(
        customLifts: List<CustomWorkoutLift>
    ) {
        val removedSetIds = buildList {
            customLifts.forEach { l ->
                val existing = customSetsDao.getByWorkoutLiftId(l.id).map { it.id }.toSet()
                val keep = l.customLiftSets.map { it.id }.filter { it != 0L }.toSet()
                addAll(existing - keep)
            }
        }
        if (removedSetIds.isNotEmpty()) {
            customSetsDao.softDeleteMany(removedSetIds)
        }
    }

    private suspend fun purgeStandardLiftSets(
        standardLifts: List<StandardWorkoutLift>
    ) {
        if (standardLifts.isEmpty()) return
        val stdSetIds = standardLifts
            .flatMap { l -> customSetsDao.getByWorkoutLiftId(l.id) }
            .map { it.id }
        if (stdSetIds.isNotEmpty()) {
            customSetsDao.softDeleteMany(stdSetIds)
        }
    }
}
