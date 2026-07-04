package com.erekeai.core.di

import com.erekeai.data.agent.AgentOrchestratorImpl
import com.erekeai.data.tools.CalculatorTool
import com.erekeai.data.tools.BrowserAutomationTool
import com.erekeai.data.tools.CloudSyncTool
import com.erekeai.data.tools.CreateWorkflowTool
import com.erekeai.data.tools.DateTimeTool
import com.erekeai.data.tools.GgufManagerTool
import com.erekeai.data.tools.ImportDocumentTool
import com.erekeai.data.tools.KnowledgeBaseSearchTool
import com.erekeai.data.tools.ListFilesTool
import com.erekeai.data.tools.ListKnowledgeBaseSourcesTool
import com.erekeai.data.tools.ListWorkflowsTool
import com.erekeai.data.tools.OllamaManagerTool
import com.erekeai.data.tools.ReadFileTool
import com.erekeai.data.tools.RunWorkflowTool
import com.erekeai.data.tools.TakePhotoTool
import com.erekeai.data.tools.WebSearchTool
import com.erekeai.data.tools.dev.AnalyzeLogTool
import com.erekeai.data.tools.dev.AnalyzeProjectTool
import com.erekeai.data.tools.dev.BuildApkAgentTool
import com.erekeai.data.tools.dev.CodeSearchTool
import com.erekeai.data.tools.dev.DevPlanTool
import com.erekeai.data.tools.dev.GitHubTool
import com.erekeai.data.tools.dev.GitLabTool
import com.erekeai.data.tools.dev.GitOpsTool
import com.erekeai.data.tools.dev.StaticCheckTool
import com.erekeai.data.tools.dev.TerminalAgentTool
import com.erekeai.data.tools.dev.WriteFileTool
import com.erekeai.data.tools.mcp.McpCallToolTool
import com.erekeai.data.tools.mcp.McpListServersTool
import com.erekeai.data.tools.mcp.McpListToolsTool
import com.erekeai.data.tools.ArchiveProjectTool
import com.erekeai.data.tools.CancelScheduledTaskTool
import com.erekeai.data.tools.CheckPluginUpdatesTool
import com.erekeai.data.tools.CreateProjectTool
import com.erekeai.data.tools.InstallPluginTool
import com.erekeai.data.tools.ListAvailablePluginsTool
import com.erekeai.data.tools.ListInstalledPluginsTool
import com.erekeai.data.tools.ListProjectsTool
import com.erekeai.data.tools.ListScheduledTasksTool
import com.erekeai.data.tools.ScheduleTaskTool
import com.erekeai.data.tools.SetActiveProjectTool
import com.erekeai.data.tools.UninstallPluginTool
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import java.io.File

@Module
@InstallIn(SingletonComponent::class)
object ToolModule {

