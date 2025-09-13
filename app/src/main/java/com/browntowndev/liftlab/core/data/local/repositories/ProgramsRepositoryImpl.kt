package com.browntowndev.liftlab.core.data.local.repositories

import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.valueOrDefault
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.domain.delta.LiftChange
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.delta.WorkoutChange
import com.browntowndev.liftlab.core.domain.delta.validate
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgramsRepositoryImpl(
    private val programsDao: ProgramsDao,
    private val workoutsDao: WorkoutsDao,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val customSetsDao: CustomSetsDao,
    private val liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao,
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val syncScheduler: SyncScheduler,
    private val transactionScope: TransactionScope,
) : ProgramsRepository {
    override suspend fun insert(program: Program): Long {
        return programsDao.insert(program.toEntity())
    }

    override fun getAllFlow(): Flow<List<Program>> {
        return programsDao.getAllFlow().map { entities ->
            entities.map { entity -> entity.toDomainModel() }
        }
    }

    override suspend fun getNewest(): Program? {
        return programsDao.getNewest()?.toDomainModel()
    }

    override suspend fun getForWorkout(workoutId: Long): Program? {
        return programsDao.getForWorkout(workoutId)?.toDomainModel()
    }

    override suspend fun getActive(): Program? {
        return programsDao.getActive()?.let { programEntity ->
            val program = programEntity.toDomainModel()
            getSortedCopy(program)
        }
    }

    override suspend fun getAllActive(): List<Program> {
        return programsDao.getAllActive().fastMap { entity ->
            val program = entity.toDomainModel()
            getSortedCopy(program)
        }
    }

    override fun getActiveProgramFlow(): Flow<Program?> {
        val programMeta = programsDao.getActiveFlow().map { programEntity ->
            if (programEntity != null) {
                val program = programEntity.toDomainModel()
                getSortedCopy(program)
            } else null
        }

        return programMeta
    }

    override fun getActiveProgramWorkoutCountFlow(): Flow<Int> {
        return programsDao.getActiveProgramWorkoutCountFlow()
    }

    private fun getSortedCopy(program: Program): Program {
        return program.copy(workouts = program.workouts
            .sortedBy { workout -> workout.position }
            .map { workout ->
                workout.copy(lifts = workout.lifts
                    .sortedBy { lift -> lift.position }
                    .map { lift ->
                        when (lift) {
                            is CustomWorkoutLift -> lift.copy(
                                customLiftSets = lift.customLiftSets.sortedBy { it.position }
                            )
                            else -> lift
                        }
                    }
                )
            }
        )
    }

    override suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    override fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadata?> {
        return programsDao.getActiveProgramMetadata().map { metadata ->
            if (metadata == null) return@map null
            ActiveProgramMetadata(
                programId = metadata.programId,
                name = metadata.name,
                deloadWeek = metadata.deloadWeek,
                currentMesocycle = metadata.currentMesocycle,
                currentMicrocycle = metadata.currentMicrocycle,
                currentMicrocyclePosition = metadata.currentMicrocyclePosition,
                workoutCount = metadata.workoutCount,
            )
        }
    }

    /**
     * Apply the delta to the program.
     */
    override suspend fun applyDelta(programId: Long, delta: ProgramDelta) = transactionScope.execute {
        // 0) Validate intent
        delta.validate(programId)

        var didMutate = false

        // 1) Delete whole aggregate (exclusive)
        if (delta.deleteProgram) {
            didMutate = softDeleteProgramAndCascade(programId)
            if (didMutate) syncScheduler.scheduleSync()
            return@execute
        }

        // 2) Patch program (preserve unspecified fields)
        delta.programUpdate?.let { programUpdate ->
            patchProgram(programId, programUpdate)
            didMutate = true
        }

        // 3) Remove workouts (and cascade)
        if (delta.removedWorkoutIds.isNotEmpty()) {
            didMutate = removeWorkoutsAndCascade(delta.removedWorkoutIds) || didMutate
        }

        // 4) Upserts under the program
        delta.workouts.fastForEach { workoutChange ->

            // 4a) Upsert workout, deriving canonical workoutId
            val upsertResult = maybeUpsertWorkout(programId, workoutChange)
            val workoutId = upsertResult.workoutId
            didMutate = didMutate || upsertResult.upserted

            // 4b) Upsert lifts under this workout and resolve workoutLiftIds
            val upsertedWorkoutLiftIds = maybeUpsertWorkoutLifts(workoutId, workoutChange)
            didMutate = upsertedWorkoutLiftIds.isNotEmpty() || didMutate

            // Combine upserted workout lift IDs with workout lift IDs of lifts that were not upserted (lifts with set changes only)
            val canonicalWorkoutLiftIds = workoutChange.lifts
                .fastMapNotNull {
                    if (it.workoutLiftId != 0L) it.workoutLiftId
                    else null
                }.toMutableList().apply {
                    addAll(upsertedWorkoutLiftIds)
                }.distinct()

            // Map each LiftChange → canonical workoutLiftId
            val workoutLiftIdByLiftChange = mutableMapOf<LiftChange, Long>()
            workoutChange.lifts.zip(canonicalWorkoutLiftIds).fastForEach { (liftChange, id) ->
                workoutLiftIdByLiftChange[liftChange] = id
            }

            // 4c) Remove lifts (and cascade sets)
            if (workoutChange.removedWorkoutLiftIds.isNotEmpty()) {
                didMutate = softDeleteWorkoutLiftsAndCascade(workoutChange) || didMutate
            }

            // 4d) Sets for each lift (insert/update/delete + purge)
            // Upsert sets
            didMutate = upsertSetEntities(workoutChange, workoutLiftIdByLiftChange) || didMutate

            val setDeletionIds = getSetDeletions(workoutChange, workoutLiftIdByLiftChange)
            setDeletionIds.setDeletions.fastForEach { setDeletion ->
                customSetsDao.softDeleteMany(setDeletion.setIds)
                val remainingSets = customSetsDao.getByWorkoutLiftId(setDeletion.workoutLiftId)
                if (remainingSets.isNotEmpty()) {
                    val setsWithNewPositions = remainingSets.mapIndexed { index, entity ->
                        entity.copy(position = index)
                    }
                    customSetsDao.updateMany(setsWithNewPositions)
                    Log.d("ProgramsRepositoryImpl", "Updated set positions for lift: ${setDeletion.workoutLiftId}")
                }
                didMutate = true
            }

            if (setDeletionIds.workoutLiftIds.isNotEmpty()) {
                customSetsDao.softDeleteByWorkoutLiftIds(setDeletionIds.workoutLiftIds)
                didMutate = true
                Log.d("ProgramsRepositoryImpl", "Deleted custom lift sets: ${setDeletionIds.workoutLiftIds}")
            }
        }

        if (didMutate) {
            syncScheduler.scheduleSync()
        }
    }

    /**
     * Soft-delete the program and cascade to children as well as tangential tables.
     */
    private suspend fun softDeleteProgramAndCascade(programId: Long): Boolean {
        val deleted = programsDao.softDelete(programId)
        if (deleted > 0) {
            // Cascade to children + tangential tables
            workoutsDao.softDeleteByProgramId(programId)
            workoutLiftsDao.softDeleteByProgramId(programId)
            customSetsDao.softDeleteByProgramId(programId)
            workoutInProgressDao.softDeleteByProgramId(programId)
            liveWorkoutCompletedSetsDao.softDeleteByProgramId(programId)
            Log.d("ProgramsRepositoryImpl", "Deleted program: $programId")
        }

        return deleted > 0
    }

    /**
     * Patch program fields (preserve unspecified fields).
     */
    private suspend fun patchProgram(programId: Long, programUpdate: ProgramUpdate) {
        val existingProgram = programsDao.get(programId)?.programEntity
            ?: error("Program $programId not found")

        val patchedProgram = existingProgram.copy(
            name = programUpdate.name.valueOrDefault(existingProgram.name),
            isActive = programUpdate.isActive.valueOrDefault(existingProgram.isActive),
            deloadWeek = programUpdate.deloadWeek.valueOrDefault(existingProgram.deloadWeek),
            currentMesocycle = programUpdate.currentMesocycle.valueOrDefault(existingProgram.currentMesocycle),
            currentMicrocycle = programUpdate.currentMicrocycle.valueOrDefault(existingProgram.currentMicrocycle),
            currentMicrocyclePosition = programUpdate.currentMicrocyclePosition.valueOrDefault(existingProgram.currentMicrocyclePosition)
        ).applyRemoteStorageMetadata(
            remoteId = existingProgram.remoteId,
            remoteLastUpdated = existingProgram.remoteLastUpdated,
            synced = false
        )

        programsDao.update(patchedProgram)
        Log.d("ProgramsRepositoryImpl", "Patched program: $programId")
    }

    /**
     * Soft-delete the workouts and cascade to children as well as tangential tables.
     */
    private suspend fun removeWorkoutsAndCascade(workoutIds: List<Long>): Boolean {
        val rows = workoutsDao.softDeleteMany(workoutIds)
        if (rows > 0) {
            workoutLiftsDao.softDeleteByWorkoutIds(workoutIds)
            customSetsDao.softDeleteByWorkoutIds(workoutIds)
            workoutInProgressDao.softDeleteByWorkoutIds(workoutIds)
            liveWorkoutCompletedSetsDao.softDeleteByWorkoutIds(workoutIds)

            Log.d("ProgramsRepositoryImpl", "Deleted workouts: $workoutIds")
        }

        return rows > 0
    }

    /**
     * Insert the workout and its children.
     *
     * @param programId Program ID for the workout.
     * @param workout Workout to insert.
     * @return Canonical workout ID.
     */
    private suspend fun insertWorkoutAndChildren(programId: Long, workout: Workout): Long {
        // program ID should already match, but just in case
        val workoutEntity = workout.toEntity().copy(programId = programId)
        val workoutId = workoutsDao.insert(workoutEntity)
        Log.d("ProgramsRepositoryImpl", "Inserted workout: $workoutId")
        if (workout.lifts.isEmpty()) return workoutId

        val workoutLifts = workout.lifts.fastMap { it.toEntity().copy(workoutId = workoutId) }
        val workoutLiftIds = workoutLiftsDao.insertMany(workoutLifts)
        Log.d("ProgramsRepositoryImpl", "Inserted workout lifts for workout: $workoutId")

        val customLiftSets = workout.lifts.zip(workoutLiftIds).fastFlatMap { (lift, workoutLiftId) ->
            if (lift is CustomWorkoutLift) {
                lift.customLiftSets.fastMap {
                    it.toEntity().copy(workoutLiftId = workoutLiftId)
                }
            } else emptyList()
        }

        if (customLiftSets.isNotEmpty()) {
            customSetsDao.insertMany(customLiftSets)
            Log.d("ProgramsRepositoryImpl", "Inserted custom lift sets for workout: $workoutId")
        }

        return workoutId
    }

    /**
     * Result of attempting to upsert a workout, deriving canonical workoutId.
     */
    private data class MaybeUpsertWorkoutResult(
        val workoutId: Long,
        val upserted: Boolean,
    )

    /**
     * Upsert the workout if the [workoutChange] has insert or update data.
     *
     * @param programId Program ID for the workout.
     * @param workoutChange Workout change details.
     * @return Result of attempting to upsert a workout, deriving canonical workoutId.
     */
    private suspend fun maybeUpsertWorkout(programId: Long, workoutChange: WorkoutChange): MaybeUpsertWorkoutResult {
        var upserted = false
        val workoutId = if (workoutChange.workoutInsert != null) {
            upserted = true
            insertWorkoutAndChildren(programId, workoutChange.workoutInsert)
        }
        else if (workoutChange.workoutUpdate != null) {
            val existingWorkout = workoutsDao.getWithoutRelationshipsWithProgramValidation(workoutChange.workoutId, programId)
                ?: error("Workout ${workoutChange.workoutId} not found for program: $programId")

            val updatedWorkout = existingWorkout.copy(
                name = workoutChange.workoutUpdate.name.valueOrDefault(existingWorkout.name),
                position = workoutChange.workoutUpdate.position.valueOrDefault(existingWorkout.position),
            ).applyRemoteStorageMetadata(
                remoteId = existingWorkout.remoteId,
                remoteLastUpdated = existingWorkout.remoteLastUpdated,
                synced = false
            )
            workoutsDao.update(updatedWorkout)
            upserted = true
            Log.d("ProgramsRepositoryImpl", "Patched workout: ${workoutChange.workoutId}")
            existingWorkout.id
        } else workoutChange.workoutId

        return MaybeUpsertWorkoutResult(workoutId, upserted)
    }

    /**
     * Upsert the lift entities under the workout.
     *
     * @param workoutId Workout ID for the lift entities.
     * @param workoutChange Workout change details.
     * @return List of upserted lift IDs.
     */
    private suspend fun maybeUpsertWorkoutLifts(workoutId: Long, workoutChange: WorkoutChange): List<Long> {
        val workoutLiftsToInsert = workoutChange.lifts.mapNotNull { it.insertLift }
        val updateWorkoutLiftChanges = workoutChange.lifts.fastFilter { it.liftUpdate != null }

        val upsertedWorkoutLiftIds = if (workoutLiftsToInsert.isNotEmpty()) {
            val upsertLiftEntities = workoutLiftsToInsert.fastMapNotNull { insertLift ->
                insertLift.toEntity().copy(workoutId = workoutId)
            }

            val existingWorkoutLiftsById = workoutLiftsDao.getForWorkout(workoutId).associateBy { it.workoutLiftEntity.id }
            val upsertLiftEntitiesWithRemoteMetadata = upsertLiftEntities.fastMap { liftEntity ->
                val existing = existingWorkoutLiftsById[liftEntity.id]
                liftEntity.applyRemoteStorageMetadata(
                    remoteId = existing?.workoutLiftEntity?.remoteId,
                    remoteLastUpdated = existing?.workoutLiftEntity?.remoteLastUpdated,
                    synced = false
                )
            }
            Log.d("ProgramsRepositoryImpl", "Inserted workout lifts for workout: $workoutId")
            workoutLiftsDao.insertMany(upsertLiftEntitiesWithRemoteMetadata).toMutableList()
        } else mutableListOf()

        upsertedWorkoutLiftIds += if (updateWorkoutLiftChanges.isNotEmpty()) {
            val existingWorkoutLiftsById = workoutLiftsDao.getForWorkout(workoutId).associateBy { it.workoutLiftEntity.id }
            val liftsToUpdate = updateWorkoutLiftChanges.fastMap { updateChange ->
                val existing = existingWorkoutLiftsById[updateChange.workoutLiftId]?.workoutLiftEntity
                            ?: error("Workout lift ${updateChange.workoutLiftId} not found")

                val liftUpdate = updateChange.liftUpdate!!
                existing.copy(
                    workoutId = workoutId,
                    liftId = liftUpdate.liftId.valueOrDefault(existing.liftId),
                    position = liftUpdate.position.valueOrDefault(existing.position),
                    setCount = liftUpdate.setCount.valueOrDefault(existing.setCount),
                    progressionScheme = liftUpdate.progressionScheme.valueOrDefault(existing.progressionScheme),
                    deloadWeek = liftUpdate.deloadWeek.valueOrDefault(existing.deloadWeek),
                    repRangeTop = liftUpdate.repRangeTop.valueOrDefault(existing.repRangeTop),
                    repRangeBottom = liftUpdate.repRangeBottom.valueOrDefault(existing.repRangeBottom),
                    rpeTarget = liftUpdate.rpeTarget.valueOrDefault(existing.rpeTarget),
                    stepSize = liftUpdate.stepSize.valueOrDefault(existing.stepSize),
                    volumeCyclingSetCeiling = liftUpdate.volumeCyclingSetCeiling.valueOrDefault(existing.volumeCyclingSetCeiling),
                ).applyRemoteStorageMetadata(
                    remoteId = existing.remoteId,
                    remoteLastUpdated = existing.remoteLastUpdated,
                    synced = false
                )
            }

            workoutLiftsDao.updateMany(liftsToUpdate)
            Log.d("ProgramsRepositoryImpl", "Patched workout lifts for workout: $workoutId")
            liftsToUpdate.fastMap { it.id }
        } else emptyList()

        return upsertedWorkoutLiftIds
    }

    /**
     * Soft-delete the workout lifts and cascade to children as well as tangential tables.
     *
     * @param workoutChange Workout change details.
     * @return True if any workout lifts were soft-deleted.
     */
    private suspend fun softDeleteWorkoutLiftsAndCascade(workoutChange: WorkoutChange): Boolean {
        val removedLiftIds = workoutChange.removedWorkoutLiftIds.distinct()
        val deletedRows = workoutLiftsDao.softDeleteMany(removedLiftIds)
        if (deletedRows > 0) {
            customSetsDao.softDeleteByWorkoutLiftIds(removedLiftIds)
            Log.d("ProgramsRepositoryImpl", "Deleted workout lifts: $removedLiftIds")
        }

        return deletedRows > 0
    }

    /**
     * Upsert the set entities under the lifts.
     *
     * @param workoutChange Workout change details.
     * @param workoutLiftIdByLiftChange Map of LiftChange to canonical workoutLiftId.
     * @return True if any sets were upserted.
     */
    private suspend fun upsertSetEntities(workoutChange: WorkoutChange, workoutLiftIdByLiftChange: Map<LiftChange, Long>): Boolean {
        Log.d("ProgramsRepositoryImpl", "Upserting custom lift sets")
        val upsertSetEntities = mutableListOf<CustomLiftSetEntity>()
        val existingSetIds = workoutChange.lifts
            .flatMap { it.sets }
            .mapNotNull { setChange -> setChange.set.id.takeIf { id -> id != 0L } }

        val existingSetsById = if (existingSetIds.isNotEmpty())
            customSetsDao.getMany(existingSetIds).associateBy { it.id }
        else
            emptyMap()

        workoutChange.lifts
            .fastFlatMap{ liftChange ->
                liftChange.sets.fastMap { setChange ->
                    workoutLiftIdByLiftChange.getValue(liftChange) to setChange
                }
            }
            .fastForEach { (workoutLiftId, setChange) ->
                val setEntity = setChange.set
                    .toEntity()
                    .copy(workoutLiftId = workoutLiftId)
                val existingSet = existingSetsById[setEntity.id]
                upsertSetEntities += setEntity.applyRemoteStorageMetadata(
                    remoteId = existingSet?.remoteId,
                    remoteLastUpdated = existingSet?.remoteLastUpdated,
                    synced = false
                )
            }

        val hasSetsToUpsert = upsertSetEntities.isNotEmpty()
        if (hasSetsToUpsert) {
            customSetsDao.upsertMany(upsertSetEntities)
            Log.d("ProgramsRepositoryImpl", "Upserted custom lift sets: $upsertSetEntities")
        }

        return hasSetsToUpsert
    }

    /**
     * Container of ids for set deletion
     */
    private data class SetDeletionData(
        val workoutLiftIds: List<Long>,
        val setDeletions: List<SetDeletion>,
    )

    private data class SetDeletion(
        val workoutLiftId: Long,
        val setIds: List<Long>,
    )

    /**
     * Get ids for set deletion.
     *
     * @param workoutChange Workout change details.
     * @param workoutLiftIdByLiftChange Map of LiftChange to canonical workoutLiftId.
     * @return Set deletion ids.
     */
    private fun getSetDeletions(workoutChange: WorkoutChange, workoutLiftIdByLiftChange: Map<LiftChange, Long>): SetDeletionData {
        val workoutLiftIds = mutableListOf<Long>()
        val setDeletions = mutableListOf<SetDeletion>()

        workoutChange.lifts.forEach { liftChange ->
            val workoutLiftId = workoutLiftIdByLiftChange.getValue(liftChange)

            if (liftChange.removeAllSets) {
                workoutLiftIds.add(workoutLiftId)
                // enforce that sets/removedSetIds are empty already via validate()
                return@forEach
            }

            // Collect deletions
            if (liftChange.removedSetIds.isNotEmpty()) {
                setDeletions.add(SetDeletion(workoutLiftId, liftChange.removedSetIds))
            }
        }

        return SetDeletionData(workoutLiftIds, setDeletions)
    }
}