package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Простой и безопасный вычислитель арифметических выражений (+ - * / (), десятичные числа).
 * Не использует eval/Rhino/JS-движки — только собственный рекурсивный разбор,
 * поэтому не может выполнить произвольный код.
 */
@Singleton
class CalculatorTool @Inject constructor() : Tool {

    override val definition = ToolDefinition(
        name = "calculator",
        description = "Вычисляет арифметическое выражение, например (2 + 3) * 4 / 2",
        parameters = listOf(ToolParameter("expression", "арифметическое выражение"))
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val expression = args["expression"]?.trim()
        if (expression.isNullOrBlank()) {
            return ToolResult(false, "Не указан параметр 'expression'")
        }
        return try {
            val result = ExpressionEvaluator(expression).evaluate()
            ToolResult(true, formatNumber(result))
        } catch (e: Exception) {
            ToolResult(false, "Не удалось вычислить выражение: ${e.message}")
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}

/**
 * Рекурсивный разбор арифметического выражения (грамматика: expr -> term (+|-) term ...,
 * term -> factor (*|/) factor ..., factor -> number | '(' expr ')' | '-' factor).
 */
private class ExpressionEvaluator(private val input: String) {
    private var pos = 0

    fun evaluate(): Double {
        val result = parseExpression()
        skipWhitespace()
        if (pos != input.length) throw IllegalArgumentException("Неожиданный символ на позиции $pos")
        return result
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            when (currentChar()) {
                '+' -> { pos++; value += parseTerm() }
                '-' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            when (currentChar()) {
                '*' -> { pos++; value *= parseFactor() }
                '/' -> {
                    pos++
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("деление на ноль")
                    value /= divisor
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        skipWhitespace()
        return when (currentChar()) {
            '-' -> { pos++; -parseFactor() }
            '+' -> { pos++; parseFactor() }
            '(' -> {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (currentChar() != ')') throw IllegalArgumentException("Ожидалась ')'")
                pos++
                value
            }
            else -> parseNumber()
        }
    }

    private fun parseNumber(): Double {
        skipWhitespace()
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        if (start == pos) throw IllegalArgumentException("Ожидалось число на позиции $pos")
        return input.substring(start, pos).toDouble()
    }

    private fun currentChar(): Char? = if (pos < input.length) input[pos] else null

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }
}
