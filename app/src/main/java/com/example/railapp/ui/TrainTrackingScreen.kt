package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railapp.auth.UserRole
import com.example.railapp.domain.train.ForecastSource
import com.example.railapp.domain.train.RouteStationStop
import com.example.railapp.domain.train.StopStatus
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainTrackingScreen(
    viewModel: TrainTrackingViewModel,
    onBack: () -> Unit
) {
    val trackingData by viewModel.trackingData.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val showGuestPrompt by viewModel.showGuestPrompt.collectAsState()

    val isAdmin = currentRole == UserRole.ADMIN

    val tracking = trackingData

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (tracking != null) {
                        Column {
                            Text(
                                text = "${tracking.trainNumber} ${tracking.trainName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tracking.origin.substringBefore(" (")} → ${tracking.destination.substringBefore(" (")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Train Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isAdmin && tracking != null) {
                        IconButton(onClick = { viewModel.toggleSaveTrain() }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Save Train",
                                tint = if (isSaved) StatusHighThreat else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (tracking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val delayMinutes = tracking.predictedDelayMinutes
            val delayColor = when {
                delayMinutes > 15 -> StatusHighThreat
                delayMinutes > 5 -> StatusMediumThreat
                else -> StatusSafe
            }

            val listState = rememberLazyListState()
            val currentStopIndex = tracking.routeStops.indexOfFirst { it.status == StopStatus.CURRENT }
            LaunchedEffect(currentStopIndex) {
                if (currentStopIndex >= 0) {
                    listState.scrollToItem(currentStopIndex)
                }
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val nextStop = tracking.routeStops.find { it.status == StopStatus.UPCOMING } ?: tracking.routeStops.last()
            val nextEtaStr = timeFormat.format(Date(nextStop.expectedTime))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, delayColor.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Big Status Line
                        Text(
                            text = if (delayMinutes > 0) "Running $delayMinutes min late" else "On time",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = delayColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Near Station
                        Text(
                            text = "Near ${tracking.currentStationName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Next Station & ETA
                        Text(
                            text = "Next: ${nextStop.stationName} · ETA $nextEtaStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        // Latest Reason & Update Status (Passenger/Guest primary info)
                        if (delayMinutes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val latestReason = tracking.etaUpdates.firstOrNull()?.reason
                                ?: tracking.operationalFactors.primaryCauseDescription
                            Text(
                                text = "$latestReason · Updated ${tracking.updatedMinutesAgo} min ago",
                                style = MaterialTheme.typography.bodySmall,
                                color = delayColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Thin Route Progress Bar with Train Icon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            LinearProgressIndicator(
                                progress = { tracking.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(tracking.progressPercent.coerceIn(0.05f, 0.95f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(20.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsRailway,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Forecast Source Chip & Update Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (tracking.forecastSource == ForecastSource.ML_FORECAST) MaterialTheme.colorScheme.primaryContainer else StatusMediumThreatContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (tracking.forecastSource == ForecastSource.ML_FORECAST) "ML forecast" else "Schedule-based",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tracking.forecastSource == ForecastSource.ML_FORECAST) MaterialTheme.colorScheme.onPrimaryContainer else StatusMediumThreat,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (tracking.forecastSource == ForecastSource.SCHEDULE_BASED) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Live data delayed, showing scheduled ETA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusMediumThreat,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = "Updated ${tracking.updatedMinutesAgo} min ago · SIMULATED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isAdmin) {
                    val activeAction by viewModel.activePassengerAction.collectAsState()
                    var showPublishDialog by remember { mutableStateOf(false) }

                    val currentPassengerUpdate = activeAction?.newDelayMinutes ?: tracking.currentDelayMinutes
                    val newPrediction = tracking.predictedDelayMinutes

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PASSENGER DELAY PUBLISHING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (activeAction != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PUBLISHED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Current passenger update",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+$currentPassengerUpdate min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "New prediction",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+$newPrediction min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusHighThreat
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Reason",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = activeAction?.reason ?: "Speed restriction",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showPublishDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PUBLISH / UPDATE PASSENGER DELAY")
                            }
                        }
                    }

                    if (showPublishDialog) {
                        PublishDelayDialog(
                            currentPassengerUpdate = currentPassengerUpdate,
                            newPrediction = newPrediction,
                            currentReason = activeAction?.reason ?: "Speed restriction",
                            onPublish = { delay, reason ->
                                viewModel.updatePassengerDelay(delay, reason)
                                showPublishDialog = false
                            },
                            onDismiss = { showPublishDialog = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ETA Updates Section (Admin Only - Passengers see current info in Hero Card)
                if (isAdmin && tracking.etaUpdates.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ETA UPDATES HISTORY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            tracking.etaUpdates.take(5).forEachIndexed { index, update ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${update.oldTimeFormatted} → ${update.newTimeFormatted} (${if (update.delayDeltaMinutes >= 0) "+${update.delayDeltaMinutes}" else update.delayDeltaMinutes} min)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = " · ${update.timeAgo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = update.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Timeline Header
                Text(
                    text = "JOURNEY TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timeline List
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(tracking.routeStops) { index, stop ->
                        TimelineRow(
                            stop = stop,
                            isFirst = index == 0,
                            isLast = index == tracking.routeStops.lastIndex,
                            defaultDelayColor = delayColor
                        )
                    }
                }

                if (!isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.toggleSaveTrain() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (isSaved) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSaved) "SAVED TO YOUR JOURNEYS" else "SAVE TRAIN")
                    }
                }
            }
        }
    }

    if (showGuestPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGuestPrompt() },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sign In Required", fontWeight = FontWeight.Bold) },
            text = {
                Text("Sign in as a passenger to save your favorite trains and receive personalized updates.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissGuestPrompt() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun TimelineRow(
    stop: RouteStationStop,
    isFirst: Boolean,
    isLast: Boolean,
    defaultDelayColor: Color
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Continuous Line & Node Column
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Vertical Line
            val lineColor = when (stop.status) {
                StopStatus.COMPLETED -> StatusSafe
                StopStatus.CURRENT -> MaterialTheme.colorScheme.primary
                StopStatus.UPCOMING -> MaterialTheme.colorScheme.outlineVariant
            }

            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isFirst) Color.Transparent else lineColor)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isLast) Color.Transparent else lineColor)
                )
            }

            // Node Icon
            when (stop.status) {
                StopStatus.COMPLETED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSafe,
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
                StopStatus.CURRENT -> {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(defaultDelayColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRailway,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                StopStatus.UPCOMING -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {}
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Station Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (stop.status) {
                    StopStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = BorderStroke(
                1.dp,
                if (stop.status == StopStatus.CURRENT) defaultDelayColor else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${stop.stationName} (${stop.stationCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (stop.status == StopStatus.CURRENT) FontWeight.ExtraBold else FontWeight.SemiBold
                    )

                    Text(
                        text = "Sch: ${timeFormat.format(Date(stop.scheduledTime))} · ${if (stop.platform.startsWith("Platform", ignoreCase = true)) stop.platform else "Platform ${stop.platform}"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expected Time & Range
                Column(horizontalAlignment = Alignment.End) {
                    val expStr = timeFormat.format(Date(stop.expectedTime))

                    val displayStr = when (stop.status) {
                        StopStatus.COMPLETED -> expStr
                        StopStatus.CURRENT -> expStr
                        StopStatus.UPCOMING -> if (stop.confidenceMarginMinutes > 0) "$expStr ±${stop.confidenceMarginMinutes} min" else expStr
                    }

                    val timeColor = if (stop.delayMinutes > 0) defaultDelayColor else StatusSafe

                    Text(
                        text = displayStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = timeColor
                    )

                    if (stop.delayMinutes > 0) {
                        Text(
                            text = "+${stop.delayMinutes} min late",
                            style = MaterialTheme.typography.labelSmall,
                            color = timeColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishDelayDialog(
    currentPassengerUpdate: Int,
    newPrediction: Int,
    currentReason: String,
    onPublish: (newDelay: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var delayInput by remember { mutableStateOf(newPrediction.toString()) }
    var selectedReason by remember { mutableStateOf(currentReason) }
    var customReasonInput by remember { mutableStateOf("") }
    var reasonExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Publish Passenger Delay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current passenger update",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+$currentPassengerUpdate min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "New prediction",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+$newPrediction min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusHighThreat
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = delayInput,
                    onValueChange = { delayInput = it.filter { char -> char.isDigit() } },
                    label = { Text("PASSENGER-FACING DELAY (MIN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = !reasonExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DELAY REASON") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        PREDEFINED_DELAY_REASONS.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReason = reason
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = customReasonInput,
                        onValueChange = { customReasonInput = it },
                        label = { Text("SPECIFY REASON") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            val delayVal = delayInput.toIntOrNull() ?: newPrediction
            val finalReason = if (selectedReason == "Other") customReasonInput.ifBlank { "Operational issue" } else selectedReason
            Button(
                onClick = { onPublish(delayVal, finalReason) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PUBLISH +$delayVal MIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
