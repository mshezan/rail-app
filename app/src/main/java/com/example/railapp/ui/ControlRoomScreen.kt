package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railapp.domain.train.*
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun formatPlatform(platform: String): String {
    return if (platform.startsWith("Platform", ignoreCase = true)) platform else "Platform $platform"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlRoomScreen(
    viewModel: ControlRoomViewModel,
    passengerDelaysViewModel: PassengerDelaysViewModel? = null,
    onSelectTrainForTracking: (trainId: String, stationId: String) -> Unit = { _, _ -> }
) {
    val stations by viewModel.stations.collectAsState()
    val operatingStation by viewModel.operatingStation.collectAsState()
    val approachingTrains by viewModel.approachingTrains.collectAsState()
    val priorityActions by viewModel.priorityActions.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var stationPickerExpanded by remember { mutableStateOf(false) }
    var selectedTrainForPublish by remember { mutableStateOf<StationTrainOperationalInfo?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dashboard Header & Operating Station Picker & Time Row
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Control Room",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = if (isOffline) StatusMediumThreatContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CellTower,
                                contentDescription = null,
                                tint = if (isOffline) StatusMediumThreat else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOffline) "OFFLINE" else "SIMULATED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOffline) StatusMediumThreat else MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = "Updated ${timeFormat.format(Date(lastUpdatedTime))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Train Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "SEARCH TRAIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val searchInteractionSource = remember { MutableInteractionSource() }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            singleLine = true,
                            interactionSource = searchInteractionSource,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                OutlinedTextFieldDefaults.DecorationBox(
                                    value = searchQuery,
                                    innerTextField = innerTextField,
                                    enabled = true,
                                    singleLine = true,
                                    visualTransformation = VisualTransformation.None,
                                    interactionSource = searchInteractionSource,
                                    placeholder = {
                                        Text(
                                            text = "Enter Train number",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { viewModel.clearSearch() },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    container = {
                                        OutlinedTextFieldDefaults.ContainerBox(
                                            enabled = true,
                                            isError = searchError != null,
                                            interactionSource = searchInteractionSource,
                                            colors = OutlinedTextFieldDefaults.colors(),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                )
                            }
                        )

                        Button(
                            onClick = { viewModel.performSearch() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("SEARCH", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    searchError?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    if (isSearching) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Searching...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    searchResult?.let { info ->
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("${info.train.trainNumber} ${info.train.trainName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${info.train.origin.substringBefore(" (")} ➔ ${info.train.destination.substringBefore(" (")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+${info.prediction.currentDelayMinutes} min", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Predicted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+${info.prediction.predictedDelayMinutes} min", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = StatusHighThreat)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ETA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeFormat.format(Date(info.status.predictedArrival)), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onSelectTrainForTracking(info.train.id, info.schedule.stationId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VIEW TRAIN")
                        }
                    }
                }
            }
        }

        // Priority Actions
        if (priorityActions.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LowPriority,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRIORITY ACTIONS",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${priorityActions.size} NEED ATTENTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            priorityActions.forEach { action ->
                                PriorityActionItemRow(
                                    item = action,
                                    onReview = { onSelectTrainForTracking(action.trainId, action.stationId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Approaching Trains Header & List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Approaching Trains",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (approachingTrains.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Approaching Trains",
                    description = "No active trains detected for the operating station.",
                    icon = Icons.Default.DirectionsRailway,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        } else {
            items(
                items = approachingTrains,
                key = { info -> "${info.train.id}_${info.schedule.stationId}" }
            ) { info ->
                ApproachingTrainCard(
                    info = info,
                    onClick = { onSelectTrainForTracking(info.train.id, info.schedule.stationId) },
                    onUpdatePassengerDelay = { selectedTrainForPublish = info }
                )
            }
        }
    }

    selectedTrainForPublish?.let { info ->
        val trainHistory = passengerDelaysViewModel?.passengerActions?.collectAsState()?.value
            ?.filter { it.trainId == info.train.id }
            ?: emptyList()

        EditDelayDialog(
            info = info,
            history = trainHistory,
            onConfirm = { newDelay, reason ->
                passengerDelaysViewModel?.updatePassengerDelay(info, newDelay, reason)
                selectedTrainForPublish = null
            },
            onDismiss = { selectedTrainForPublish = null }
        )
    }
}

val PREDEFINED_DELAY_REASONS = listOf(
    "Speed restriction",
    "Signal halt",
    "Congestion ahead",
    "Weather",
    "Unscheduled stoppage",
    "Maintenance block",
    "Previous train delay",
    "Operational issue",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDelayDialog(
    info: StationTrainOperationalInfo,
    history: List<PassengerDelayAction> = emptyList(),
    onConfirm: (newDelay: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentDelay = info.activePassengerDelayMinutes ?: info.prediction.predictedDelayMinutes
    var newDelayInput by remember { mutableStateOf(currentDelay.toString()) }
    var selectedReason by remember { mutableStateOf(PREDEFINED_DELAY_REASONS.first()) }
    var customReasonInput by remember { mutableStateOf("") }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Update Passenger Delay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${info.train.trainNumber} - ${info.train.trainName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CURRENT DELAY READOUT
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT DELAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+$currentDelay min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // NEW PASSENGER DELAY INPUT
                OutlinedTextField(
                    value = newDelayInput,
                    onValueChange = { newDelayInput = it.filter { char -> char.isDigit() } },
                    label = { Text("NEW PASSENGER DELAY (MINUTES)") },
                    placeholder = { Text("e.g. 20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // REASON DROPDOWN SELECTOR
                ExposedDropdownMenuBox(
                    expanded = reasonDropdownExpanded,
                    onExpandedChange = { reasonDropdownExpanded = !reasonDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DELAY REASON") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = reasonDropdownExpanded,
                        onDismissRequest = { reasonDropdownExpanded = false }
                    ) {
                        PREDEFINED_DELAY_REASONS.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedReason = reason
                                    reasonDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // CUSTOM REASON INPUT IF OTHER SELECTED
                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = customReasonInput,
                        onValueChange = { customReasonInput = it },
                        label = { Text("SPECIFY CUSTOM REASON") },
                        placeholder = { Text("Enter reason...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // DELAY UPDATE HISTORY
                if (history.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "UPDATE HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        history.sortedByDescending { it.createdAt }.take(4).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "+${item.previousDelayMinutes}m ➔ +${item.newDelayMinutes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(item.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delay = newDelayInput.toIntOrNull() ?: currentDelay
                    val finalReason = if (selectedReason == "Other") {
                        customReasonInput.ifBlank { "Operational issue" }
                    } else {
                        selectedReason
                    }
                    onConfirm(delay, finalReason)
                },
                shape = RoundedCornerShape(8.dp)
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

    val (statusText, statusColor, statusContainer) = when {
        prediction.predictedDelayMinutes > 15 -> Triple(
            "Delayed",
            StatusHighThreat,
            if (dark) StatusHighThreatDarkContainer else StatusHighThreatContainer
        )
        prediction.predictedDelayMinutes > 5 -> Triple(
            "Slight delay",
            StatusMediumThreat,
            if (dark) StatusMediumThreatDarkContainer else StatusMediumThreatContainer
        )
        else -> Triple(
            "On time",
            StatusSafe,
            if (dark) StatusSafeDarkContainer else StatusSafeContainer
        )
    }

    val (trendIcon, trendText, trendColor) = when (prediction.trend) {
        PredictionTrend.WORSENING -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            "+${prediction.predictedDelayMinutes - prediction.currentDelayMinutes}m worsening",
            StatusHighThreat
        )
        PredictionTrend.IMPROVING -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            "${prediction.predictedDelayMinutes - prediction.currentDelayMinutes}m recovering",
            StatusSafe
        )
        PredictionTrend.STABLE -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            "Stable",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Train Number, Name, Status Badge
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
                    color = statusContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatPlatform(schedule.platform),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: 3-Column Delay Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${prediction.currentDelayMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("PREDICTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${prediction.predictedDelayMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("ARRIVAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeFormat.format(Date(prediction.predictedArrival)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
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
                        text = "Passengers see +${info.activePassengerDelayMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    Text("UPDATE", style = MaterialTheme.typography.labelSmall)
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

                AdminDetailSectionHeader("PREDICTION ANALYSIS")
                AdminDetailRow("Predicted Delay", "+${prediction.predictedDelayMinutes} minutes")
                AdminDetailRow("Predicted Arrival", timeFormat.format(Date(prediction.predictedArrival)))
                AdminDetailRow("Prediction Trend", prediction.trend.name)
                AdminDetailRow("Severity Risk", prediction.riskSeverity.name)
                AdminDetailRow("Confidence", "${prediction.confidence}%")

                HorizontalDivider()

                AdminDetailSectionHeader("OPERATIONAL BOTTLENECK ANALYSIS")
                AdminDetailRow("Rail Zone", prediction.operationalFactors.operationalZone)
                AdminDetailRow("Primary Bottleneck", prediction.operationalFactors.primaryCauseDescription)
                AdminDetailRow("Speed Restriction", "${prediction.operationalFactors.speedRestrictionKmh ?: "None"} km/h")
                AdminDetailRow("Weather Condition", prediction.operationalFactors.weatherCondition)
                AdminDetailRow("GPS Accuracy", "±${prediction.operationalFactors.gpsAccuracyMeters}m")

                HorizontalDivider()

                AdminDetailSectionHeader("MODEL METADATA")
                AdminDetailRow("Model", "RailETA Neural Engine")
                AdminDetailRow("Version", prediction.modelVersion)
                AdminDetailRow("Generated", timeFormat.format(Date(prediction.predictionTimestamp)))
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
fun PriorityActionItemRow(
    item: PriorityActionItem,
    onReview: () -> Unit
) {
    val dark = isSystemInDarkTheme()

    val (priorityLabel, priorityColor, priorityContainer) = when (item.priority) {
        ActionPriority.HIGH -> Triple(
            "HIGH",
            StatusHighThreat,
            if (dark) StatusHighThreatDarkContainer else StatusHighThreatContainer
        )
        ActionPriority.MEDIUM -> Triple(
            "MEDIUM",
            StatusMediumThreat,
            if (dark) StatusMediumThreatDarkContainer else StatusMediumThreatContainer
        )
        ActionPriority.LOW -> Triple(
            "LOW",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Priority Badge & Train Number + Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = priorityContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = priorityLabel,
                            color = priorityColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.trainNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.trainName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onReview,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp),
                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = "REVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = priorityColor
                    )
                }
            }

            if (!item.routeSubtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.routeSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Delay Line: Current vs Predicted
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current: +${item.currentDelayMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "Predicted: +${item.predictedDelayMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = priorityColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Reasons List
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.reasons.forEach { reason ->
                    Text(
                        text = "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (item.priority == ActionPriority.HIGH) priorityColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

