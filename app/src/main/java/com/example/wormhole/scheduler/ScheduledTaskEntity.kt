package com.example.wormhole.scheduler

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val command: String,
    val recurrenceType: String, // ONCE, DAILY, WEEKLY, MONTHLY, CRON
    val cronExpression: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: String = "", // e.g. "1,2,3,4,5" for Mon-Fri
    val nextExecutionTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunTime: Long? = null,
    val lastRunStatus: String? = null, // SUCCESS, FAILED
    val retryCount: Int = 0
)
