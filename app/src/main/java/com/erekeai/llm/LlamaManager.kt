package com.erekeai.llm

import android.content.Context
import android.util.Log
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

    companion object {
        private const val TAG = "ErekeAI"
    }

    private val engine: InferenceEngine =
        AiChat.getInferenceEngine(context)

    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {

        if (loaded) {
            Log.i(TAG, "Model already loaded.")
            return@withContext
        }

        try {

            Log.i(TAG, "Installing model...")

            val modelPath = ModelInstaller.install(context)

            Log.i(TAG, "Model path: $modelPath")

            Log.i(TAG, "Loading Qwen3...")

            engine.loadModel(modelPath)

            Log.i(TAG, "Model loaded successfully.")

            Log.i(TAG, "Loading system prompt...")

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

            Log.i(TAG, "System prompt loaded.")

            loaded = true

            Log.i(TAG, "Offline AI is ready.")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to load local model.", e)

            loaded = false

            throw e
        }
    }

    fun isLoaded(): Boolean = loaded

    fun generate(prompt: String): Flow<String> {
        return engine.sendUserPrompt(prompt)
    }

    fun unload() {
        Log.i(TAG, "Unloading model...")
        engine.cleanUp()
        loaded = false
    }
}
