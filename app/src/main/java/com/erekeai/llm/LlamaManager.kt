package com.erekeai.llm

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    private val engine: InferenceEngine =
        AiChat.getInferenceEngine(context)

    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {

        if (loaded) return@withContext

        val modelPath = ModelInstaller.install(context)

        engine.loadModel(modelPath)

        engine.setSystemPrompt(
            """
            You are ErekeAI.

            You are an offline AI assistant.

            Always answer in the user's language.

            Think carefully before answering.

            Keep answers concise unless more detail is requested.

            You work completely offline.

            If internet access is required, reply exactly:

            [INTERNET_REQUIRED]

            Do not invent information.
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
