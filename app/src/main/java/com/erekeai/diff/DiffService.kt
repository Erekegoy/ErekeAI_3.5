package com.erekeai.diff

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType

enum class LineType { UNCHANGED, ADDED, REMOVED }

data class DiffLine(
    val type: LineType,
    val text: String,
    /** Номер строки в старой версии файла, null если строка добавлена. */
    val oldLineNumber: Int? = null,
    /** Номер строки в новой версии файла, null если строка удалена. */
    val newLineNumber: Int? = null
)

interface DiffService {
    fun diff(oldText: String, newText: String): List<DiffLine>

    /** Быстрая текстовая сводка для логов/Memory: "+3 -1 строк". */
    fun summary(lines: List<DiffLine>): String
}

/**
 * Реализация на java-diff-utils (com.github.difflib).
 * Gradle: implementation("io.github.java-diff-utils:java-diff-utils:4.12")
 *
 * Возвращает чистую модель DiffLine — никакого HTML, поэтому ApprovalDialog
 * в Compose просто раскрашивает строки по типу (зелёный/красный/обычный).
 */
class JavaDiffUtilsService : DiffService {

    override fun diff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        val patch = DiffUtils.diff(oldLines, newLines)

        val result = mutableListOf<DiffLine>()
        var oldIndex = 0

        for (delta in patch.deltas) {
            // Строки без изменений перед текущей правкой
            while (oldIndex < delta.source.position) {
                result.add(
                    DiffLine(
                        type = LineType.UNCHANGED,
                        text = oldLines[oldIndex],
                        oldLineNumber = oldIndex + 1,
                        newLineNumber = oldIndex + 1
                    )
                )
                oldIndex++
            }

            when (delta.type) {
                DeltaType.DELETE -> {
                    delta.source.lines.forEachIndexed { i, line ->
                        result.add(
                            DiffLine(
                                type = LineType.REMOVED,
                                text = line,
                                oldLineNumber = delta.source.position + i + 1,
                                newLineNumber = null
                            )
                        )
                    }
                    oldIndex += delta.source.lines.size
                }
                DeltaType.INSERT -> {
                    delta.target.lines.forEachIndexed { i, line ->
                        result.add(
                            DiffLine(
                                type = LineType.ADDED,
                                text = line,
                                oldLineNumber = null,
                                newLineNumber = delta.target.position + i + 1
                            )
                        )
                    }
                }
                DeltaType.CHANGE -> {
                    delta.source.lines.forEachIndexed { i, line ->
                        result.add(
                            DiffLine(
                                type = LineType.REMOVED,
                                text = line,
                                oldLineNumber = delta.source.position + i + 1,
                                newLineNumber = null
                            )
                        )
                    }
                    delta.target.lines.forEachIndexed { i, line ->
                        result.add(
                            DiffLine(
                                type = LineType.ADDED,
                                text = line,
                                oldLineNumber = null,
                                newLineNumber = delta.target.position + i + 1
                            )
                        )
                    }
                    oldIndex += delta.source.lines.size
                }
                else -> Unit
            }
        }

        // Остаток неизменённых строк после последней правки
        while (oldIndex < oldLines.size) {
            result.add(
                DiffLine(
                    type = LineType.UNCHANGED,
                    text = oldLines[oldIndex],
                    oldLineNumber = oldIndex + 1,
                    newLineNumber = oldIndex + 1
                )
            )
            oldIndex++
        }

        return result
    }

    override fun summary(lines: List<DiffLine>): String {
        val added = lines.count { it.type == LineType.ADDED }
        val removed = lines.count { it.type == LineType.REMOVED }
        return "+$added -$removed строк"
    }
}
