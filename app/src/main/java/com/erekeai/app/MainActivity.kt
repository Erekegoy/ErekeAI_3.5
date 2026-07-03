package com.erekeai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erekeai.features.chat.ui.ChatScreen
import com.erekeai.features.devagent.ui.DevAgentScreen
import com.erekeai.features.fileexplorer.ui.FileExplorerScreen
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
                    NavHost(navController = navController, startDestination = "chat") {
                        composable("chat") {
                            ChatScreen(
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenKnowledgeBase = { navController.navigate("knowledge_base") },
                                onOpenDevAgent = { navController.navigate("dev_agent") }
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
                      composable("file_explorer") {
    FileExplorerScreen(
    onBack = {
        navController.popBackStack()
    },
    onOpenFile = { file ->
        // Пока просто заглушка.
        // На следующем этапе здесь откроем CodeEditor.
        println("Open file: ${file.absolutePath}")
    }
)
}
                    }
                }
            }
        }
    }
}
