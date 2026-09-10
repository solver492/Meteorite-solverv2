package com.example.wormhole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wormhole.data.ActionStatus
import com.example.wormhole.data.AgentAction

@Composable
fun AgentModeDialog(
    viewModel: EdgeCometViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F)
                    )
                    Text(
                        text = "Agent Navigation Mode",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x88FFFFFF))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Auto-pilot toggle card
                Surface(
                    color = Color(0xFF221F3E),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Pilot Mode",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Execute approved sequences automatically without manual tap",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = viewModel.isAutoPilotEnabled,
                            onCheckedChange = { viewModel.isAutoPilotEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD54F),
                                checkedTrackColor = Color(0x66FFD54F)
                            )
                        )
                    }
                }

                // Status message
                Text(
                    text = viewModel.agentStatusText,
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Actions list
                Text(
                    text = "Proposed Action Plan (${viewModel.proposedActions.size} steps):",
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (viewModel.proposedActions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending actions. Ask the assistant to interact with this page.",
                            color = Color(0x66FFFFFF),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(viewModel.proposedActions) { idx, action ->
                            ActionItemCard(
                                stepNumber = idx + 1,
                                action = action,
                                onExecute = { viewModel.executeAction(action) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (viewModel.proposedActions.isNotEmpty()) {
                Button(
                    onClick = { viewModel.executeAllActions() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF0F0E1A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("execute_all_actions_button")
                ) {
                    Text("Execute All Steps", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xAAFFFFFF))
            }
        },
        containerColor = Color(0xFF17142F),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ActionItemCard(
    stepNumber: Int,
    action: AgentAction,
    onExecute: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1B38),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (action.status) {
                ActionStatus.EXECUTED -> Color(0xFF81C784)
                ActionStatus.EXECUTING -> Color(0xFFFFD54F)
                ActionStatus.FAILED -> Color(0xFFFF5252)
                else -> Color(0x22FFFFFF)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Step Badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = action.type.name,
                            color = Color(0xFF64B5F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${action.status.name}",
                            color = when (action.status) {
                                ActionStatus.EXECUTED -> Color(0xFF81C784)
                                ActionStatus.FAILED -> Color(0xFFFF5252)
                                else -> Color(0x88FFFFFF)
                            },
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = action.description,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (action.executionResult.isNotBlank()) {
                        Text(
                            text = action.executionResult,
                            color = Color(0xAA81C784),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (action.status != ActionStatus.EXECUTED) {
                IconButton(
                    onClick = onExecute,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color(0xFFFFD54F)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
