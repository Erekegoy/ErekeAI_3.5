package com.erekeai.llm

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object ModelManager {

    private const val PREFS = "llm_models"
    private const val KEY_ACTIVE = "active_model"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun getModels(context: Context): List<ModelInfo> {

        val active = getActiveModelPath(context)

        return modelsDir(context)
            .listFiles()
            ?.filter { it.extension.equals("gguf", true) }
            ?.map {
                ModelInfo(
                    id = it.name,
                    name = it.nameWithoutExtension,
                    path = it.absolutePath,
                    size = it.length(),
                    selected = it.absolutePath == active
                )
            }
            ?: emptyList()
    }

    fun setActiveModel(context: Context, path: String) {
        prefs(context).edit().putString(KEY_ACTIVE, path).apply()
    }

    fun getActiveModelPath(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)
}
