package com.erekeai.llm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object ModelPicker {

    fun importModel(
        context: Context,
        uri: Uri
    ): File {

        val modelsDir = File(context.filesDir, "models").apply {
            mkdirs()
        }

        val fileName = queryDisplayName(context, uri)
            ?: "model_${System.currentTimeMillis()}.gguf"

        require(fileName.lowercase().endsWith(".gguf")) {
            "Можно выбрать только GGUF-модель."
        }

        val target = File(modelsDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Не удалось открыть файл.")

        return target
    }

    private fun queryDisplayName(
        context: Context,
        uri: Uri
    ): String? {

        val cursor =
            context.contentResolver.query(uri, null, null, null, null)
                ?: return null

        cursor.use {

            val index =
                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (index >= 0 && it.moveToFirst()) {
                return it.getString(index)
            }
        }

        return null
    }
}
