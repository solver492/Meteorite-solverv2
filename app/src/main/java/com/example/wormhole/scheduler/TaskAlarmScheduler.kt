package com.example.wormhole.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TaskAlarmScheduler {

    const val MAX_ACTIVE_TASKS = 20
    const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife")
    fun createBatteryOptimizationIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun createExactAlarmSettingIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun scheduleTask(context: Context, task: ScheduledTaskEntity): Boolean {
        if (!task.isActive) {
            cancelTask(context, task.id)
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delayMillis = (task.nextExecutionTime - System.currentTimeMillis()).coerceAtLeast(1000L)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "com.example.wormhole.scheduler.EXECUTE_TASK"
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Exact Alarm via AlarmManager (setExactAndAllowWhileIdle)
        val hasExactPermission = canScheduleExactAlarms(context)
        if (hasExactPermission) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.nextExecutionTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        task.nextExecutionTime,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                scheduleWorkManagerFallback(context, task.id, delayMillis)
                return false
            }
        } else {
            // Fallback cleanly to WorkManager if exact alarm permission not granted
            scheduleWorkManagerFallback(context, task.id, delayMillis)
            return false
        }

        // Also register with WorkManager as a backup safeguard
        scheduleWorkManagerFallback(context, task.id, delayMillis)
        return true
    }

    fun scheduleWorkManagerFallback(context: Context, taskId: Long, initialDelayMillis: Long) {
        val workData = Data.Builder()
            .putLong(EXTRA_TASK_ID, taskId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskExecutionWorker>()
            .setInputData(workData)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_work_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "com.example.wormhole.scheduler.EXECUTE_TASK"
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        WorkManager.getInstance(context).cancelUniqueWork("task_work_$taskId")
    }

    fun rescheduleAllActiveTasks(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = TaskSchedulerRepository(context)
            val activeTasks = repository.getActiveTasks()
            val now = System.currentTimeMillis()

            activeTasks.forEach { task ->
                var nextRun = task.nextExecutionTime
                if (nextRun <= now) {
                    nextRun = CronEngine.calculateNextExecution(
                        recurrenceType = task.recurrenceType,
                        hour = task.hour,
                        minute = task.minute,
                        daysOfWeek = task.daysOfWeek,
                        cronExpression = task.cronExpression,
                        fromMillis = now
                    )
                    repository.updateTask(task.copy(nextExecutionTime = nextRun))
                }
                scheduleTask(context, task.copy(nextExecutionTime = nextRun))
            }
        }
    }
}
