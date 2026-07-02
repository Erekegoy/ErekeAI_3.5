package com.erekeai.data.tools

import android.content.Context
import com.erekeai.data.documents.DocumentTextExtractor
import com.erekeai.data.documents.TextChunker
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import com.erekeai.domain.vector.EmbeddingProviderRegistry
import com.erekeai.domain.vector.VectorChunk
import com.erekeai.domain.vector.VectorStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private fun agentDocumentsDir(context: Context): File =
    File(context.filesDir, "agent_documents").apply { mkdirs() }

/**
 * Импортирует документ (PDF/DOCX/XLSX/TXT/MD) из песочницы agent_documents
 * в векторную БД: извлекает текст, режет на чанки, считает эмбеддинги и
 * сохраняет их — это делает документ доступным для knowledge_base_search.
 * Реальная работа, не заглушка: без настроенного ключа Gemini или OpenAI
 * (для эмбеддингов) инструмент честно откажет, а не притворится, что сработал.
 */
@Singleton
class ImportDocumentTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extractor: DocumentTextExtractor,
    private val chunker: TextChunker,
    private val embeddingRegistry: EmbeddingProviderRegistry,
    private val vectorStore: VectorStore
) : Tool {

    override val definition = ToolDefinition(
        name = "import_document",
        description = "Индексирует документ (PDF/DOCX/XLSX/TXT/MD) из папки документов в векторную базу знаний для последующего поиска через knowledge_base_search",
        parameters = listOf(ToolParameter("filename", "точное имя файла, включая расширение (см. list_files)"))
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val filename = args["filename"]?.trim()
        if (filename.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'filename'")
        }

        val provider = embeddingRegistry.resolveConfigured()
            ?: return@withContext ToolResult(
                false,
                "Не настроен ни один провайдер эмбеддингов. Добавь GEMINI_API_KEY или OPENAI_API_KEY в Настройках."
            )

        val dir = agentDocumentsDir(context)
        val target = File(dir, filename)
        if (!target.canonicalPath.startsWith(dir.canonicalPath)) {
            return@withContext ToolResult(false, "Недопустимый путь к файлу")
        }
        if (!target.exists() || !target.isFile) {
            return@withContext ToolResult(false, "Файл '$filename' не найден")
        }

        try {
            val text = extractor.extract(target)
            if (text.isBlank()) {
                return@withContext ToolResult(false, "Не удалось извлечь текст из '$filename' (файл пуст или это скан без текстового слоя)")
            }

            val pieces = chunker.chunk(text)
            vectorStore.deleteBySource(filename) // повторный импорт заменяет старую версию

            val chunks = pieces.mapIndexed { index, piece ->
                VectorChunk(
                    sourceId = filename,
                    sourceName = filename,
                    chunkIndex = index,
                    text = piece,
                    embedding = provider.embed(piece),
                    embeddingProviderId = provider.id
                )
            }
            vectorStore.upsert(chunks)

            ToolResult(true, "Документ '$filename' проиндексирован: ${chunks.size} фрагмент(ов) через ${provider.id}. Теперь по нему можно искать через knowledge_base_search.")
        } catch (e: Exception) {
            ToolResult(false, "Ошибка индексации '$filename': ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

/**
 * Ищет наиболее релевантные фрагменты в проиндексированных документах по
 * смысловому сходству (векторный поиск), а не по точному совпадению слов.
 */
@Singleton
class KnowledgeBaseSearchTool @Inject constructor(
    private val embeddingRegistry: EmbeddingProviderRegistry,
    private val vectorStore: VectorStore
) : Tool {

    override val definition = ToolDefinition(
        name = "knowledge_base_search",
        description = "Ищет релевантные фрагменты в проиндексированных документах (после import_document) по смыслу запроса",
        parameters = listOf(
            ToolParameter("query", "поисковый запрос"),
            ToolParameter("top_k", "сколько фрагментов вернуть, по умолчанию 5", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.trim()
        if (query.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'query'")
        }
        val topK = args["top_k"]?.toIntOrNull()?.coerceIn(1, 20) ?: 5

        val provider = embeddingRegistry.resolveConfigured()
            ?: return@withContext ToolResult(false, "Не настроен ни один провайдер эмбеддингов")

        try {
            val queryEmbedding = provider.embed(query)
            val results = vectorStore.search(queryEmbedding, provider.id, topK)

            if (results.isEmpty()) {
                return@withContext ToolResult(true, "В базе знаний ничего не найдено. Возможно, документы ещё не проиндексированы через import_document.")
            }

            val formatted = results.joinToString("\n\n") { r ->
                "[${r.chunk.sourceName} #${r.chunk.chunkIndex}, сходство ${"%.2f".format(r.score)}]\n${r.chunk.text}"
            }
            ToolResult(true, formatted)
        } catch (e: Exception) {
            ToolResult(false, "Ошибка поиска: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

/**
 * Список уже проиндексированных источников — чтобы модель (и пользователь)
 * знали, что уже доступно для knowledge_base_search.
 */
@Singleton
class ListKnowledgeBaseSourcesTool @Inject constructor(
    private val vectorStore: VectorStore
) : Tool {

    override val definition = ToolDefinition(
        name = "list_knowledge_base_sources",
        description = "Показывает, какие документы уже проиндексированы в базе знаний",
        parameters = emptyList()
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val sources = vectorStore.listSources()
        if (sources.isEmpty()) {
            ToolResult(true, "База знаний пуста. Используй import_document, чтобы проиндексировать документ.")
        } else {
            ToolResult(true, sources.joinToString("\n") { "- $it" })
        }
    }
}
