package com.erekeai.domain.tool

/**
 * Описание одного параметра инструмента — используется, чтобы объяснить модели
 * (в системном промпте), какие аргументы и в каком формате нужно передать.
 */
data class ToolParameter(
    val name: String,
    val description: String,
    val required: Boolean = true
)

/**
 * Метаданные инструмента, видимые языковой модели при выборе действия.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList()
)

/**
 * Результат выполнения инструмента.
 */
data class ToolResult(
    val success: Boolean,
    val content: String
)
