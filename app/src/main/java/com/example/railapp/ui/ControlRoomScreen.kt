package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railapp.domain.train.*
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlRoomScreen(
    viewModel: ControlRoomViewModel,
    passengerDelaysViewModel: PassengerDelaysViewModel? = null
) {
    val stations by viewModel.stations.collectAsState()
    val operatingStation by viewModel.operatingStation.collectAsState()
    val approachingTrains by viewModel.approachingTrains.collectAsState()
    val opsSummary by viewModel.opsSummary.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()

    var stationPickerExpanded by remember { mutableStateOf(false) }
    var selectedTrainForDetail by remember { mutableStateOf<StationTrainOperationalInfo?>(null) }
    var selectedTrainForPublish by remember { mutableStateOf<StationTrainOperationalInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dashboard Header with Network & ML Model Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONTROL ROOM",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Station Operations & Delay Prediction Terminal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (isOffline) StatusMediumThreatContainer else StatusSafeContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CellTower,
                        contentDescription = null,
                        tint = if (isOffline) StatusMediumThreat else StatusSafe,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "OFFLINE" else "LIVE NETWORK",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOffline) StatusMediumThreat else StatusSafe,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operating Station Picker
        ExposedDropdownMenuBox(
            expanded = stationPickerExpanded,
            onExpandedChange = { stationPickerExpanded = !stationPickerExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = operatingStation?.let { "${it.name} (${it.code})" } ?: "Select Station",
                onValueChange = {},
                readOnly = true,
                label = { Text("OPERATING STATION") },
                leadingIcon = { Icon(Icons.Default.Train, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationPickerExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = stationPickerExpanded,
                onDismissRequest = { stationPickerExpanded = false }
            ) {
                stations.forEach { station ->
                    DropdownMenuItem(
                        text = {
                            Text("${station.name} (${station.code})", fontWeight = FontWeight.Bold)
                        },
                        onClick = {
                            viewModel.selectOperatingStation(station.id)
                            stationPickerExpanded = false
                        }
                    )
                }
            }
        }

        // ML Prototype Banner & Offline timestamp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "ML MODEL: PROTOTYPE-V1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            Text(
                text = "Updated: ${dateFormat.format(Date(lastUpdatedTime))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operational Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpsStatCard(
                label = "APPROACHING",
                value = opsSummary.approachingCount.toString(),
                icon = Icons.Default.DirectionsRailway,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            OpsStatCard(
                label = "CURRENT DELAY",
                value = "+${opsSummary.currentAvgDelayMinutes}m",
                icon = Icons.Default.Schedule,
                color = StatusMediumThreat,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpsStatCard(
                label = "ML PREDICTED",
                value = "+${opsSummary.predictedAvgDelayMinutes}m",
                icon = Icons.Default.Psychology,
                color = StatusHighThreat,
                modifier = Modifier.weight(1f)
            )
            OpsStatCard(
                label = "HIGH RISK",
                value = opsSummary.highRiskCount.toString(),
                icon = Icons.Default.Warning,
                color = StatusHighThreat,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "APPROACHING TRAINS & PREDICTIONS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Approaching Trains List
        if (approachingTrains.isEmpty()) {
            EmptyStateView(
                title = "NO APPROACHING TRAINS",
                description = "No active trains detected for the operating station.",
                icon = Icons.Default.DirectionsRailway,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(approachingTrains) { info ->
                    ApproachingTrainCard(
                        info = info,
                        onClick = { selectedTrainForDetail = info },
                        onUpdatePassengerDelay = { selectedTrainForPublish = info }
                    )
                }
            }
        }
    }

    selectedTrainForDetail?.let { info ->
        PredictionDetailDialog(info = info, onDismiss = { selectedTrainForDetail = null })
    }

    selectedTrainForPublish?.let { info ->
        val mockAction = PassengerDelayAction(
            trainId = info.train.id,
            stationId = info.schedule.stationId,
            trainNumber = info.train.trainNumber,
            trainName = info.train.trainName,
            origin = info.train.origin,
            destination = info.train.destination,
            previousDelayMinutes = info.prediction.currentDelayMinutes,
            newDelayMinutes = info.prediction.predictedDelayMinutes,
            actionType = PassengerDelayActionType.PUBLISH
        )
        EditDelayDialog(
            action = mockAction,
            onConfirm = { newDelay, reason ->
                passengerDelaysViewModel?.updatePassengerDelay(info, newDelay, reason)
                selectedTrainForPublish = null
            },
            onDismiss = { selectedTrainForPublish = null }
        )
    }
}

@Composable
fun ApproachingTrainCard(
    info: StationTrainOperationalInfo,
    onClick: () -> Unit,
    onUpdatePassengerDelay: () -> Unit
) {
    val train = info.train
    val schedule = info.schedule
    val prediction = info.prediction
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dark = isSystemInDarkTheme()

    val riskColor = when (prediction.riskSeverity) {
        DelayRiskSeverity.HIGH -> StatusHighThreat
        DelayRiskSeverity.MODERATE -> StatusMediumThreat
        DelayRiskSeverity.LOW -> StatusSafe
    }

    val riskContainer = when (prediction.riskSeverity) {
        DelayRiskSeverity.HIGH -> if (dark) StatusHighThreatDarkContainer else StatusHighThreatContainer
        DelayRiskSeverity.MODERATE -> if (dark) StatusMediumThreatDarkContainer else StatusMediumThreatContainer
        DelayRiskSeverity.LOW -> if (dark) StatusSafeDarkContainer else StatusSafeContainer
    }

    val (trendIcon, trendText, trendColor) = when (prediction.trend) {
        PredictionTrend.WORSENING -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            "DELAY INCREASING (+${prediction.predictedDelayMinutes - prediction.currentDelayMinutes}M)",
            StatusHighThreat
        )
        PredictionTrend.IMPROVING -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            "RECOVERING (${prediction.predictedDelayMinutes - prediction.currentDelayMinutes}M)",
            StatusSafe
        )
        PredictionTrend.STABLE -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            "STABLE DELAY",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Train Number, Name, Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = train.trainNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = train.trainName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = riskContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${prediction.riskSeverity} RISK",
                        color = riskColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Route Origin -> Destination & Platform
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${train.origin} ➔ ${train.destination}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Platform ${schedule.platform}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Current Delay vs ML Predicted Delay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Current Delay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${prediction.currentDelayMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("ML Predicted Delay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${prediction.predictedDelayMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Expected Arrival", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeFormat.format(Date(prediction.predictedArrival)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (info.activePassengerDelayMinutes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PUBLIC PASSENGER UPDATE: +${info.activePassengerDelayMinutes}m MIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Trend Indicator & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onUpdatePassengerDelay,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UPDATE PASSENGER", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PredictionDetailDialog(
    info: StationTrainOperationalInfo,
    onDismiss: () -> Unit
) {
    val train = info.train
    val schedule = info.schedule
    val prediction = info.prediction
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${train.trainNumber} - ${train.trainName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${train.origin} ➔ ${train.destination}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminDetailSectionHeader("CURRENT STATUS")
                AdminDetailRow("Current Delay", "+${prediction.currentDelayMinutes} minutes")
                AdminDetailRow("Scheduled Arrival", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(schedule.scheduledArrival)))

                HorizontalDivider()

                AdminDetailSectionHeader("ML PREDICTION ANALYSIS")
                AdminDetailRow("Predicted Delay at Station", "+${prediction.predictedDelayMinutes} minutes")
                AdminDetailRow("Predicted Arrival", timeFormat.format(Date(prediction.predictedArrival)))
                AdminDetailRow("Prediction Trend", prediction.trend.name)
                AdminDetailRow("Severity Risk", prediction.riskSeverity.name)
                AdminDetailRow("Model Confidence", "${prediction.confidence}%")

                HorizontalDivider()

                AdminDetailSectionHeader("MODEL METADATA")
                AdminDetailRow("Model Engine", "Prototype ML")
                AdminDetailRow("Model Version", prediction.modelVersion)
                AdminDetailRow("Data Source", prediction.dataSource)
                AdminDetailRow("Generated At", timeFormat.format(Date(prediction.predictionTimestamp)))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun AdminDetailSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun AdminDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OpsStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
