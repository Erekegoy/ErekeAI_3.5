package com.erekeai.tools

import com.erekeai.core.FileRepository
import com.erekeai.core.Permission
import com.erekeai.core.Tool
import com.erekeai.core.ToolResult

/**
 * Читает файл проекта. READ — выполняется без Approval.
 * args: { "path": "app/src/main/.../File.kt" }
 */
class ReadFileTool(
    private val repo: FileRepository
) : Tool {
    override val id = "file.read"
    override val permission = Permission.READ

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult.Failure("Missing 'path' argument")
        return repo.read(path).fold(
            onSuccess = { ToolResult.Success(it) },
            onFailure = { ToolResult.Failure("Failed to read $path: ${it.message}", it) }
        )
    }
}

/**
 * Перезаписывает файл проекта. WRITE — требует Approval перед вызовом execute().
 * ВАЖНО: Approval должен быть получен ДО вызова этого метода — сам Tool
 * ничего не знает про Approval, это ответственность Executor.
 * args: { "path": "...", "content": "..." }
 */
class WriteFileTool(
    private val repo: FileRepository
) : Tool {
    override val id = "file.write"
    override val permission = Permission.WRITE

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult.Failure("Missing 'path' argument")
        val content = args["content"] ?: return ToolResult.Failure("Missing 'content' argument")
        return repo.write(path, content).fold(
            onSuccess = { ToolResult.Success("Written $path") },
            onFailure = { ToolResult.Failure("Failed to write $path: ${it.message}", it) }
        )
    }
}
