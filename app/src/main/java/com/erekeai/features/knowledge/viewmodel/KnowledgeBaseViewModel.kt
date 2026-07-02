package com.erekeai.features.knowledge.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erekeai.data.tools.ImportDocumentTool
import com.erekeai.domain.tool.ToolResult
import com.erekeai.domain.vector.VectorStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class KnowledgeBaseUiState(
    val indexedSources: List<String> = emptyList(),
    val isImporting: Boolean = false,
    val lastMessage: String? = null
)

@HiltViewModel
class KnowledgeBaseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importDocumentTool: ImportDocumentTool,
    private val vectorStore: VectorStore
) : ViewModel() {

    private val _state = MutableStateFlow(KnowledgeBaseUiState())
    val state: StateFlow<KnowledgeBaseUiState> = _state.asStateFlow()

    init {
        refreshSources()
    }

    /**
     * Копирует файл, выбранный через системный File Picker (SAF), в песочницу
     * agent_documents под его исходным именем, затем сразу индексирует его.
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, lastMessage = null)

            val fileName = withContext(Dispatchers.IO) {
                queryDisplayName(uri) ?: "document_${System.currentTimeMillis()}"
            }

            val copyResult = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.filesDir, "agent_documents").apply { mkdirs() }
                    val target = File(dir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Не удалось открыть выбранный файл")
                    target
                }
            }

            val message = copyResult.fold(
                onSuccess = { file ->
                    val result: ToolResult = importDocumentTool.execute(mapOf("filename" to file.name))
                    result.content
                },
                onFailure = { e -> "Ошибка копирования файла: ${e.message}" }
            )

            _state.value = _state.value.copy(isImporting = false, lastMessage = message)
            refreshSources()
        }
    }

    fun refreshSources() {
        viewModelScope.launch {
            val sources = vectorStore.listSources()
            _state.value = _state.value.copy(indexedSources = sources)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) return it.getString(nameIndex)
        }
        return null
    }
}
