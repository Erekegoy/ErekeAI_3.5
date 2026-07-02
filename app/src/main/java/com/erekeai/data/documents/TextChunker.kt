package com.erekeai.data.documents

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Разбивает длинный текст на чанки подходящего размера для эмбеддинга и
 * контекстного окна модели. Режет по границам предложений/абзацев там, где
 * можно, с небольшим перекрытием между соседними чанками (чтобы не
 * разрывать мысль ровно на границе чанка и терять контекст при поиске).
 */
@Singleton
class TextChunker @Inject constructor() {

    fun chunk(text: String, maxChunkChars: Int = 1200, overlapChars: Int = 150): List<String> {
        val cleaned = text.replace(Regex("\\r\\n?"), "\n").trim()
        if (cleaned.isEmpty()) return emptyList()
        if (cleaned.length <= maxChunkChars) return listOf(cleaned)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < cleaned.length) {
            val end = (start + maxChunkChars).coerceAtMost(cleaned.length)
            var boundary = end

            if (end < cleaned.length) {
                // Try to break at the last paragraph/sentence boundary before `end`
                val searchWindow = cleaned.substring(start, end)
                val lastBreak = maxOf(
                    searchWindow.lastIndexOf("\n\n"),
                    searchWindow.lastIndexOf(". "),
                    searchWindow.lastIndexOf("! "),
                    searchWindow.lastIndexOf("? ")
                )
                if (lastBreak > maxChunkChars / 2) {
                    boundary = start + lastBreak + 1
                }
            }

            val piece = cleaned.substring(start, boundary).trim()
            if (piece.isNotEmpty()) chunks.add(piece)

            if (boundary >= cleaned.length) break
            start = (boundary - overlapChars).coerceAtLeast(start + 1)
        }

        return chunks
    }
}
