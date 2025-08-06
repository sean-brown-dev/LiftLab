package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetPersonalRecordsUseCaseTest {

    private lateinit var setResultsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var setLogEntryRepository: SetLogEntryRepository
    private lateinit var getPersonalRecordsUseCase: GetPersonalRecordsUseCase

    @BeforeEach
    fun setUp() {
        setResultsRepository = mockk()
        setLogEntryRepository = mockk()
        getPersonalRecordsUseCase = GetPersonalRecordsUseCase(setLogEntryRepository)
    }

    @Test
    fun `invoke returns personal records`() = runTest {
        // Given
        val workoutId = 1L
        val mesoCycle = 1
        val microCycle = 1
        val liftIds = listOf(101L, 102L, 103L, 104L)

        val logPrs = listOf(
            PersonalRecord(liftId = 101L, personalRecord = 95),
            PersonalRecord(liftId = 102L, personalRecord = 200),
            PersonalRecord(liftId = 103L, personalRecord = 300) // Only in log
        )
        // 104L is in neither

        coEvery {
            setLogEntryRepository.getPersonalRecordsForLifts(liftIds)
        } returns logPrs

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(3, result.size)
        assertEquals(95, result[101L]?.personalRecord)
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
            setLogEntryRepository.getPersonalRecordsForLifts(any())
        } returns logPrs

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[101L]?.personalRecord)
    }

    @Test
    fun `invoke with empty log results returns none`() = runTest {
        // Given
        val workoutId = 1L
        val mesoCycle = 1
        val microCycle = 1
        val liftIds = listOf(101L)
        coEvery {
            setLogEntryRepository.getPersonalRecordsForLifts(any())
        } returns emptyList()

        // When
        val result = getPersonalRecordsUseCase(workoutId, mesoCycle, microCycle, liftIds)

        // Then
        assertEquals(0, result.size)
    }
}
