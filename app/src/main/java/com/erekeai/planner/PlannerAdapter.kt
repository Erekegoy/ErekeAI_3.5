package com.erekeai.planner

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Role
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerAdapter @Inject constructor(
    private val providerRegistry: AiProviderRegistry
) : Planner {

    override suspend fun proposeFix(
        filePath: String,
        oldContent: String,
        errorLog: String
    ): Result<String> = runCatching {

        val provider =
            providerRegistry.getProvider(AiProviderType.OPENAI)

        val prompt = """
Ты senior Kotlin developer.

Исправь файл полностью.

Верни ТОЛЬКО содержимое файла.

Путь:
$filePath

Ошибка:
$errorLog

Файл:
$oldContent
""".trimIndent()

        val result = StringBuilder()

        provider.streamReply(
            listOf(
                ChatMessage(
                    conversationId = 0L,
                    role = Role.SYSTEM,
                    text = prompt
                )
            )
        ).collect {
            result.append(it)
        }

        result.toString()
            .removePrefix("```kotlin")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
