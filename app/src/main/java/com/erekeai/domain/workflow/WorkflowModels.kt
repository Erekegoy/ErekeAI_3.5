package com.erekeai.domain.workflow

data class WorkflowStep(val toolName: String, val args: Map<String, String>)

data class Workflow(val id: Long = 0L, val name: String, val steps: List<WorkflowStep>, val createdAt: Long = System.currentTimeMillis())

data class WorkflowStepResult(val toolName: String, val success: Boolean, val content: String)
