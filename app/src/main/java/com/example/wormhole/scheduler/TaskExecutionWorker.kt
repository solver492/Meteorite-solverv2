package com.example.wormhole.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wormhole.engine.LlmInferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskExecutionWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getLong(TaskAlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return@withContext Result.failure()

        val repository = TaskSchedulerRepository(appContext)
        val task = repository.getTaskById(taskId) ?: return@withContext Result.failure()

        if (!task.isActive) {
            TaskAlarmScheduler.cancelTask(appContext, taskId)
            return@withContext Result.success()
        }

        val engine = LlmInferenceEngine(appContext)
        val now = System.currentTimeMillis()

        try {
            // Execute on-device command via LlmInferenceEngine
            val (resultText, actions) = engine.executeTaskCommand(
                command = task.command,
                pageTitle = "Page en cours d'analyse",
                pageUrl = "https://duckduckgo.com",
                pageContent = "Contenu local synchronisé pour la tâche programmée."
            )

            val log = TaskExecutionLogEntity(
                taskId = task.id,
                timestamp = now,
                result = resultText,
                isSuccess = true,
                errorMessage = null
            )
            repository.insertLog(log)

            // Show user-facing notification with local AI result
            TaskNotificationHelper.showTaskResultNotification(
                context = appContext,
                taskId = task.id,
                taskTitle = task.title,
                resultSummary = resultText,
                isSuccess = true
            )

            // Handle recurrence and reschedule
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

            repository.updateExecutionResult(
                id = task.id,
                lastRunTime = now,
                lastRunStatus = "SUCCESS",
                nextExecutionTime = nextRun,
                retryCount = 0,
                isActive = !isOnce
            )

            if (!isOnce) {
                TaskAlarmScheduler.scheduleTask(appContext, task.copy(nextExecutionTime = nextRun))
            }

            Result.success()
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown execution error"
            val log = TaskExecutionLogEntity(
                taskId = task.id,
                timestamp = now,
                result = "Échec lors de l'inférence locale : $errorMsg",
                isSuccess = false,
                errorMessage = errorMsg
            )
            repository.insertLog(log)

            // Simple retry policy: 1 retry after 5 minutes if retryCount < 1
            if (task.retryCount < 1) {
                val retryTime = now + (5 * 60 * 1000)
                repository.updateExecutionResult(
                    id = task.id,
                    lastRunTime = now,
                    lastRunStatus = "FAILED",
                    nextExecutionTime = retryTime,
                    retryCount = task.retryCount + 1,
                    isActive = true
                )
                TaskAlarmScheduler.scheduleTask(
                    appContext,
                    task.copy(nextExecutionTime = retryTime, retryCount = task.retryCount + 1)
                )
                Result.retry()
            } else {
                // If retry already attempted, reschedule next regular cycle without crashing
                val nextRegularRun = CronEngine.calculateNextExecution(
                    recurrenceType = task.recurrenceType,
                    hour = task.hour,
                    minute = task.minute,
                    daysOfWeek = task.daysOfWeek,
                    cronExpression = task.cronExpression,
                    fromMillis = now + (10 * 60 * 1000)
                )
                repository.updateExecutionResult(
                    id = task.id,
                    lastRunTime = now,
                    lastRunStatus = "FAILED",
                    nextExecutionTime = nextRegularRun,
                    retryCount = 0,
                    isActive = !task.recurrenceType.equals("ONCE", ignoreCase = true)
                )
                Result.failure()
            }
        } finally {
            engine.close()
        }
    }
}
