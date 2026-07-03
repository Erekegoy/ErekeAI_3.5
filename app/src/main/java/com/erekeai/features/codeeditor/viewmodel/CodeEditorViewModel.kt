package com.erekeai.features.codeeditor.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CodeEditorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CodeEditorUiState())
    val uiState = _uiState.asStateFlow()

    fun open(name: String, text: String) {
        _uiState.value = CodeEditorUiState(
            text = text,
            fileName = name
        )
    }

    fun update(text: String) {
        _uiState.value = _uiState.value.copy(
            text = text
        )
    }

    fun status(message: String) {
        _uiState.value = _uiState.value.copy(
            status = message
        )
    }
}
