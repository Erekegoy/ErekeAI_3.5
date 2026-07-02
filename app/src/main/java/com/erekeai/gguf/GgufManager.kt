package com.erekeai.gguf

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class GgufFileInfo(val name: String, val sizeBytes: Long)

/**
 * ⚠️ "Local GGUF Manager" — ЧЕСТНО: управляет ФАЙЛАМИ моделей (.gguf) в хранилище приложения
 * (скачивание по прямой ссылке, список, удаление), но НЕ выполняет инференс на устройстве —
 * для реального запуска GGUF-модели нужна нативная библиотека llama.cpp, скомпилированная под
 * Android NDK (arm64-v8a), которую нельзя "просто написать текстом" в этой сессии: это отдельный
 * шаг сборки с Android Studio + NDK на вашей машине (см. комментарий в конце файла).
 * До тех пор используйте [com.erekeai.data.remote.provider.OllamaProvider] ("Ollama Manager") —
 * там инференс реально работает, просто не на самом телефоне, а на компьютере в сети.
 */
@Singleton
class GgufManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private fun modelsDir(): File = File(context.filesDir, "gguf_models").apply { mkdirs() }

    fun listModels(): List<GgufFileInfo> =
        modelsDir().listFiles()?.filter { it.isFile }?.map { GgufFileInfo(it.name, it.length()) } ?: emptyList()

    suspend fun download(url: String, fileName: String): Result<GgufFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(modelsDir(), fileName)
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "HTTP ${response.code}" }
                val body = response.body ?: error("Пустое тело ответа")
                target.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            GgufFileInfo(target.name, target.length())
        }
    }

    fun delete(fileName: String): Boolean = File(modelsDir(), fileName).delete()

    /*
     * Как реально включить инференс GGUF на телефоне (следующий шаг, вне этой сессии):
     * 1. Соберите нативную библиотеку из ggml-org/llama.cpp (папка examples/llama.android) под
     *    Android NDK — получится .so для arm64-v8a.
     * 2. Оберните её JNI-мостом (nativeLoadModel(path), nativeGenerate(prompt, callback)).
     * 3. Реализуйте AiProvider (LocalLlamaProvider), который читает файл из [modelsDir] через
     *    nativeLoadModel и стримит токены через nativeGenerate — интерфейс AiProvider.streamReply
     *    уже готов принять такую реализацию, менять остальной код приложения не нужно.
     */
}
