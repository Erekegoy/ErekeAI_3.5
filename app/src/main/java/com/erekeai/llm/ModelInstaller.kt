package com.erekeai.llm

import android.content.Context
import java.io.File

object ModelInstaller {

    private const val MODEL_NAME = "Qwen3.gguf"

    fun install(context: Context): String {

        val modelsDir = File(context.filesDir, "models")

        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val modelFile = File(modelsDir, MODEL_NAME)

        if (!modelFile.exists()) {
            context.assets.open("models/$MODEL_NAME").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return modelFile.absolutePath
    }
}
