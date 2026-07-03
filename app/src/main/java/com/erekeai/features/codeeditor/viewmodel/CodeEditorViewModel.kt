package com.erekeai.features.codeeditor.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CodeEditorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CodeEditorUiState())
    val uiState: StateFlow<CodeEditorUiState> = _uiState

    fun open(name: String, content: String) {
        _uiState.value = CodeEditorUiState(
            fileName = name,
            text = content
        )
    }

    fun update(text: String) {
        _uiState.value = _uiState.value.copy(text = text)
    }

    fun status(text: String) {
        _uiState.value = _uiState.value.copy(status = text)
    }
}
