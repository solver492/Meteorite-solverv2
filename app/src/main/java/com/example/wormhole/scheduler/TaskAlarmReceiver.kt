package com.example.wormhole.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskAlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId != -1L) {
            val workData = Data.Builder()
                .putLong(TaskAlarmScheduler.EXTRA_TASK_ID, taskId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TaskExecutionWorker>()
                .setInputData(workData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "task_alarm_exec_$taskId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
