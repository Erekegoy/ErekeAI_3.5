package com.erekeai.features.codeeditor.viewmodel

sealed interface CodeEditorIntent {
    data object Explain : CodeEditorIntent
    data object Fix : CodeEditorIntent
    data object Refactor : CodeEditorIntent
    data object Run : CodeEditorIntent
}
