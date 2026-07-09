package com.erekeai.llm

data class ModelInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val selected: Boolean = false
)

