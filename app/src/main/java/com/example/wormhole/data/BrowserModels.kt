package com.example.wormhole.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    title: String = "New Tab",
    url: String = "about:blank",
    val isIncognito: Boolean = false,
    pageTextContent: String = "",
    pageTitle: String = "",
    isLoading: Boolean = false,
    progress: Int = 0,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false
) {
    var title by mutableStateOf(title)
    var url by mutableStateOf(url)
    var pageTextContent by mutableStateOf(pageTextContent)
    var pageTitle by mutableStateOf(pageTitle)
    var isLoading by mutableStateOf(isLoading)
    var progress by mutableIntStateOf(progress)
    var canGoBack by mutableStateOf(canGoBack)
    var canGoForward by mutableStateOf(canGoForward)
}

data class Bookmark(
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    AGENT_STEP
}

class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    text: String,
    val timestamp: Long = System.currentTimeMillis(),
    isStreaming: Boolean = false,
    val actions: List<AgentAction> = emptyList()
) {
    var text by mutableStateOf(text)
    var isStreaming by mutableStateOf(isStreaming)
}

enum class ActionType {
    CLICK,
    INPUT_TEXT,
    SCROLL,
    SUBMIT,
    EXTRACT,
    NAVIGATE
}

enum class ActionStatus {
    PROPOSED,
    APPROVED,
    EXECUTING,
    EXECUTED,
    FAILED,
    REJECTED
}

class AgentAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val selector: String,
    val value: String = "",
    val description: String,
    status: ActionStatus = ActionStatus.PROPOSED,
    executionResult: String = ""
) {
    var status by mutableStateOf(status)
    var executionResult by mutableStateOf(executionResult)
}

data class ModelInfo(
    val id: String,
    val name: String,
    val family: String,
    val parameterSize: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val ramRequiredMb: Int,
    val downloadUrl: String,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val estimatedSpeedTokensSec: Float = 14.5f,
    val isRecommended: Boolean = false
)

data class ScriptAutomation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val language: String = "Python", // or JavaScript
    val code: String,
    val trigger: String = "Manual"
)

data class RagMemoryItem(
    val id: Long = 0,
    val title: String,
    val url: String,
    val contentSnippet: String,
    val timestamp: Long = System.currentTimeMillis()
)
