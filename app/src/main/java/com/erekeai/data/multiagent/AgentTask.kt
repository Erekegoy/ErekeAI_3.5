package com.erekeai.data.multiagent

data class AgentTask(
    val title: String,
    val prompt: String,
    val language: String = "kotlin",
    val maxRounds: Int = 5
)
