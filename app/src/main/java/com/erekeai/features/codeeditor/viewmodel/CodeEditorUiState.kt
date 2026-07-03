package com.erekeai.features.codeeditor.viewmodel

data class CodeEditorUiState(
    val text: String = "",
    val isLoading: Boolean = false,
    val fileName: String = "",
    val status: String = ""
)
