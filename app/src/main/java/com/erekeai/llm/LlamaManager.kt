package com.erekeai.llm

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaManager @Inject constructor(
    context: Context
) {

    private val engine: InferenceEngine =
        AiChat.getInferenceEngine(context)

    private var loaded = false

    suspend fun load(modelPath: String) {
        if (loaded) return

        engine.loadModel(modelPath)

        engine.setSystemPrompt(
            """
            You are ErekeAI.

            You are an offline AI assistant.

            Answer in the user's language.

            Think carefully.

            Be concise.

            If internet is required, tell the application.
            """.trimIndent()
        )

        loaded = true
    }

    fun isLoaded(): Boolean = loaded

    fun generate(prompt: String): Flow<String> {
        return engine.sendUserPrompt(prompt)
    }

    fun unload() {
        engine.cleanUp()
        loaded = false
    }
}
