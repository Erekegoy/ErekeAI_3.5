package com.erekeai.data.multiagent

data class AgentAnswer(

    val provider: AgentProvider,

    val model: String,

    val answer: String,

    val score: Int = 0,

    val success: Boolean = true

)
