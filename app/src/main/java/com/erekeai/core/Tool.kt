package com.erekeai.core

/**
 * Уровень риска инструмента.
 * READ        — безопасно, выполняется без подтверждения.
 * WRITE       — изменяет файлы/репозиторий, требует Approval.
 * DESTRUCTIVE — потенциально необратимо (удаление, force push), требует отдельного явного Approval.
 */
enum class Permission {
    READ,
    WRITE,
    DESTRUCTIVE
}

/**
 * Результат выполнения инструмента.
 * Используем sealed class вместо Result<String>, чтобы явно различать
 * "успех с данными" и "успех без данных" и упростить логирование в Memory.
 */
sealed class ToolResult {
    data class Success(val output: String = "") : ToolResult()
    data class Failure(val error: String, val cause: Throwable? = null) : ToolResult()
}

/**
 * Базовый контракт инструмента.
 *
 * ВАЖНО: любая новая интеграция (Git, Terminal, Search, LSP) должна
 * реализовывать именно этот интерфейс и регистрироваться в ToolRegistry,
 * а не вызываться напрямую из Planner/Executor/Agent.
 */
interface Tool {
    val id: String
    val permission: Permission

    suspend fun execute(args: Map<String, String>): ToolResult
}
