package com.erekeai.core

/**
 * Уровень риска инструмента.
 * READ        — безопасно, выполняется без подтверждения.
 * WRITE       — изменяет файлы/репозиторий, требует Approval.
 * DESTRUCTIVE — потенциально необратимо (удаление, force push), требует отдельного явного Approval.
 *
 * Используется исполнителем автофиксов (SimpleFixExecutor/RetryingFixExecutor) и ApprovalService.
 * ВАЖНО: основной контракт инструментов агента — это com.erekeai.domain.tool.Tool
 * (см. ToolModule) — раньше здесь же лежали дублирующие interface Tool / ToolResult /
 * class ToolRegistry, которые нигде не импортировались и были удалены как мёртвый код,
 * чтобы не путать с реальной системой инструментов в domain.tool.*.
 */
enum class Permission {
    READ,
    WRITE,
    DESTRUCTIVE
}
