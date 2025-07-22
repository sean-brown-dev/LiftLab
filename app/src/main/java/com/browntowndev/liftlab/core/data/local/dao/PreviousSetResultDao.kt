package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.data.local.entities.PreviousSetResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreviousSetResultDao: BaseDao<PreviousSetResultEntity> {
    @Query("SELECT * FROM previousSetResults WHERE synced = 0")
    suspend fun getAllUnsynced(): List<PreviousSetResultEntity>

    @Query("SELECT * FROM previousSetResults WHERE previously_completed_set_id = :id AND deleted = 0")
    suspend fun get(id: Long): PreviousSetResultEntity?

    @Transaction
    @Query("SELECT * FROM previousSetResults WHERE previously_completed_set_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<PreviousSetResultEntity>

    @Transaction
    @Query("SELECT * FROM previousSetResults WHERE deleted = 0")
    suspend fun getAll(): List<PreviousSetResultEntity>

    @Transaction
    @Query("SELECT * FROM previousSetResults WHERE deleted = 0")
    fun getAllFlow(): Flow<List<PreviousSetResultEntity>>

    @Query("DELETE FROM previousSetResults")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "(mesoCycle != :mesoCycle OR " +
            "microCycle != :microCycle) AND deleted = 0")
    fun getByWorkoutIdExcludingGivenMesoAndMicroFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<PreviousSetResultEntity>>

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle AND deleted = 0")
    fun getForWorkoutFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<PreviousSetResultEntity>>

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle AND deleted = 0")
    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResultEntity>

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE liftId = :liftId AND deleted = 0")
    suspend fun getForLift(liftId: Long): List<PreviousSetResultEntity>

    @Transaction
    @Query("SELECT liftId, MAX(oneRepMax) as 'personalRecord' " +
            "FROM previousSetResults " +
            "WHERE liftId IN (:liftIds) AND " +
            "(workoutId != :workoutId OR mesoCycle != :mesoCycle OR microCycle != :microCycle) " +
            "AND deleted = 0 " +
            "GROUP BY liftId")
    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>,
    ): List<PersonalRecordDto>

    @Query("""
    SELECT * FROM previousSetResults
    WHERE previously_completed_set_id IN (
        SELECT sr.previously_completed_set_id
        FROM previousSetResults sr
        WHERE sr.workoutId = :workoutId
          AND (
              sr.previously_completed_set_id IN (:currentResultsToDelete)
              OR (
                  sr.mesoCycle != :currentMesocycle
                  OR sr.microCycle != :currentMicrocycle
              )
          )
          AND sr.previously_completed_set_id NOT IN (
              SELECT psr.previously_completed_set_id
              FROM previousSetResults dsr
              INNER JOIN previousSetResults psr ON
                  dsr.liftId = psr.liftId AND
                  dsr.liftPosition = psr.liftPosition AND
                  dsr.workoutId = psr.workoutId
              WHERE dsr.previously_completed_set_id IN (:currentResultsToDelete)
                AND psr.previously_completed_set_id NOT IN (:currentResultsToDelete)
          )
    ) AND deleted = 0
""")
    suspend fun getAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDelete: List<Long>,
    ): List<PreviousSetResultEntity>

    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle AND deleted = 0")
    suspend fun getAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResultEntity>

    @Query("UPDATE previousSetResults SET deleted = 1, synced = 0 WHERE previously_completed_set_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE previousSetResults SET deleted = 1, synced = 0 WHERE previously_completed_set_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM previousSetResults WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): PreviousSetResultEntity?

    @Query("SELECT * FROM previousSetResults WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<PreviousSetResultEntity>
}