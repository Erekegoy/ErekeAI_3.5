package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Возвращает текущую дату и время устройства — модели неизвестно "сегодня",
 * этот инструмент закрывает базовый пробел для задач планирования и напоминаний.
 */
@Singleton
class DateTimeTool @Inject constructor() : Tool {

    override val definition = ToolDefinition(
        name = "current_datetime",
        description = "Возвращает текущую дату и время на устройстве пользователя",
        parameters = emptyList()
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm", Locale("ru"))
        return ToolResult(true, formatter.format(Date()))
    }
}
