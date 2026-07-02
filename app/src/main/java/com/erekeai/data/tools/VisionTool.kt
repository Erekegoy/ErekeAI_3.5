package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolResult
import com.erekeai.vision.CameraCaptureManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TakePhotoTool @Inject constructor(
    private val cameraCaptureManager: CameraCaptureManager
) : Tool {
    override val definition = ToolDefinition(
        name = "take_photo",
        description = "Делает снимок с камеры устройства для визуального анализа (Vision Agent). Требует открытого экрана камеры.",
        parameters = emptyList()
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = try {
        val uri = cameraCaptureManager.capturePhoto()
        ToolResult(true, "Фото сделано: $uri. Прикрепите как imageUri к следующему сообщению для анализа мультимодальной моделью.")
    } catch (e: Exception) {
        ToolResult(false, "Ошибка камеры: ${e.message}")
    }
}
