package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetPersonalRecordsUseCaseTest {

    private lateinit var setResultsRepository: PreviousSetResultsRepository
    private lateinit var setLogEntryRepository: SetLogEntryRepository
    private lateinit var getPersonalRecordsUseCase: GetPersonalRecordsUseCase

    @BeforeEach
    fun setUp() {
        setResultsRepository = mockk()
        setLogEntryRepository = mockk()
        getPersonalRecordsUseCase = GetPersonalRecordsUseCase(setResultsRepository, setLogEntryRepository)
    }

    @Test
    fun `invoke returns merged personal records`() = runTest {
        // Given
        val workoutId = 1L
        val mesoCycle = 1
        val microCycle = 1
        val liftIds = listOf(101L, 102L, 103L, 104L)

        val prevWorkoutPrs = listOf(
            PersonalRecord(liftId = 101L, personalRecord = 100), // Higher than log
            PersonalRecord(liftId = 102L, personalRecord = 200)  // Same as log
        )
        val logPrs = listOf(
            PersonalRecord(liftId = 101L, personalRecord = 95),
            PersonalRecord(liftId = 102L, personalRecord = 200),
            PersonalRecord(liftId = 103L, personalRecord = 300) // Only in log
        )
        // 104L is in neither

        coEvery {
            setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(workoutId, mesoCycle, microCycle, liftIds)
        } returns prevWorkoutPrs

        coEvery {
            setLogEntryRepository.getPersonalRecordsForLifts(liftIds)
        } returns logPrs

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(3, result.size)
        assertEquals(100, result[101L]?.personalRecord)
        assertEquals(200, result[102L]?.personalRecord)
        assertEquals(300, result[103L]?.personalRecord)
    }

    @Test
    fun `invoke with empty previous results returns only log records`() = runTest {
        // Given
        val workoutId = 1L
        val mesoCycle = 1
        val microCycle = 1
        val liftIds = listOf(101L)
        val logPrs = listOf(PersonalRecord(liftId = 101L, personalRecord = 100))

        coEvery {
            setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any())
        } returns emptyList()
        coEvery {
            setLogEntryRepository.getPersonalRecordsForLifts(any())
        } returns logPrs

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[101L]?.personalRecord)
    }

    @Test
    fun `invoke with empty log results returns only previous records`() = runTest {
        // Given
        val workoutId = 1L
        val mesoCycle = 1
        val microCycle = 1
        val liftIds = listOf(101L)
        val prevWorkoutPrs = listOf(PersonalRecord(liftId = 101L, personalRecord = 100))

        coEvery {
            setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any())
        } returns prevWorkoutPrs
        coEvery {
            setLogEntryRepository.getPersonalRecordsForLifts(any())
        } returns emptyList()

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[101L]?.personalRecord)
    }
}