    /**
     * Список активных инструментов агента. Чтобы добавить новый инструмент —
     * реализуйте [Tool] в data/tools и добавьте его сюда, больше ничего менять не нужно.
     */
    @Provides
    @Singleton
    fun provideToolRegistry(
        webSearchTool: WebSearchTool,
        listFilesTool: ListFilesTool,
        readFileTool: ReadFileTool,
        calculatorTool: CalculatorTool,
        dateTimeTool: DateTimeTool,
        importDocumentTool: ImportDocumentTool,
        knowledgeBaseSearchTool: KnowledgeBaseSearchTool,
        listKnowledgeBaseSourcesTool: ListKnowledgeBaseSourcesTool,
        // 🤖 AI Developer Agent: анализ проекта, генерация/правка кода, проверки, git/github, планирование
        writeFileTool: WriteFileTool,
        analyzeProjectTool: AnalyzeProjectTool,
        codeSearchTool: CodeSearchTool,
        staticCheckTool: StaticCheckTool,
        analyzeLogTool: AnalyzeLogTool,
        gitHubTool: GitHubTool,
        devPlanTool: DevPlanTool,
        // Новое в этом раунде:
        gitOpsTool: GitOpsTool,
        terminalAgentTool: TerminalAgentTool,
        buildApkAgentTool: BuildApkAgentTool,
        createWorkflowTool: CreateWorkflowTool,
        runWorkflowTool: RunWorkflowTool,
        listWorkflowsTool: ListWorkflowsTool,
        mcpListServersTool: McpListServersTool,
        mcpListToolsTool: McpListToolsTool,
        mcpCallToolTool: McpCallToolTool,
        ollamaManagerTool: OllamaManagerTool,
        ggufManagerTool: GgufManagerTool,
        browserAutomationTool: BrowserAutomationTool,
        cloudSyncTool: CloudSyncTool,
        takePhotoTool: TakePhotoTool,
        // Раунд 4: Project Manager, Scheduler, Plugins, GitLab
        createProjectTool: CreateProjectTool,
        listProjectsTool: ListProjectsTool,
        setActiveProjectTool: SetActiveProjectTool,
        archiveProjectTool: ArchiveProjectTool,
        scheduleTaskTool: ScheduleTaskTool,
        listScheduledTasksTool: ListScheduledTasksTool,
        cancelScheduledTaskTool: CancelScheduledTaskTool,
        // listAvailablePluginsTool: ListAvailablePluginsTool,
        // installPluginTool: InstallPluginTool,
        // listInstalledPluginsTool: ListInstalledPluginsTool,
        // uninstallPluginTool: UninstallPluginTool,
        // checkPluginUpdatesTool: CheckPluginUpdatesTool,
        gitLabTool: GitLabTool
    ): ToolRegistry = ToolRegistry(
        listOf(
            webSearchTool, listFilesTool, readFileTool, calculatorTool, dateTimeTool,
            importDocumentTool, knowledgeBaseSearchTool, listKnowledgeBaseSourcesTool,
            writeFileTool, analyzeProjectTool, codeSearchTool, staticCheckTool,
            analyzeLogTool, gitHubTool, devPlanTool,
            gitOpsTool, terminalAgentTool, buildApkAgentTool,
            createWorkflowTool, runWorkflowTool, listWorkflowsTool,
            mcpListServersTool, mcpListToolsTool, mcpCallToolTool,
            ollamaManagerTool, ggufManagerTool,
            browserAutomationTool, cloudSyncTool, takePhotoTool,
            createProjectTool, listProjectsTool, setActiveProjectTool, archiveProjectTool,
            scheduleTaskTool,
            listScheduledTasksTool,
            cancelScheduledTaskTool,
            // listAvailablePluginsTool,
            // installPluginTool,
            // listInstalledPluginsTool,
            // uninstallPluginTool,
            // checkPluginUpdatesTool,
            gitLabTool
        )
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {
    @Binds
    @Singleton
    abstract fun bindAgentOrchestrator(impl: AgentOrchestratorImpl): AgentOrchestrator
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkflowModule {
    @Binds
    @Singleton
    abstract fun bindWorkflowEngine(impl: com.erekeai.data.workflow.WorkflowEngineImpl): com.erekeai.domain.workflow.WorkflowEngine
}

@Module
@InstallIn(SingletonComponent::class)
abstract class McpModule {
    @Binds
    @Singleton
    abstract fun bindMcpServerStore(impl: com.erekeai.data.mcp.McpServerStoreImpl): com.erekeai.domain.mcp.McpServerStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlannerModule {
    @Binds
    @Singleton
    abstract fun bindMultiAgentPlanner(impl: com.erekeai.data.planner.MultiAgentPlannerImpl): com.erekeai.domain.planner.MultiAgentPlanner
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExecutorPlannerModule {

    @Binds
    @Singleton
    abstract fun bindPlanner(
        impl: com.erekeai.planner.PlannerAdapter
    ): com.erekeai.planner.Planner
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectModule {
    @Binds
    @Singleton
    abstract fun bindProjectManager(impl: com.erekeai.data.project.ProjectManagerImpl): com.erekeai.domain.project.ProjectManager
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RouterModule {
    @Binds
    @Singleton
    abstract fun bindAiRouter(impl: com.erekeai.data.router.AiRouterImpl): com.erekeai.domain.router.AiRouter
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {
    @Binds
    @Singleton
    abstract fun bindTaskScheduler(impl: com.erekeai.data.scheduler.TaskSchedulerImpl): com.erekeai.domain.scheduler.TaskScheduler
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PluginModule {
    @Binds
    @Singleton
    abstract fun bindPluginRepository(impl: com.erekeai.data.plugin.PluginRepositoryImpl): com.erekeai.domain.plugin.PluginRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ExecutorModule {

    @Provides
    @Singleton
    fun provideFileRepository(): com.erekeai.core.FileRepository =
        com.erekeai.core.LocalFileRepository(
            java.io.File("/storage/emulated/0")
        )

    @Provides
    @Singleton
    fun provideDiffService(): com.erekeai.diff.DiffService =
        com.erekeai.diff.JavaDiffUtilsService()

    @Provides
    @Singleton
    fun provideSimpleFixExecutor(
        fileRepository: com.erekeai.core.FileRepository,
        planner: com.erekeai.planner.Planner,
        diffService: com.erekeai.diff.DiffService,
        approvalService: com.erekeai.approval.ApprovalService
    ): com.erekeai.executor.SimpleFixExecutor =
        com.erekeai.executor.SimpleFixExecutor(
            fileRepository,
            planner,
            diffService,
            approvalService
        )
}

@Module
@InstallIn(SingletonComponent::class)
object Milestone2Module {

    @Provides
    @Singleton
    fun provideBackupManager(): com.erekeai.backup.BackupManager =
        com.erekeai.backup.FileBackupManager(
            File(".erekeai-backups")
        )

    @Provides
    @Singleton
    fun provideBuildRunner(): com.erekeai.build.BuildRunner =
        com.erekeai.build.LocalGradleBuildRunner(
            File(".")
        )

    @Provides
@Singleton
fun provideChangeNotifier(): com.erekeai.notifier.ChangeNotifier {
    return com.erekeai.notifier.ChangeNotifier()
}
}
