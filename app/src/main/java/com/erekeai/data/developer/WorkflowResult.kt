package com.erekeai.data.developer

data class WorkflowResult(
    val success: Boolean,
    val retry: Boolean = false,
    val message: String = ""
)
