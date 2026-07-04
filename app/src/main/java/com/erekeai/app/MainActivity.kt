package com.erekeai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import com.erekeai.approval.ApprovalHost
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erekeai.features.projectexplorer.ui.ProjectExplorerScreen
import com.erekeai.features.codeeditor.ui.CodeEditorScreen
import com.erekeai.features.chat.ui.ChatScreen
import com.erekeai.features.devagent.ui.DevAgentScreen

import com.erekeai.features.knowledge.ui.KnowledgeBaseScreen
import com.erekeai.features.settings.ui.SettingsScreen
import com.erekeai.ui.theme.ErekeAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ErekeAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
    val navController = rememberNavController()
    var openedFile by remember { mutableStateOf<java.io.File?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        NavHost(
            navController = navController,
            startDestination = "chat"
        ) {

            composable("chat") {
                ChatScreen(
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenKnowledgeBase = {
                        navController.navigate("knowledge_base")
                    },
                    onOpenDevAgent = {
                        navController.navigate("dev_agent")
                    },
                    onOpenProjectExplorer = {
                        navController.navigate("project_explorer")
                    }
                )
            }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("knowledge_base") {
                            KnowledgeBaseScreen(onBack = { navController.popBackStack() })
                        }
                        composable("dev_agent") {
                            DevAgentScreen(onBack = { navController.popBackStack() })
                        }
                      composable("project_explorer") {
    ProjectExplorerScreen(
        root = java.io.File("/storage/emulated/0"),
        onBack = {
            navController.popBackStack()
        },
        onOpenFile = { file ->
            openedFile = file
            navController.navigate("code_editor")
        }
    )
}
composable("code_editor") {
    val file = openedFile

    CodeEditorScreen(
        fileName = file?.name ?: "Новый файл",
        content = try {
    file?.readText() ?: ""
} catch (e: Exception) {
    ""
},
        onBack = {
            navController.popBackStack()
        },
        onSave = { newText ->
            file?.writeText(newText)
        }
    )
  } 
}

ApprovalHost()        
 
 }
 }
 }
 }
 }
 
