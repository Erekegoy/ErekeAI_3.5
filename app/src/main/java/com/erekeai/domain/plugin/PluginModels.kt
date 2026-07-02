package com.erekeai.domain.plugin

/**
 * 🟡 "Плагин" ErekeAI в этой реализации — ДЕКЛАРАТИВНОЕ описание обёртки над HTTP API
 * (JSON-манифест), а НЕ произвольный исполняемый код. Это осознанное и важное решение по
 * безопасности: загрузка и динамическое выполнение чужого кода (DexClassLoader / скачанных
 * .dex/.jar) внутри работающего приложения — это классический вектор для инъекции вредоносного
 * кода, особенно для ассистента с доступом к файлам/сети/памяти пользователя. Манифест же не
 * может ничего "выполнить" сам по себе — он просто описывает, как собрать HTTP-запрос, поэтому
 * плагин из недоверенного репозитория в худшем случае бесполезен, но не опасен.
 */
data class PluginManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val method: String,               // GET | POST
    val urlTemplate: String,          // например "https://api.example.com/search?q={query}"
    val parameters: List<PluginParameter>,
    val bodyTemplate: String? = null, // для POST, тоже с {placeholder}
    val authHeaderName: String? = null,
    /** id ключа в SecureKeyStore, где хранится значение для authHeaderName (пользователь вводит сам). */
    val authKeyId: String? = null
)

data class PluginParameter(val name: String, val description: String, val required: Boolean = true)

data class InstalledPlugin(val manifest: PluginManifest, val installedAt: Long, val sourceRepoUrl: String)
