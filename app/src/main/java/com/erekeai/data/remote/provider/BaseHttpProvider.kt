package com.erekeai.data.remote.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Общая логика похода в REST API с потоковым (SSE-подобным) чтением ответа.
 * Конкретные провайдеры (Gemini/Groq/OpenAI) переопределяют [buildRequest] и [parseChunk].
 */
abstract class BaseHttpProvider(
    protected val client: OkHttpClient = OkHttpClient()
) {
    protected abstract fun buildRequest(prompt: String, apiKey: String): Request

    /** Извлекает текстовый фрагмент из одной строки SSE-потока ("data: {...}"), либо null. */
    protected abstract fun parseChunk(line: String): String?

    protected fun streamRequest(prompt: String, apiKey: String): Flow<String> = flow {
        val request = buildRequest(prompt, apiKey)
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Ошибка API: ${response.code} ${response.message}")
            }
            val body = response.body ?: return@use
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val chunk = parseChunk(line ?: continue)
                    if (!chunk.isNullOrEmpty()) emit(chunk)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    protected fun jsonBody(json: String) = json.toRequestBody("application/json".toMediaType())
}
