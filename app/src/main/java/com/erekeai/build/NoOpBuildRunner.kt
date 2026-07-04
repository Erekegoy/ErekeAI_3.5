package com.erekeai.build

class NoOpBuildRunner : BuildRunner {
    override suspend fun run(task: String): BuildResult {
        return BuildResult(
            success = true,
            log = "disabled",
            durationMillis = 0
        )
    }
}
