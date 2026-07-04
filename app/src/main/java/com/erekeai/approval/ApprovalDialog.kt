package com.erekeai.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.core.Permission
import com.erekeai.diff.DiffLine
import com.erekeai.diff.LineType

/**
 * Вставить один раз в корневой Composable приложения (например, в MainActivity/App()),
 * чтобы диалог approval появлялся поверх любого экрана, когда Executor
 * запрашивает подтверждение.
 */
@Composable
fun ApprovalHost(
    viewModel: ApprovalViewModel = hiltViewModel()
) {
    val request by viewModel.currentRequest.collectAsState()

    request?.let { req ->
        ApprovalDialog(
            request = req,
            onApprove = { viewModel.respond(req.id, ApprovalDecision.APPROVED) },
            onReject = { viewModel.respond(req.id, ApprovalDecision.REJECTED) }
        )
    }
}

@Composable
fun ApprovalDialog(
    request: ApprovalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDestructive = request.permission == Permission.DESTRUCTIVE

    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                text = if (isDestructive) "⚠️ Опасное действие" else "AI хочет изменить файл",
                color = if (isDestructive) Color(0xFFD32F2F) else Color.Unspecified
            )
        },
        text = {
            Column {
                Text(request.title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                if (request.diff.isNotEmpty()) {
                    DiffView(request.diff)
                } else if (request.plainDescription != null) {
                    Text(request.plainDescription, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (isDestructive) "Всё равно выполнить" else "Одобрить")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text("Отклонить") }
        }
    )
}

@Composable
private fun DiffView(lines: List<DiffLine>) {
    LazyColumn(
        modifier = Modifier
            .heightIn(max = 320.dp)
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        items(lines) { line ->
            val (bg, prefix) = when (line.type) {
                LineType.ADDED -> Color(0x3300FF00) to "+ "
                LineType.REMOVED -> Color(0x33FF0000) to "- "
                LineType.UNCHANGED -> Color.Transparent to "  "
            }
            Text(
                text = prefix + line.text,
                fontFamily = FontFamily.Monospace,
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                color = Color(0xFFDDDDDD),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
