package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import android.util.Log
import com.browntowndev.liftlab.core.domain.ai.AiClient
import com.browntowndev.liftlab.core.domain.ai.validateAndTryCorrect
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.extensions.toProgramDomainModel
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramGenerationRequest
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class GenerateProgramUseCase(
    private val aiClient: AiClient,
    private val liftsRepository: LiftsRepository,
) {
    suspend operator fun invoke(
        workoutCount: Int,
        muscleGroupsToSpecialize: Set<VolumeType>,
        deloadWeek: Int
    ): Program? {
        val lifts = liftsRepository.getAll()
        val request = ProgramGenerationRequest(
            microcycleWorkoutCount = workoutCount,
            specializationMuscles = muscleGroupsToSpecialize,
            deloadEvery = deloadWeek,
            liftCatalog = lifts,
        )

        var retries = 0
        var program: Program? = null

        while (retries < 3 && program == null) {
            try {
                val aiProgram = aiClient.generateProgram(request = request)
                val validatedAiProgram = aiProgram.validateAndTryCorrect(request, aiClient)
                program = validatedAiProgram.toProgramDomainModel(lifts)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e("GenerateProgramUseCase", "Error generating program", e)
                retries++
                program = null
            }
        }

        return program
    }
}