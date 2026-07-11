package com.erekeai.llm

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object ModelManager {

    private const val PREFS = "llm_models"
    private const val KEY_ACTIVE = "active_model"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    /** Список всех установленных .gguf моделей. */
    fun getModels(context: Context): List<ModelInfo> {

        val active = getActiveModelPath(context)

        return modelsDir(context)
            .listFiles()
            ?.filter { it.extension.equals("gguf", true) }
            ?.sortedBy { it.name.lowercase() }
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

    /** Есть ли хотя бы одна установленная модель. */
    fun hasModels(context: Context): Boolean = getModels(context).isNotEmpty()

    fun setActiveModel(context: Context, path: String) {
        prefs(context).edit().putString(KEY_ACTIVE, path).apply()
    }

    fun getActiveModelPath(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)

    /** Объект активной модели, либо null если ничего не выбрано / модель была удалена. */
    fun getActiveModel(context: Context): ModelInfo? {
        val activePath = getActiveModelPath(context) ?: return null
        return getModels(context).firstOrNull { it.path == activePath }
    }

    /**
     * Копирует выбранный пользователем .gguf файл (из системного файлового менеджера)
     * во внутреннее хранилище приложения. Возвращает ModelInfo новой модели или null,
     * если файл не .gguf / произошла ошибка чтения.
     *
     * Если после импорта активная модель ещё не выбрана — новая модель становится активной.
     */
    fun importModel(context: Context, uri: Uri): ModelInfo? {
        val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "model.gguf"
        if (!displayName.endsWith(".gguf", ignoreCase = true)) return null

        val targetFile = uniqueFileFor(context, displayName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (getActiveModelPath(context) == null) {
                setActiveModel(context, targetFile.absolutePath)
            }

            ModelInfo(
                id = targetFile.name,
                name = targetFile.nameWithoutExtension,
                path = targetFile.absolutePath,
                size = targetFile.length(),
                selected = targetFile.absolutePath == getActiveModelPath(context)
            )
        } catch (e: Exception) {
            targetFile.delete()
            null
        }
    }

    /** Удаляет модель с диска. Если она была активной — сбрасывает активную модель. */
    fun deleteModel(context: Context, path: String): Boolean {
        val file = File(path)
        val wasActive = getActiveModelPath(context) == path
        val deleted = !file.exists() || file.delete()

        if (deleted && wasActive) {
            prefs(context).edit().remove(KEY_ACTIVE).apply()
        }
        return deleted
    }

    /** Переименовывает модель (без расширения .gguf в newName). Возвращает обновлённый ModelInfo. */
    fun renameModel(context: Context, path: String, newName: String): ModelInfo? {
        val file = File(path)
        if (!file.exists()) return null

        val sanitized = newName.trim().removeSuffix(".gguf")
        if (sanitized.isBlank()) return null

        val wasActive = getActiveModelPath(context) == path
        val newFile = uniqueFileFor(context, "$sanitized.gguf", excluding = file)

        if (!file.renameTo(newFile)) return null

        if (wasActive) {
            setActiveModel(context, newFile.absolutePath)
        }

        return ModelInfo(
            id = newFile.name,
            name = newFile.nameWithoutExtension,
            path = newFile.absolutePath,
            size = newFile.length(),
            selected = wasActive
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Гарантирует уникальное имя файла в modelsDir, добавляя (2), (3)... при коллизии. */
    private fun uniqueFileFor(context: Context, desiredName: String, excluding: File? = null): File {
        val dir = modelsDir(context)
        var candidate = File(dir, desiredName)
        if (candidate == excluding || !candidate.exists()) return candidate

        val base = desiredName.removeSuffix(".gguf")
        var index = 2
        while (candidate.exists() && candidate != excluding) {
            candidate = File(dir, "$base ($index).gguf")
            index++
        }
        return candidate
    }
}
