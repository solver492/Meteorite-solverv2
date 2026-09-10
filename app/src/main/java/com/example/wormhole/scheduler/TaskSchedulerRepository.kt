package com.example.wormhole.scheduler

import android.content.Context
import com.example.wormhole.data.BrowserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskSchedulerRepository(
    private val context: Context,
    private val database: BrowserDatabase = BrowserDatabase(context)
) : ScheduledTaskDao {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _tasksFlow = MutableStateFlow<List<ScheduledTaskEntity>>(emptyList())
    private val _allLogsFlow = MutableStateFlow<List<TaskExecutionLogEntity>>(emptyList())

    init {
        refreshTasks()
        refreshLogs()
    }

    private fun refreshTasks() {
        scope.launch {
            val list = database.getAllScheduledTasks()
            _tasksFlow.value = list
        }
    }

    private fun refreshLogs() {
        scope.launch {
            val logs = database.getAllExecutionLogs()
            _allLogsFlow.value = logs
        }
    }

    override fun getAllTasksFlow(): Flow<List<ScheduledTaskEntity>> = _tasksFlow.asStateFlow()

    override suspend fun getActiveTasks(): List<ScheduledTaskEntity> = withContext(Dispatchers.IO) {
        database.getActiveScheduledTasks()
    }

    override suspend fun getTaskById(id: Long): ScheduledTaskEntity? = withContext(Dispatchers.IO) {
        database.getScheduledTaskById(id)
    }

    override suspend fun getActiveTasksCount(): Int = withContext(Dispatchers.IO) {
        database.getActiveScheduledTasks().size
    }

    override suspend fun insertTask(task: ScheduledTaskEntity): Long = withContext(Dispatchers.IO) {
        val id = database.insertOrUpdateScheduledTask(task)
        refreshTasks()
        id
    }

    override suspend fun updateTask(task: ScheduledTaskEntity) = withContext(Dispatchers.IO) {
        database.insertOrUpdateScheduledTask(task)
        refreshTasks()
    }

    override suspend fun deleteTask(task: ScheduledTaskEntity) = withContext(Dispatchers.IO) {
        database.deleteScheduledTask(task.id)
        refreshTasks()
        refreshLogs()
    }

    override suspend fun deleteTaskById(id: Long) = withContext(Dispatchers.IO) {
        database.deleteScheduledTask(id)
        refreshTasks()
        refreshLogs()
    }

    override fun getLogsForTaskFlow(taskId: Long): Flow<List<TaskExecutionLogEntity>> {
        val flow = MutableStateFlow<List<TaskExecutionLogEntity>>(emptyList())
        scope.launch {
            val logs = database.getExecutionLogsForTask(taskId)
            flow.value = logs
        }
        return flow
    }

    override suspend fun getLogsForTask(taskId: Long): List<TaskExecutionLogEntity> = withContext(Dispatchers.IO) {
        database.getExecutionLogsForTask(taskId)
    }

    override fun getAllLogsFlow(): Flow<List<TaskExecutionLogEntity>> = _allLogsFlow.asStateFlow()

    override suspend fun insertLog(log: TaskExecutionLogEntity): Long = withContext(Dispatchers.IO) {
        val id = database.insertExecutionLog(log)
        refreshLogs()
        id
    }

    override suspend fun deleteLogsForTask(taskId: Long) = withContext(Dispatchers.IO) {
        database.writableDatabase.delete("task_execution_logs", "task_id = ?", arrayOf(taskId.toString()))
        refreshLogs()
    }

    suspend fun updateTaskStatus(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        database.updateTaskStatus(id, isActive)
        refreshTasks()
    }

    suspend fun updateExecutionResult(
        id: Long,
        lastRunTime: Long,
        lastRunStatus: String,
        nextExecutionTime: Long,
        retryCount: Int = 0,
        isActive: Boolean = true
    ) = withContext(Dispatchers.IO) {
        database.updateTaskExecutionResult(id, lastRunTime, lastRunStatus, nextExecutionTime, retryCount, isActive)
        refreshTasks()
        refreshLogs()
    }
}
