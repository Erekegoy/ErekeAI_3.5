package com.erekeai.data.developer

data class DeveloperTask(

    val title: String,

    val description: String,

    val project: String? = null,

    val autoCommit: Boolean = false,

    val autoPush: Boolean = false,

    val autoBuild: Boolean = true,

    val autoFix: Boolean = true,

    val maxRetries: Int = 5

)
