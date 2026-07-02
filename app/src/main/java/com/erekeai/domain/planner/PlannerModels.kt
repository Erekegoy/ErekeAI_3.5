package com.erekeai.domain.planner

data class SubTask(val id: Int, val title: String, val instructions: String)

sealed class PlannerEvent {
    data class PlanCreated(val subTasks: List<SubTask>) : PlannerEvent()
    data class SubTaskStarted(val subTask: SubTask) : PlannerEvent()
    data class SubTaskFinished(val subTask: SubTask, val result: String, val success: Boolean) : PlannerEvent()
    data class FinalReport(val text: String) : PlannerEvent()
    data class Error(val message: String) : PlannerEvent()
}
