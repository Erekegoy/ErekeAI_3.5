package com.erekeai.data.tools.dev

import android.content.Context
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧠 "Планирование разработки". Строит структурированный markdown-план (цель, шаги,
 * риски, критерии готовности) и сохраняет его в песочницу агента (папка "plans"),
 * чтобы пользователь мог вернуться к нему позже, а модель — сослаться на него в
 * последующих шагах ReAct-цикла.
 */
@Singleton
class DevPlanTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "create_dev_plan",
        description = "Создаёт и сохраняет структурированный план разработки (цель, шаги, риски, критерии готовности)",
        parameters = listOf(
            ToolParameter("goal", "цель задачи одним предложением"),
            ToolParameter(
                "steps",
                "шаги плана, разделённые переносом строки или ';'. Если не указаны — будет создан шаблон",
                required = false
            ),
            ToolParameter("risks", "известные риски/неопределённости, через ';'", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val goal = args["goal"]?.trim()
        if (goal.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'goal'")
        }

        val steps = args["steps"]?.split(Regex("[;\n]"))?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: listOf("Проанализировать текущий код/проект (analyze_project, code_search)",
                "Определить минимальный набор изменений",
                "Внести изменения (write_file)",
                "Проверить результат (run_static_checks)",
                "Подвести итог и предложить дальнейшие шаги")

        val risks = args["risks"]?.split(Regex("[;\n]"))?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: listOf("Не указаны — уточните ограничения окружения и требования перед началом работы")

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        val markdown = buildString {
            appendLine("# План разработки")
            appendLine()
            appendLine("**Создан:** ${SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(Date())}")
            appendLine()
            appendLine("## Цель")
            appendLine(goal)
            appendLine()
            appendLine("## Шаги")
            steps.forEachIndexed { index, step -> appendLine("${index + 1}. [ ] $step") }
            appendLine()
            appendLine("## Риски и неопределённости")
            risks.forEach { appendLine("- $it") }
            appendLine()
            appendLine("## Критерии готовности")
            appendLine("- Изменения внесены и сохранены (write_file)")
            appendLine("- Проверки пройдены без критичных замечаний (run_static_checks)")
            appendLine("- Результат соответствует исходной цели")
        }

        val plansDir = File(devSandboxDir(context), "plans").apply { mkdirs() }
        val file = File(plansDir, "plan_$timestamp.md")

        return@withContext try {
            file.writeText(markdown)
            ToolResult(true, "$markdown\n\n(сохранено в plans/${file.name})")
        } catch (e: Exception) {
            // Даже если не удалось сохранить на диск, отдаём модели сам план — он всё равно полезен.
            ToolResult(true, "$markdown\n\n⚠️ Не удалось сохранить файл плана: ${e.message}")
        }
    }
}
