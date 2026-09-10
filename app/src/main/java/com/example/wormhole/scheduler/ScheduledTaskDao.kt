package com.example.wormhole.scheduler

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTaskDao {

    @Query("SELECT * FROM scheduled_tasks ORDER BY nextExecutionTime ASC")
    fun getAllTasksFlow(): Flow<List<ScheduledTaskEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE isActive = 1 ORDER BY nextExecutionTime ASC")
    suspend fun getActiveTasks(): List<ScheduledTaskEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): ScheduledTaskEntity?

    @Query("SELECT COUNT(*) FROM scheduled_tasks WHERE isActive = 1")
    suspend fun getActiveTasksCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTaskEntity): Long

    @Update
    suspend fun updateTask(task: ScheduledTaskEntity)

    @Delete
    suspend fun deleteTask(task: ScheduledTaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT * FROM task_execution_logs WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getLogsForTaskFlow(taskId: Long): Flow<List<TaskExecutionLogEntity>>

    @Query("SELECT * FROM task_execution_logs WHERE taskId = :taskId ORDER BY timestamp DESC")
    suspend fun getLogsForTask(taskId: Long): List<TaskExecutionLogEntity>

    @Query("SELECT * FROM task_execution_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogsFlow(): Flow<List<TaskExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TaskExecutionLogEntity): Long

    @Query("DELETE FROM task_execution_logs WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: Long)
}
