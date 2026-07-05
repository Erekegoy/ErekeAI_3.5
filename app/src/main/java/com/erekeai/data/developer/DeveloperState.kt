package com.erekeai.data.developer

data class DeveloperState(
    val mode: DeveloperMode = DeveloperMode.IDLE,
    val currentTask: String = "",
    val currentStep: Int = 0,
    val maxSteps: Int = 20,
    val lastError: String? = null,
    val success: Boolean = false
)
