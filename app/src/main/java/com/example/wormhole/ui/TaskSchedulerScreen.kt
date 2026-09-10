package com.example.wormhole.ui

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wormhole.scheduler.CronEngine
import com.example.wormhole.scheduler.ScheduledTaskEntity
import com.example.wormhole.scheduler.TaskAlarmScheduler
import com.example.wormhole.scheduler.TaskExecutionLogEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSchedulerScreen(
    viewModel: EdgeCometViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var taskToEdit by remember { mutableStateOf<ScheduledTaskEntity?>(null) }
    var isEditSheetOpen by remember { mutableStateOf(false) }
    var taskForHistory by remember { mutableStateOf<ScheduledTaskEntity?>(null) }
    var isRunningTaskId by remember { mutableStateOf<Long?>(null) }
    var recentRunResult by remember { mutableStateOf<Pair<String, String>?>(null) }

    val canExactAlarm = remember { TaskAlarmScheduler.canScheduleExactAlarms(context) }
    val isBatteryOptimized = remember { TaskAlarmScheduler.isBatteryOptimizationIgnored(context) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color(0xFF0D0B18),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141226))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF26214A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Planificateur IA",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${viewModel.activeTasksCount} / ${TaskAlarmScheduler.MAX_ACTIVE_TASKS} tâches actives",
                                color = Color(0xFFA5A0D6),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFFB74D)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Tâches (${viewModel.scheduledTasks.size})",
                                color = if (selectedTab == 0) Color.White else Color(0x88FFFFFF),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Historique (${viewModel.taskExecutionLogs.size})",
                                color = if (selectedTab == 1) Color.White else Color(0x88FFFFFF),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        taskToEdit = null
                        isEditSheetOpen = true
                    },
                    containerColor = Color(0xFFFFB74D),
                    contentColor = Color(0xFF0F0D20)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nouvelle tâche")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission and Battery warning banners
            if (!canExactAlarm) {
                BannerWarning(
                    title = "Alarmes exactes désactivées",
                    message = "L'app utilise le mode WorkManager approximatif. Autorisez les alarmes exactes pour une précision horaire exacte.",
                    actionLabel = "Paramètres",
                    onAction = {
                        try {
                            context.startActivity(TaskAlarmScheduler.createExactAlarmSettingIntent(context))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Ouvrez Paramètres > Applications", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (!isBatteryOptimized) {
                BannerWarning(
                    title = "Optimisation batterie active",
                    message = "Le mode Doze peut différer l'exécution en veille profonde. Désactivez l'optimisation pour une fiabilité maximale.",
                    actionLabel = "Optimiser",
                    onAction = {
                        try {
                            context.startActivity(TaskAlarmScheduler.createBatteryOptimizationIntent(context))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Ouvrez Paramètres de la batterie", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // Task List Tab
                if (viewModel.scheduledTasks.isEmpty()) {
                    EmptyTasksPlaceholder(
                        onCreateClick = {
                            taskToEdit = null
                            isEditSheetOpen = true
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.scheduledTasks, key = { it.id }) { task ->
                            ScheduledTaskCard(
                                task = task,
                                isRunning = isRunningTaskId == task.id,
                                onToggleActive = { viewModel.toggleTaskActive(task) },
                                onRunNow = {
                                    isRunningTaskId = task.id
                                    viewModel.runTaskNow(task) { result ->
                                        isRunningTaskId = null
                                        recentRunResult = Pair(task.title, result)
                                    }
                                },
                                onEdit = {
                                    taskToEdit = task
                                    isEditSheetOpen = true
                                },
                                onDelete = { viewModel.deleteScheduledTask(task.id) },
                                onHistory = { taskForHistory = task }
                            )
                        }
                    }
                }
            } else {
                // All Execution Logs Tab
                if (viewModel.taskExecutionLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucun historique d'exécution disponible",
                            color = Color(0x88FFFFFF),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.taskExecutionLogs, key = { it.id }) { log ->
                            TaskLogCard(log = log)
                        }
                    }
                }
            }
        }
    }

    // Modal Edit / Create Task Sheet
    if (isEditSheetOpen) {
        TaskEditSheet(
            task = taskToEdit,
            onDismiss = { isEditSheetOpen = false },
            onSave = { task ->
                viewModel.saveScheduledTask(
                    task = task,
                    onLimitReached = {
                        Toast.makeText(
                            context,
                            "Limite de ${TaskAlarmScheduler.MAX_ACTIVE_TASKS} tâches actives atteinte.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onSuccess = {
                        isEditSheetOpen = false
                        Toast.makeText(context, "Tâche planifiée avec succès", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // History Sheet for single task
    taskForHistory?.let { task ->
        TaskHistorySheet(
            task = task,
            viewModel = viewModel,
            onDismiss = { taskForHistory = null }
        )
    }

    // Dialog displaying immediate result of Run Now
    recentRunResult?.let { (title, result) ->
        RunResultDialog(
            title = title,
            result = result,
            onDismiss = { recentRunResult = null }
        )
    }
}

@Composable
fun BannerWarning(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2016)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFB74D)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color(0xFFFFB74D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Color(0xCCFFFFFF), fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Color(0xFFFFB74D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScheduledTaskCard(
    task: ScheduledTaskEntity,
    isRunning: Boolean,
    onToggleActive: () -> Unit,
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isActive) Color(0xFF191632) else Color(0xFF131124)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (task.isActive) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF373163))) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (task.isActive) Color(0xFF81C784) else Color(0xFF757575))
                    )
                    Text(
                        text = task.title,
                        color = if (task.isActive) Color.White else Color(0x88FFFFFF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = task.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB74D),
                        checkedTrackColor = Color(0xFF5D4037),
                        uncheckedThumbColor = Color(0xFF757575),
                        uncheckedTrackColor = Color(0xFF222038)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Natural language command box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF110F20))
                    .padding(10.dp)
            ) {
                Text(
                    text = "\"${task.command}\"",
                    color = Color(0xFFD1C4E9),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule info & Next run
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val recurrenceBadge = when (task.recurrenceType.uppercase()) {
                    "CRON" -> task.cronExpression
                    "ONCE" -> "Une fois à ${String.format("%02d:%02d", task.hour, task.minute)}"
                    "DAILY" -> "Quotidien à ${String.format("%02d:%02d", task.hour, task.minute)}"
                    "WEEKLY" -> "Hebdo à ${String.format("%02d:%02d", task.hour, task.minute)}"
                    "MONTHLY" -> "Mensuel à ${String.format("%02d:%02d", task.hour, task.minute)}"
                    else -> task.recurrenceType
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2B264F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = recurrenceBadge,
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = if (task.isActive) CronEngine.formatRelativeTime(task.nextExecutionTime) else "En pause",
                    color = if (task.isActive) Color(0xFF64B5F6) else Color(0x66FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Last Run Status if available
            if (task.lastRunTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSuccess = task.lastRunStatus == "SUCCESS"
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF81C784) else Color(0xFFE57373),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dernier run: ${CronEngine.formatDateTime(task.lastRunTime)} (${if (isSuccess) "Succès" else "Échec"})",
                        color = Color(0x88FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x22FFFFFF))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Run Now Button
                Button(
                    onClick = onRunNow,
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inférence...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exécuter", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onHistory, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique",
                            tint = Color(0xFFA5A0D6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = Color(0xFFA5A0D6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFEF9A9A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    task: ScheduledTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (ScheduledTaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var command by remember { mutableStateOf(task?.command ?: "") }
    var recurrenceType by remember { mutableStateOf(task?.recurrenceType ?: "DAILY") }
    var hour by remember { mutableIntStateOf(task?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(task?.minute ?: 0) }
    var cronExpression by remember { mutableStateOf(task?.cronExpression.orEmpty().ifBlank { "0 8 * * 1-5" }) }
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            val daysStr = task?.daysOfWeek.orEmpty().ifBlank { "1,2,3,4,5" }
            addAll(daysStr.split(",").mapNotNull { it.trim().toIntOrNull() })
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141226)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (task == null) "Nouvelle tâche planifiée IA" else "Modifier la tâche",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Libellé de la tâche", color = Color(0x88FFFFFF)) },
                placeholder = { Text("Ex: Synthèse Tech matinale") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFB74D),
                    unfocusedBorderColor = Color(0x44FFFFFF)
                )
            )

            // Natural language command
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Commande en langage naturel", color = Color(0x88FFFFFF)) },
                    placeholder = { Text("Ex: Résume les actualités tech de ma page d'accueil") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB74D),
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )

                // Quick suggestions
                Text("Suggestions rapides :", color = Color(0x88FFFFFF), fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Résume les actualités de ma page d'accueil",
                        "Vérifie le prix du billet et alerte si baisse",
                        "Fais un résumé synthétique de mes onglets ouverts"
                    ).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF231F3F))
                                .clickable { command = suggestion }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(suggestion, color = Color(0xFFD1C4E9), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Recurrence selector chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type de récurrence :", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ONCE" to "Une fois",
                        "DAILY" to "Quotidien",
                        "WEEKLY" to "Hebdomadaire",
                        "MONTHLY" to "Mensuel",
                        "CRON" to "Cron personnalisé"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = recurrenceType == key,
                            onClick = { recurrenceType = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFB74D),
                                selectedLabelColor = Color(0xFF0F0D20),
                                containerColor = Color(0xFF1E1B38),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Time Selector (Native Hour/Minute for ONCE, DAILY, WEEKLY, MONTHLY)
            if (recurrenceType != "CRON") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Heure d'exécution :", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hour Selector Box
                        NumberPickerBox(
                            label = "Heure",
                            value = hour,
                            min = 0,
                            max = 23,
                            onValueChange = { hour = it }
                        )

                        Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        // Minute Selector Box
                        NumberPickerBox(
                            label = "Minute",
                            value = minute,
                            min = 0,
                            max = 59,
                            onValueChange = { minute = it }
                        )
                    }
                }
            }

            // Day of week selector if WEEKLY
            if (recurrenceType == "WEEKLY") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jours d'exécution :", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf(1 to "L", 2 to "M", 3 to "M", 4 to "J", 5 to "V", 6 to "S", 7 to "D")
                        days.forEach { (dayNum, label) ->
                            val isSelected = selectedDays.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFFB74D) else Color(0xFF231F3F))
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays.remove(dayNum)
                                        } else {
                                            selectedDays.add(dayNum)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF0F0D20) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Custom Cron expression field if CRON
            if (recurrenceType == "CRON") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Expression Cron (5 champs) :", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = cronExpression,
                        onValueChange = { cronExpression = it },
                        placeholder = { Text("0 8 * * 1-5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (CronEngine.isValid(cronExpression)) Color(0xFF81C784) else Color(0xFFE57373),
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        )
                    )

                    // Real-time human-readable preview
                    val isValidCron = CronEngine.isValid(cronExpression)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isValidCron) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isValidCron) Color(0xFF81C784) else Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = CronEngine.describe(cronExpression),
                            color = if (isValidCron) Color(0xFF81C784) else Color(0xFFE57373),
                            fontSize = 12.sp
                        )
                    }

                    // Cron presets
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "0 8 * * 1-5" to "Ouvrés 8h",
                            "*/30 * * * *" to "30 min",
                            "0 12 * * *" to "Midi",
                            "0 9 * * 0,6" to "Week-ends"
                        ).forEach { (preset, lbl) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF231F3F))
                                    .clickable { cronExpression = preset }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$lbl ($preset)", color = Color(0xFFFFB74D), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save / Cancel Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        val finalTask = ScheduledTaskEntity(
                            id = task?.id ?: 0L,
                            title = title.ifBlank { "Tâche IA ${hour}h$minute" },
                            command = command.ifBlank { "Résume les actualités de la page" },
                            recurrenceType = recurrenceType,
                            cronExpression = cronExpression,
                            hour = hour,
                            minute = minute,
                            daysOfWeek = selectedDays.sorted().joinToString(","),
                            isActive = true
                        )
                        onSave(finalTask)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB74D),
                        contentColor = Color(0xFF0F0D20)
                    )
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun NumberPickerBox(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1B38))
            .padding(8.dp)
    ) {
        Text(label, color = Color(0x88FFFFFF), fontSize = 11.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) else onValueChange(max) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", color = Color(0xFFFFB74D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) else onValueChange(min) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", color = Color(0xFFFFB74D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TaskLogCard(log: TaskExecutionLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18152F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (log.isSuccess) Color(0xFF81C784) else Color(0xFFE57373),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (log.isSuccess) "Exécution réussie" else "Échec",
                        color = if (log.isSuccess) Color(0xFF81C784) else Color(0xFFE57373),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = CronEngine.formatDateTime(log.timestamp),
                    color = Color(0x88FFFFFF),
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF110F20))
                    .padding(10.dp)
            ) {
                Text(
                    text = log.result,
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistorySheet(
    task: ScheduledTaskEntity,
    viewModel: EdgeCometViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val taskLogs = remember(viewModel.taskExecutionLogs, task.id) {
        viewModel.taskExecutionLogs.filter { it.taskId == task.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141226)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historique : ${task.title}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${taskLogs.size} exécution(s) enregistrée(s)",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }

            if (taskLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune exécution enregistrée pour cette tâche.",
                        color = Color(0x88FFFFFF),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(taskLogs, key = { it.id }) { log ->
                        TaskLogCard(log = log)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RunResultDialog(
    title: String,
    result: String,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1B38),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF81C784)
                )
                Text(
                    text = "Résultat : $title",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = result,
                    color = Color(0xFFE0E0E0),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
            ) {
                Text("OK", color = Color(0xFF0F0D20), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun EmptyTasksPlaceholder(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF231F3F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Aucune tâche IA planifiée",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Programmez des requêtes automatiques en langage naturel exécutées 100% sur l'appareil : résumés réguliers, alertes, etc.",
                color = Color(0x99FFFFFF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB74D),
                    contentColor = Color(0xFF0F0D20)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer une tâche", fontWeight = FontWeight.Bold)
            }
        }
    }
}
