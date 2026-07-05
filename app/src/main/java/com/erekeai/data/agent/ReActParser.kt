package com.erekeai.data.agent

import org.json.JSONObject

data class ParsedStep(
    val thought: String?,
    val action: ParsedAction?,
    val finalAnswer: String?
)

data class ParsedAction(
    val toolName: String,
    val args: Map<String, String>
)

object ReActParser {

    private val thoughtRegex = Regex(
        "Thought:\\s*(.*?)(?=\\n(?:Action|Final Answer):|\\z)",
        RegexOption.DOT_MATCHES_ALL
    )

    private val finalRegex = Regex(
        "Final Answer:\\s*(.*)",
        RegexOption.DOT_MATCHES_ALL
    )

    private val actionRegex = Regex(
        "Action:\\s*(.+)"
    )

    private val actionInputRegex = Regex(
        "Action Input:\\s*(\\{.*?\\})",
        RegexOption.DOT_MATCHES_ALL
    )

    private val functionRegex = Regex(
        """([A-Za-z0-9_]+)\((.*?)\)""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parse(raw: String): ParsedStep {

        val text = raw.trim()

        parseJsonTool(text)?.let {
            return ParsedStep(null, it, null)
        }

        parseFunctionCall(text)?.let {
            return ParsedStep(null, it, null)
        }

        parseReact(text)?.let {
            return it
        }
        parseKeyValueTool(text)?.let {
    return ParsedStep(
        thought = null,
        action = it,
        finalAnswer = null
    )
}

        return ParsedStep(
            thought = null,
            action = null,
            finalAnswer = text
        )
    }

    private fun parseReact(raw: String): ParsedStep? {

        val thought =
            thoughtRegex.find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        val final =
            finalRegex.find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        if (final != null) {
            return ParsedStep(
                thought,
                null,
                final
            )
        }

        val action =
            actionRegex.find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        if (action == null)
            return null

        val input =
            actionInputRegex.find(raw)
                ?.groupValues
                ?.getOrNull(1)

        val args =
            input?.let {
                parseJsonObject(it)
            } ?: emptyMap()

        return ParsedStep(
            thought,
            ParsedAction(action, args),
            null
        )
    }
    private fun parseFunctionCall(text: String): ParsedAction? {

        val match = functionRegex.find(text) ?: return null

        val tool = match.groupValues[1].trim()

        val rawArgs = match.groupValues[2].trim()

        val args = mutableMapOf<String, String>()

        if (rawArgs.isNotBlank()) {

            rawArgs.split(",")

                .forEach {

                    val pair = it.split("=", limit = 2)

                    if (pair.size == 2) {

                        args[pair[0].trim()] =
                            pair[1]
                                .trim()
                                .trim('"')
                    }

                }

        }

        return ParsedAction(tool, args)
    }

    private fun parseJsonTool(text: String): ParsedAction? {

        return try {

            val obj = JSONObject(text)

            val tool =
                obj.optString("tool")

            if (tool.isBlank())
                return null

            val argsObject =
                obj.optJSONObject("args")

            val args =
                mutableMapOf<String, String>()

            if (argsObject != null) {

                argsObject.keys().forEach {

                    args[it] =
                        argsObject.opt(it).toString()

                }

            }

            ParsedAction(tool, args)

        } catch (_: Exception) {

            null

        }
    }

    private fun parseJsonObject(json: String): Map<String, String> {

        return try {

            val obj = JSONObject(json)

            val result = mutableMapOf<String, String>()

            obj.keys().forEach {

                result[it] =
                    obj.opt(it).toString()

            }

            result

        } catch (_: Exception) {

            emptyMap()

        }
    }
    /**
     * Поддержка формата:
     *
     * Tool: write_file
     * filename=test.kt
     * content=...
     */
    private fun parseKeyValueTool(text: String): ParsedAction? {

        val lines = text.lines()

        if (lines.isEmpty()) return null

        if (!lines.first().startsWith("Tool:"))
            return null

        val tool =
            lines.first()
                .removePrefix("Tool:")
                .trim()

        val args = mutableMapOf<String, String>()

        lines.drop(1).forEach { line ->

            val index = line.indexOf('=')

            if (index > 0) {

                val key = line.substring(0, index).trim()

                val value = line.substring(index + 1).trim()

                args[key] = value
            }
        }

        return ParsedAction(tool, args)
    }
}

