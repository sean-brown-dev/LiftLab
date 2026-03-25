package com.browntowndev.liftlab.core.domain.ai

import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramGenerationRequest
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramPayload

interface AiClient {
    /**
     * Given a ProgramGenerationRequest, generate and return a ProgramPayload.
     */
    suspend fun generateProgram(
        request: ProgramGenerationRequest,
    ): ProgramPayload

    /**
     * Given the original payload + issues, return a corrected ProgramPayload.
     */
    suspend fun fix(originalJson: String, correctionPrompt: String): ProgramPayload
}