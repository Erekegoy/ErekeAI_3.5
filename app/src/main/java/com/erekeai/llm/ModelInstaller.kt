package com.erekeai.llm

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileNotFoundException

object ModelInstaller {

    private val MODEL_NAMES = listOf(
        "Qwen3.gguf",
        "Qwen3-4B-Q4_K_M1.gguf"
    )

    fun install(context: Context): String {

        val modelsDir = File(context.filesDir, "models")

        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        // Уже установлена
        MODEL_NAMES.forEach { name ->
            val installed = File(modelsDir, name)
            if (installed.exists()) {
                return installed.absolutePath
            }
        }

        // Ищем в Download
        val downloadDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        MODEL_NAMES.forEach { name ->

            val source = File(downloadDir, name)

            if (source.exists()) {

                val target = File(modelsDir, name)

                source.inputStream().use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                return target.absolutePath
            }
        }

        // Пробуем assets
        MODEL_NAMES.forEach { name ->

            try {

                val target = File(modelsDir, name)

                context.assets.open("models/$name").use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                return target.absolutePath

            } catch (_: Exception) {
            }
        }

        throw FileNotFoundException(
            "Не найдена модель.\n\n" +
                    "Положите один из файлов:\n" +
                    MODEL_NAMES.joinToString("\n") +
                    "\n\nв папку Download."
        )
    }
}
