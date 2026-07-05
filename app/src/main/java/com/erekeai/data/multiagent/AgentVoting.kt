package com.erekeai.data.multiagent

class AgentVoting {

    fun vote(
        answers: List<AgentAnswer>
    ): AgentAnswer? {

        if (answers.isEmpty()) return null

        return answers.maxByOrNull {
            it.score
        }
    }

}
