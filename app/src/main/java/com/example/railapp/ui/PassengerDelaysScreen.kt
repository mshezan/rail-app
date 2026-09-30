package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.railapp.domain.train.DataSourceType
import com.example.railapp.domain.train.DelayPrediction
import com.example.railapp.domain.train.DelayRiskSeverity
import com.example.railapp.domain.train.PassengerDelayAction
import com.example.railapp.domain.train.PassengerDelayActionStatus
import com.example.railapp.domain.train.PassengerDelayActionType
import com.example.railapp.domain.train.PredictionTrend
import com.example.railapp.domain.train.StationTrainOperationalInfo
import com.example.railapp.domain.train.Train
import com.example.railapp.domain.train.TrainStationSchedule
import com.example.railapp.domain.train.TrainStatus
import com.example.railapp.domain.train.TrainStatusType
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PassengerDelaysScreen(viewModel: PassengerDelaysViewModel) {
    val passengerActions by viewModel.passengerActions.collectAsState()
    var selectedActionForEdit by remember { mutableStateOf<PassengerDelayAction?>(null) }
    var selectedActionForRevert by remember { mutableStateOf<PassengerDelayAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PASSENGER UPDATES",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Published Delay Communications Audit Trail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${passengerActions.count { it.status == PassengerDelayActionStatus.ACTIVE }} ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (passengerActions.isEmpty()) {
            EmptyStateView(
                title = "NO PUBLISHED UPDATES",
                description = "No passenger delay updates have been published from the Control Room yet.",
                icon = Icons.Default.Campaign,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(passengerActions) { action ->
                    PassengerActionCard(
                        action = action,
                        onEdit = { selectedActionForEdit = action },
                        onRevert = { selectedActionForRevert = action }
                    )
                }
            }
        }
    }

    // Edit Published Delay Dialog
    selectedActionForEdit?.let { action ->
        EditDelayDialog(
            action = action,
            onConfirm = { newDelay, reason ->
                // Publish update
                val mockInfo = StationTrainOperationalInfo(
                    train = Train(action.trainId, action.trainNumber, action.trainName, action.origin, action.destination),
                    schedule = TrainStationSchedule(action.trainId, action.stationId, System.currentTimeMillis(), System.currentTimeMillis(), "P1"),
                    status = TrainStatus(action.trainId, action.stationId, System.currentTimeMillis(), System.currentTimeMillis(), action.newDelayMinutes, action.newDelayMinutes, TrainStatusType.DELAYED, 85, System.currentTimeMillis(), DataSourceType.MOCK),
                    prediction = DelayPrediction(action.trainId, action.stationId, action.previousDelayMinutes, action.newDelayMinutes, System.currentTimeMillis(), System.currentTimeMillis(), 85, PredictionTrend.STABLE, DelayRiskSeverity.LOW)
                )
                viewModel.updatePassengerDelay(mockInfo, newDelay, reason)
                selectedActionForEdit = null
            },
            onDismiss = { selectedActionForEdit = null }
        )
    }

    // Revert Action Dialog
    selectedActionForRevert?.let { action ->
        AlertDialog(
            onDismissRequest = { selectedActionForRevert = null },
            icon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = StatusHighThreat) },
            title = { Text("Revert Passenger Update") },
            text = {
                Text("Are you sure you want to revert the published update (+${action.newDelayMinutes}m) for Train ${action.trainNumber}? This will create a reversal record in the audit trail.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revertPassengerDelay(action, "Reverted by Control Room Admin")
                        selectedActionForRevert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusHighThreat)
                ) {
                    Text("REVERT UPDATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedActionForRevert = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun PassengerActionCard(
    action: PassengerDelayAction,
    onEdit: () -> Unit,
    onRevert: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault())
    val dark = isSystemInDarkTheme()

    val isActive = action.status == PassengerDelayActionStatus.ACTIVE
    val statusColor = if (isActive) {
        when (action.actionType) {
            PassengerDelayActionType.PUBLISH -> StatusInfoCloud
            PassengerDelayActionType.UPDATE -> StatusMediumThreat
            PassengerDelayActionType.REVERT -> StatusHighThreat
        }
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (isActive) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Train & Action Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = action.trainNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.trainName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isActive) action.actionType.name else "REVERTED",
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${action.origin} ➔ ${action.destination}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Previous Delay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${action.previousDelayMinutes} min", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text("Published Delay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${action.newDelayMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Published At", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = dateFormat.format(Date(action.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Reason: ${action.reason} • By: ${action.createdBy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onRevert,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusHighThreat)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REVERT", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun EditDelayDialog(
    action: PassengerDelayAction,
    onConfirm: (newDelay: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newDelayInput by remember { mutableStateOf(action.newDelayMinutes.toString()) }
    var reasonInput by remember { mutableStateOf("Updated by Station Operator") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Passenger Delay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Train: ${action.trainNumber} (${action.trainName})", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newDelayInput,
                    onValueChange = { newDelayInput = it },
                    label = { Text("Passenger Delay (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Update Reason / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delay = newDelayInput.toIntOrNull() ?: action.newDelayMinutes
                    onConfirm(delay, reasonInput)
                }
            ) {
                Text("PUBLISH UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
