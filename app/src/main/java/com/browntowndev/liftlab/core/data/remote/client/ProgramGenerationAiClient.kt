package com.browntowndev.liftlab.core.data.remote.client

import android.util.Log
import com.browntowndev.liftlab.core.domain.ai.AiClient
import com.browntowndev.liftlab.core.domain.ai.buildProgramPrompt
import com.browntowndev.liftlab.core.domain.ai.programResponseSchema
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramGenerationRequest
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramPayload
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

class ProgramGenerationAiClient: AiClient {
    companion object {
        private const val GEMINI_PRO = "gemini-2.5-flash"
        private const val GEMINI_FLASH = "gemini-2.5-flash-lite"
    }

    override suspend fun generateProgram(
        request: ProgramGenerationRequest
    ): ProgramPayload {
        val prompt = buildProgramPrompt(request)
        val payload = submitPrompt(prompt, GEMINI_PRO)
        return payload
    }

    override suspend fun fix(
        originalJson: String,
        correctionPrompt: String
    ): ProgramPayload {
        val payload = submitPrompt(correctionPrompt, GEMINI_FLASH)
        return payload
    }

    private suspend fun submitPrompt(prompt: String, model: String): ProgramPayload {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = model,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = programResponseSchema()
                    temperature = 0.3f
                }
            )

        val full = content {
            text(prompt)
        }

        val response = model.generateContent(full)
        val programJson = response.text ?: error("Firebase AI returned an empty response")

        return runCatching {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<ProgramPayload>(programJson)
        }.getOrElse {
            Log.e("ProgramGenerationAiClient", "Firebase AI returned invalid JSON", it)
            error("Firebase AI returned invalid JSON")
        }
    }
}