package com.example.wormhole.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.wormhole.scheduler.ScheduledTaskEntity
import com.example.wormhole.scheduler.TaskExecutionLogEntity

class BrowserDatabase(context: Context) : SQLiteOpenHelper(context, "meteorite_solver.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE scripts (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                language TEXT NOT NULL,
                code TEXT NOT NULL,
                trigger TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE rag_memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        createSchedulerTables(db)

        // Seed default automation scripts
        insertDefaultScripts(db)
        insertDefaultScheduledTasks(db)
    }

    private fun createSchedulerTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scheduled_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                command TEXT NOT NULL,
                recurrence_type TEXT NOT NULL,
                cron_expression TEXT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                days_of_week TEXT NOT NULL,
                next_execution_time INTEGER NOT NULL,
                is_active INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                last_run_time INTEGER,
                last_run_status TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS task_execution_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                result TEXT NOT NULL,
                is_success INTEGER NOT NULL,
                error_message TEXT,
                FOREIGN KEY (task_id) REFERENCES scheduled_tasks(id) ON DELETE CASCADE
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createSchedulerTables(db)
            insertDefaultScheduledTasks(db)
        }
    }

    private fun insertDefaultScheduledTasks(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("title", "Résumé quotidien de la page d'accueil")
            put("command", "Résume les principales actualités et nouveautés de ma page")
            put("recurrence_type", "DAILY")
            put("cron_expression", "0 8 * * *")
            put("hour", 8)
            put("minute", 0)
            put("days_of_week", "1,2,3,4,5")
            put("next_execution_time", now + 3600 * 1000)
            put("is_active", 1)
            put("created_at", now)
            put("retry_count", 0)
        }
        val taskId = db.insert("scheduled_tasks", null, cv)

        val logCv = ContentValues().apply {
            put("task_id", taskId)
            put("timestamp", now - 3600 * 1000)
            put("result", "### Résumé IA On-Device\nPage analysée avec succès. 4 actualités clés relevées.")
            put("is_success", 1)
        }
        db.insert("task_execution_logs", null, logCv)
    }

    private fun insertDefaultScripts(db: SQLiteDatabase) {
        val defaultScripts = listOf(
            ScriptAutomation(
                id = "script_1",
                title = "Extract All Links & Headlines",
                description = "Extracts all h1/h2 headings and links from current page into structured JSON.",
                language = "Python",
                code = """
# Meteorite Solver On-Device Python Automator
def execute(page_dom, url):
    import json
    # Clean headings and hrefs
    data = {
        "url": url,
        "headings": [h.strip() for h in page_dom.get("headings", []) if h],
        "links_count": len(page_dom.get("links", []))
    }
    return json.dumps(data, indent=2)
                """.trimIndent()
            ),
            ScriptAutomation(
                id = "script_2",
                title = "Form Auto-Filler Helper",
                description = "Identifies input fields and suggests semantic filling rules without network calls.",
                language = "Python",
                code = """
def execute(form_fields):
    suggestions = {}
    for field in form_fields:
        name = field.get("name", "").lower()
        if "email" in name:
            suggestions[field["id"]] = "user@private-edge.local"
        elif "name" in name:
            suggestions[field["id"]] = "Meteorite Solver User"
    return suggestions
                """.trimIndent()
            ),
            ScriptAutomation(
                id = "script_3",
                title = "Reader Mode Text Sanitizer",
                description = "Cleans boilerplate banners, cookies, and ads from page text.",
                language = "Python",
                code = """
def execute(raw_text):
    lines = raw_text.splitlines()
    clean = [l.strip() for l in lines if len(l.strip()) > 30]
    return "\n\n".join(clean)
                """.trimIndent()
            )
        )

        for (s in defaultScripts) {
            val cv = ContentValues().apply {
                put("id", s.id)
                put("title", s.title)
                put("description", s.description)
                put("language", s.language)
                put("code", s.code)
                put("trigger", s.trigger)
            }
            db.insert("scripts", null, cv)
        }
    }

    fun addBookmark(title: String, url: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("bookmarks", null, cv)
    }

    fun getBookmarks(): List<Bookmark> {
        val list = mutableListOf<Bookmark>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, title, url, created_at FROM bookmarks ORDER BY id DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Bookmark(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        createdAt = it.getLong(3)
                    )
                )
            }
        }
        return list
    }

    fun deleteBookmark(id: Long) {
        writableDatabase.delete("bookmarks", "id = ?", arrayOf(id.toString()))
    }

    fun addHistory(title: String, url: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("history", null, cv)
    }

    fun getHistory(): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, title, url, timestamp FROM history ORDER BY timestamp DESC LIMIT 50", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        timestamp = it.getLong(3)
                    )
                )
            }
        }
        return list
    }

    fun clearHistory() {
        writableDatabase.delete("history", null, null)
    }

    fun getScripts(): List<ScriptAutomation> {
        val list = mutableListOf<ScriptAutomation>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, title, description, language, code, trigger FROM scripts", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ScriptAutomation(
                        id = it.getString(0),
                        title = it.getString(1),
                        description = it.getString(2),
                        language = it.getString(3),
                        code = it.getString(4),
                        trigger = it.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun saveScript(script: ScriptAutomation) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", script.id)
            put("title", script.title)
            put("description", script.description)
            put("language", script.language)
            put("code", script.code)
            put("trigger", script.trigger)
        }
        db.insertWithOnConflict("scripts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteScript(id: String) {
        writableDatabase.delete("scripts", "id = ?", arrayOf(id))
    }

    fun indexRagMemory(title: String, url: String, content: String) {
        if (content.isBlank()) return
        val db = writableDatabase
        val snippet = if (content.length > 500) content.take(500) + "..." else content
        val cv = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("content", snippet)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("rag_memory", null, cv)
    }

    fun searchRagMemory(query: String): List<RagMemoryItem> {
        val list = mutableListOf<RagMemoryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, title, url, content, timestamp FROM rag_memory WHERE title LIKE ? OR content LIKE ? ORDER BY timestamp DESC LIMIT 5",
            arrayOf("%$query%", "%$query%")
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RagMemoryItem(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        contentSnippet = it.getString(3),
                        timestamp = it.getLong(4)
                    )
                )
            }
        }
        return list
    }

    fun clearRagMemory() {
        writableDatabase.delete("rag_memory", null, null)
    }

    // --- Scheduled Tasks & Execution Logs ---

    fun getAllScheduledTasks(): List<ScheduledTaskEntity> {
        val list = mutableListOf<ScheduledTaskEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, title, command, recurrence_type, cron_expression, hour, minute, days_of_week, next_execution_time, is_active, created_at, last_run_time, last_run_status, retry_count FROM scheduled_tasks ORDER BY next_execution_time ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ScheduledTaskEntity(
                        id = it.getLong(0),
                        title = it.getString(1),
                        command = it.getString(2),
                        recurrenceType = it.getString(3),
                        cronExpression = it.getString(4),
                        hour = it.getInt(5),
                        minute = it.getInt(6),
                        daysOfWeek = it.getString(7),
                        nextExecutionTime = it.getLong(8),
                        isActive = it.getInt(9) == 1,
                        createdAt = it.getLong(10),
                        lastRunTime = if (it.isNull(11)) null else it.getLong(11),
                        lastRunStatus = if (it.isNull(12)) null else it.getString(12),
                        retryCount = it.getInt(13)
                    )
                )
            }
        }
        return list
    }

    fun getActiveScheduledTasks(): List<ScheduledTaskEntity> {
        return getAllScheduledTasks().filter { it.isActive }
    }

    fun getScheduledTaskById(id: Long): ScheduledTaskEntity? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, title, command, recurrence_type, cron_expression, hour, minute, days_of_week, next_execution_time, is_active, created_at, last_run_time, last_run_status, retry_count FROM scheduled_tasks WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        )
        cursor.use {
            if (it.moveToNext()) {
                return ScheduledTaskEntity(
                    id = it.getLong(0),
                    title = it.getString(1),
                    command = it.getString(2),
                    recurrenceType = it.getString(3),
                    cronExpression = it.getString(4),
                    hour = it.getInt(5),
                    minute = it.getInt(6),
                    daysOfWeek = it.getString(7),
                    nextExecutionTime = it.getLong(8),
                    isActive = it.getInt(9) == 1,
                    createdAt = it.getLong(10),
                    lastRunTime = if (it.isNull(11)) null else it.getLong(11),
                    lastRunStatus = if (it.isNull(12)) null else it.getString(12),
                    retryCount = it.getInt(13)
                )
            }
        }
        return null
    }

    fun insertOrUpdateScheduledTask(task: ScheduledTaskEntity): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("command", task.command)
            put("recurrence_type", task.recurrenceType)
            put("cron_expression", task.cronExpression)
            put("hour", task.hour)
            put("minute", task.minute)
            put("days_of_week", task.daysOfWeek)
            put("next_execution_time", task.nextExecutionTime)
            put("is_active", if (task.isActive) 1 else 0)
            put("created_at", task.createdAt)
            put("last_run_time", task.lastRunTime)
            put("last_run_status", task.lastRunStatus)
            put("retry_count", task.retryCount)
        }
        return if (task.id > 0) {
            db.update("scheduled_tasks", cv, "id = ?", arrayOf(task.id.toString()))
            task.id
        } else {
            db.insert("scheduled_tasks", null, cv)
        }
    }

    fun deleteScheduledTask(id: Long) {
        val db = writableDatabase
        db.delete("scheduled_tasks", "id = ?", arrayOf(id.toString()))
        db.delete("task_execution_logs", "task_id = ?", arrayOf(id.toString()))
    }

    fun updateTaskStatus(id: Long, isActive: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_active", if (isActive) 1 else 0)
        }
        db.update("scheduled_tasks", cv, "id = ?", arrayOf(id.toString()))
    }

    fun updateTaskExecutionResult(
        id: Long,
        lastRunTime: Long,
        lastRunStatus: String,
        nextExecutionTime: Long,
        retryCount: Int = 0,
        isActive: Boolean = true
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("last_run_time", lastRunTime)
            put("last_run_status", lastRunStatus)
            put("next_execution_time", nextExecutionTime)
            put("retry_count", retryCount)
            put("is_active", if (isActive) 1 else 0)
        }
        db.update("scheduled_tasks", cv, "id = ?", arrayOf(id.toString()))
    }

    fun insertExecutionLog(log: TaskExecutionLogEntity): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("task_id", log.taskId)
            put("timestamp", log.timestamp)
            put("result", log.result)
            put("is_success", if (log.isSuccess) 1 else 0)
            put("error_message", log.errorMessage)
        }
        return db.insert("task_execution_logs", null, cv)
    }

    fun getExecutionLogsForTask(taskId: Long): List<TaskExecutionLogEntity> {
        val list = mutableListOf<TaskExecutionLogEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, task_id, timestamp, result, is_success, error_message FROM task_execution_logs WHERE task_id = ? ORDER BY timestamp DESC LIMIT 50",
            arrayOf(taskId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TaskExecutionLogEntity(
                        id = it.getLong(0),
                        taskId = it.getLong(1),
                        timestamp = it.getLong(2),
                        result = it.getString(3),
                        isSuccess = it.getInt(4) == 1,
                        errorMessage = if (it.isNull(5)) null else it.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun getAllExecutionLogs(): List<TaskExecutionLogEntity> {
        val list = mutableListOf<TaskExecutionLogEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, task_id, timestamp, result, is_success, error_message FROM task_execution_logs ORDER BY timestamp DESC LIMIT 100",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TaskExecutionLogEntity(
                        id = it.getLong(0),
                        taskId = it.getLong(1),
                        timestamp = it.getLong(2),
                        result = it.getString(3),
                        isSuccess = it.getInt(4) == 1,
                        errorMessage = if (it.isNull(5)) null else it.getString(5)
                    )
                )
            }
        }
        return list
    }
}
