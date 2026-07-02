package com.erekeai.plugins

/**
 * Базовый контракт плагина ErekeAI. Плагины смогут добавлять новые команды,
 * инструменты или источники данных, не изменяя ядро приложения.
 *
 * Пример будущего плагина: "Погода", "Калькулятор", "Поиск в интернете".
 */
interface ErekePlugin {
    val id: String
    val displayName: String

    /** Может ли плагин обработать данный пользовательский ввод. */
    fun canHandle(input: String): Boolean

    /** Выполняет действие плагина и возвращает текстовый результат. */
    suspend fun execute(input: String): String
}

/**
 * Реестр плагинов — заполняется на старте приложения (или динамически).
 */
class PluginRegistry {
    private val plugins = mutableListOf<ErekePlugin>()

    fun register(plugin: ErekePlugin) {
        plugins.add(plugin)
    }

    fun findHandler(input: String): ErekePlugin? = plugins.firstOrNull { it.canHandle(input) }

    fun all(): List<ErekePlugin> = plugins.toList()
}
