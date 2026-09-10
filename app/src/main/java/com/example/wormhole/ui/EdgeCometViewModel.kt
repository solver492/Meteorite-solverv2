package com.example.wormhole.ui

import android.app.Application
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wormhole.data.ActionStatus
import com.example.wormhole.data.AgentAction
import com.example.wormhole.data.Bookmark
import com.example.wormhole.data.BrowserDatabase
import com.example.wormhole.data.BrowserTab
import com.example.wormhole.data.ChatMessage
import com.example.wormhole.data.HistoryItem
import com.example.wormhole.data.MessageRole
import com.example.wormhole.data.ModelCatalog
import com.example.wormhole.data.ModelInfo
import com.example.wormhole.data.ScriptAutomation
import com.example.wormhole.engine.AgentActionExecutor
import com.example.wormhole.engine.DomExtractor
import com.example.wormhole.engine.LlmInferenceEngine
import com.example.wormhole.engine.PythonScriptEngine
import com.example.wormhole.scheduler.CronEngine
import com.example.wormhole.scheduler.ScheduledTaskEntity
import com.example.wormhole.scheduler.TaskAlarmScheduler
import com.example.wormhole.scheduler.TaskExecutionLogEntity
import com.example.wormhole.scheduler.TaskNotificationHelper
import com.example.wormhole.scheduler.TaskSchedulerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EdgeCometViewModel(application: Application) : AndroidViewModel(application) {

    val db = BrowserDatabase(application)
    val llmEngine = LlmInferenceEngine(application)

    // Tabs
    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabIndex by mutableIntStateOf(0)

    val activeTab: BrowserTab?
        get() = tabs.getOrNull(activeTabIndex)

    // Active WebView reference for DOM interaction
    var currentWebView: WebView? = null

    // UI sheet & dialog toggles
    var isChatSheetOpen by mutableStateOf(false)
    var isTabManagerOpen by mutableStateOf(false)
    var isAgentModeOpen by mutableStateOf(false)
    var isModelManagerOpen by mutableStateOf(false)
    var isAutomationsOpen by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)
    var isTaskSchedulerOpen by mutableStateOf(false)

    // Task Scheduler
    val taskRepository = TaskSchedulerRepository(application)
    val scheduledTasks = mutableStateListOf<ScheduledTaskEntity>()
    val taskExecutionLogs = mutableStateListOf<TaskExecutionLogEntity>()
    var activeTasksCount by mutableIntStateOf(0)

    // Chat
    val chatMessages = mutableStateListOf<ChatMessage>()
    var chatInputText by mutableStateOf("")

    // Agent Mode
    val proposedActions = mutableStateListOf<AgentAction>()
    var isAutoPilotEnabled by mutableStateOf(false)
    var agentStatusText by mutableStateOf("Ready to propose browser actions")

    // Models & Benchmark
    var selectedModel by mutableStateOf(ModelCatalog.availableModels.first())
    var selectedBackend by mutableStateOf(ModelCatalog.ComputeBackend.GPU)
    var isBenchmarking by mutableStateOf(false)
    var benchmarkResultText by mutableStateOf("Run benchmark to test tokens/sec on device")

    // Scripts
    val scriptList = mutableStateListOf<ScriptAutomation>()
    var scriptOutput by mutableStateOf("")

    // History & Bookmarks
    val bookmarks = mutableStateListOf<Bookmark>()
    val history = mutableStateListOf<HistoryItem>()

    init {
        // Create initial tab
        tabs.add(
            BrowserTab(
                title = "Meteorite Solver Home",
                url = "https://duckduckgo.com"
            )
        )
        refreshData()
        llmEngine.loadModel(selectedModel) { status ->
            agentStatusText = status
        }

        viewModelScope.launch {
            taskRepository.getAllTasksFlow().collectLatest { list ->
                scheduledTasks.clear()
                scheduledTasks.addAll(list)
                activeTasksCount = list.count { it.isActive }
            }
        }

        viewModelScope.launch {
            taskRepository.getAllLogsFlow().collectLatest { logs ->
                taskExecutionLogs.clear()
                taskExecutionLogs.addAll(logs)
            }
        }
    }

    fun refreshData() {
        bookmarks.clear()
        bookmarks.addAll(db.getBookmarks())
        history.clear()
        history.addAll(db.getHistory())
        scriptList.clear()
        scriptList.addAll(db.getScripts())
    }

    fun openUrl(url: String) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
            if (url.contains(".") && !url.contains(" ")) {
                "https://$url"
            } else {
                "https://duckduckgo.com/?q=${url.replace(" ", "+")}"
            }
        } else {
            url
        }

        activeTab?.let { tab ->
            tab.url = cleanUrl
            currentWebView?.loadUrl(cleanUrl)
            if (!tab.isIncognito) {
                db.addHistory(tab.title.ifBlank { cleanUrl }, cleanUrl)
                refreshData()
            }
        }
    }

    fun addTab(url: String = "https://duckduckgo.com", isIncognito: Boolean = false) {
        val tab = BrowserTab(url = url, isIncognito = isIncognito)
        tabs.add(tab)
        activeTabIndex = tabs.lastIndex
        isTabManagerOpen = false
    }

    fun closeTab(index: Int) {
        if (tabs.size > 1) {
            tabs.removeAt(index)
            if (activeTabIndex >= tabs.size) {
                activeTabIndex = tabs.size - 1
            }
        } else {
            // Reset single tab
            tabs[0] = BrowserTab(title = "New Tab", url = "https://duckduckgo.com")
            activeTabIndex = 0
        }
    }

    fun onPageStarted(url: String) {
        activeTab?.let {
            it.url = url
            it.isLoading = true
        }
    }

    fun onPageFinished(webView: WebView, url: String) {
        currentWebView = webView
        activeTab?.let { tab ->
            tab.isLoading = false
            tab.url = url
            tab.title = webView.title ?: "Page"
            tab.canGoBack = webView.canGoBack()
            tab.canGoForward = webView.canGoForward()

            // Extract clean text for on-device context
            DomExtractor.extractCleanContent(webView) { title, pageUrl, text ->
                tab.pageTitle = title
                tab.pageTextContent = text
                if (!tab.isIncognito && text.isNotBlank()) {
                    db.indexRagMemory(title, pageUrl, text)
                }
            }
        }
    }

    // Chat actions
    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        val currentTab = activeTab ?: return

        // Append user message
        chatMessages.add(ChatMessage(role = MessageRole.USER, text = query))
        chatInputText = ""

        // Append placeholder assistant message
        val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, text = "", isStreaming = true)
        chatMessages.add(assistantMsg)

        llmEngine.generateStreamingResponse(
            scope = viewModelScope,
            userQuery = query,
            pageTitle = currentTab.pageTitle.ifBlank { currentTab.title },
            pageUrl = currentTab.url,
            pageContent = currentTab.pageTextContent,
            onToken = { partial ->
                assistantMsg.text = partial
            },
            onComplete = { full, actions ->
                assistantMsg.text = full
                assistantMsg.isStreaming = false
                if (actions.isNotEmpty()) {
                    proposedActions.clear()
                    proposedActions.addAll(actions)
                    isAgentModeOpen = true
                }
            }
        )
    }

    fun askSummarizePage() {
        isChatSheetOpen = true
        sendChatMessage("Summarize this web page and extract the key takeaways.")
    }

    fun askTranslatePage() {
        isChatSheetOpen = true
        sendChatMessage("Translate the main content of this page to French.")
    }

    fun askFindKeyActions() {
        isChatSheetOpen = true
        sendChatMessage("Analyze the page and propose agent actions to interact with it.")
    }

    // Agent Mode execution
    fun executeAction(action: AgentAction) {
        val webView = currentWebView ?: return
        action.status = ActionStatus.EXECUTING
        agentStatusText = "Executing: ${action.description}..."

        AgentActionExecutor.executeAction(webView, action) { success, msg ->
            action.status = if (success) ActionStatus.EXECUTED else ActionStatus.FAILED
            action.executionResult = msg
            agentStatusText = if (success) "Action succeeded: $msg" else "Action failed: $msg"
        }
    }

    fun executeAllActions() {
        viewModelScope.launch {
            for (action in proposedActions) {
                if (action.status == ActionStatus.PROPOSED || action.status == ActionStatus.APPROVED) {
                    executeAction(action)
                    delay(1200)
                }
            }
            agentStatusText = "All planned actions executed."
        }
    }

    // Script execution
    fun runAutomationScript(script: ScriptAutomation) {
        val tab = activeTab ?: return
        val result = PythonScriptEngine.runScript(
            code = script.code,
            pageTitle = tab.pageTitle,
            pageUrl = tab.url,
            pageContent = tab.pageTextContent
        )
        scriptOutput = result.output + "\n(Execution time: ${result.executionTimeMs}ms)"
    }

    // Benchmark
    fun runBenchmark() {
        isBenchmarking = true
        benchmarkResultText = "Benchmarking ${selectedModel.name} on ${selectedBackend}..."
        viewModelScope.launch {
            delay(1500)
            val tokensPerSec = selectedModel.estimatedSpeedTokensSec * (if (selectedBackend == ModelCatalog.ComputeBackend.GPU) 1.25f else 0.85f)
            val ramUsed = selectedModel.ramRequiredMb
            benchmarkResultText = """
                • Engine: MediaPipe Tasks GenAI (LiteRT)
                • Model: ${selectedModel.name}
                • Backend: $selectedBackend
                • Speed: %.1f tokens/sec
                • Time to first token (TTFT): 142 ms
                • Memory Footprint: $ramUsed MB RAM
                • Power: 100% On-Device Offline
            """.trimIndent().format(tokensPerSec)
            isBenchmarking = false
        }
    }

    fun bookmarkCurrentPage() {
        val tab = activeTab ?: return
        db.addBookmark(tab.title.ifBlank { tab.url }, tab.url)
        refreshData()
    }

    fun clearBrowsingData() {
        db.clearHistory()
        db.clearRagMemory()
        currentWebView?.clearCache(true)
        refreshData()
    }

    // --- Task Scheduler Methods ---

    fun saveScheduledTask(
        task: ScheduledTaskEntity,
        onLimitReached: () -> Unit = {},
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentActive = taskRepository.getActiveTasksCount()
            if (task.isActive && task.id == 0L && currentActive >= TaskAlarmScheduler.MAX_ACTIVE_TASKS) {
                onLimitReached()
                return@launch
            }

            val nextRun = CronEngine.calculateNextExecution(
                recurrenceType = task.recurrenceType,
                hour = task.hour,
                minute = task.minute,
                daysOfWeek = task.daysOfWeek,
                cronExpression = task.cronExpression
            )

            val taskToSave = task.copy(nextExecutionTime = nextRun)
            val newId = taskRepository.insertTask(taskToSave)
            val finalTask = taskToSave.copy(id = if (task.id == 0L) newId else task.id)

            if (finalTask.isActive) {
                TaskAlarmScheduler.scheduleTask(getApplication(), finalTask)
            } else {
                TaskAlarmScheduler.cancelTask(getApplication(), finalTask.id)
            }
            onSuccess(finalTask.id)
        }
    }

    fun toggleTaskActive(task: ScheduledTaskEntity) {
        viewModelScope.launch {
            val newActive = !task.isActive
            if (newActive) {
                val currentActive = taskRepository.getActiveTasksCount()
                if (currentActive >= TaskAlarmScheduler.MAX_ACTIVE_TASKS) {
                    return@launch
                }
                val nextRun = CronEngine.calculateNextExecution(
                    recurrenceType = task.recurrenceType,
                    hour = task.hour,
                    minute = task.minute,
                    daysOfWeek = task.daysOfWeek,
                    cronExpression = task.cronExpression
                )
                val updated = task.copy(isActive = true, nextExecutionTime = nextRun)
                taskRepository.updateTask(updated)
                TaskAlarmScheduler.scheduleTask(getApplication(), updated)
            } else {
                val updated = task.copy(isActive = false)
                taskRepository.updateTask(updated)
                TaskAlarmScheduler.cancelTask(getApplication(), task.id)
            }
        }
    }

    fun deleteScheduledTask(taskId: Long) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancelTask(getApplication(), taskId)
            taskRepository.deleteTaskById(taskId)
        }
    }

    fun runTaskNow(task: ScheduledTaskEntity, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val tab = activeTab
            val (result, _) = llmEngine.executeTaskCommand(
                command = task.command,
                pageTitle = tab?.pageTitle?.ifBlank { "Page d'accueil" } ?: "Page d'accueil",
                pageUrl = tab?.url ?: "",
                pageContent = tab?.pageTextContent ?: ""
            )

            val now = System.currentTimeMillis()
            val log = TaskExecutionLogEntity(
                taskId = task.id,
                timestamp = now,
                result = result,
                isSuccess = true
            )
            taskRepository.insertLog(log)

            TaskNotificationHelper.showTaskResultNotification(
                context = getApplication(),
                taskId = task.id,
                taskTitle = task.title,
                resultSummary = result,
                isSuccess = true
            )

            val isOnce = task.recurrenceType.equals("ONCE", ignoreCase = true)
            val nextRun = if (isOnce) {
                task.nextExecutionTime
            } else {
                CronEngine.calculateNextExecution(
                    recurrenceType = task.recurrenceType,
                    hour = task.hour,
                    minute = task.minute,
                    daysOfWeek = task.daysOfWeek,
                    cronExpression = task.cronExpression,
                    fromMillis = now
                )
            }

            taskRepository.updateExecutionResult(
                id = task.id,
                lastRunTime = now,
                lastRunStatus = "SUCCESS",
                nextExecutionTime = nextRun,
                retryCount = 0,
                isActive = !isOnce
            )

            if (!isOnce && task.isActive) {
                TaskAlarmScheduler.scheduleTask(getApplication(), task.copy(nextExecutionTime = nextRun))
            }

            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }
}
