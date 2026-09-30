package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.railapp.auth.UserRole
import com.example.railapp.domain.train.StationTrainOperationalInfo
import com.example.railapp.domain.train.TrainStatusType
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(viewModel: PassengerHomeViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val relevantTrains by viewModel.relevantTrains.collectAsState()
    val savedTrainsWithStatus by viewModel.savedTrainsWithStatus.collectAsState()
    val savedTrainKeys by viewModel.savedTrainKeys.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()
    val showGuestPrompt by viewModel.showGuestPrompt.collectAsState()

    var stationPickerExpanded by remember { mutableStateOf(false) }
    var selectedTrainForDetail by remember { mutableStateOf<StationTrainOperationalInfo?>(null) }

    val isPassenger = currentRole == UserRole.PASSENGER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Personalized Header / Guest Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPassenger) "GOOD DAY, ${currentUser?.email?.substringBefore('@')?.uppercase() ?: "PASSENGER"}" else "GUEST MODE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isPassenger) "Personalized Railway Services" else "Temporary Live Station Schedule Viewer",
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
                        imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isOffline) StatusMediumThreat else StatusSafe,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "OFFLINE" else "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOffline) StatusMediumThreat else StatusSafe,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Saved Trains Carousel (For Authenticated Passengers)
        if (isPassenger && savedTrainsWithStatus.isNotEmpty()) {
            Text(
                text = "YOUR SAVED TRAINS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(savedTrainsWithStatus) { info ->
                    SavedTrainCard(
                        info = info,
                        onClick = { selectedTrainForDetail = info },
                        onUnsave = { viewModel.toggleSaveTrain(info) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Station Selector Dropdown
        ExposedDropdownMenuBox(
            expanded = stationPickerExpanded,
            onExpandedChange = { stationPickerExpanded = !stationPickerExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedStation?.let { "${it.name} (${it.code})" } ?: "Select Station",
                onValueChange = {},
                readOnly = true,
                label = { Text("SELECT STATION") },
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
                            viewModel.selectStation(station.id)
                            stationPickerExpanded = false
                        }
                    )
                }
            }
        }

        if (isOffline) {
            Spacer(modifier = Modifier.height(4.dp))
            val timeAgoMin = ((System.currentTimeMillis() - lastUpdatedTime) / 60000).coerceAtLeast(0)
            Text(
                text = "Cached Data • Last updated $timeAgoMin min ago",
                color = StatusMediumThreat,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "UPCOMING TRAINS AT ${selectedStation?.code ?: "STATION"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Relevant Train List
        if (relevantTrains.isEmpty()) {
            EmptyStateView(
                title = "NO TRAINS SCHEDULED",
                description = "No active trains arriving or departing at this time for the selected station.",
                icon = Icons.Default.DirectionsRailway,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(relevantTrains) { info ->
                    val key = "${info.train.id}_${info.schedule.stationId}"
                    val isSaved = savedTrainKeys.contains(key)

                    PassengerTrainCard(
                        info = info,
                        isSaved = isSaved,
                        onToggleSave = { viewModel.toggleSaveTrain(info) },
                        onClick = { selectedTrainForDetail = info }
                    )
                }
            }
        }
    }

    // Guest Registration Prompt Dialog
    if (showGuestPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGuestPrompt() },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sign In Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Create an account or sign in as a passenger to save your favorite trains and receive personalized delay alerts!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissGuestPrompt() }) {
                    Text("OK, GOT IT")
                }
            }
        )
    }

    selectedTrainForDetail?.let { info ->
        val key = "${info.train.id}_${info.schedule.stationId}"
        val isSaved = savedTrainKeys.contains(key)

        PassengerTrainDetailDialog(
            info = info,
            isSaved = isSaved,
            onToggleSave = { viewModel.toggleSaveTrain(info) },
            onDismiss = { selectedTrainForDetail = null }
        )
    }
}

@Composable
fun SavedTrainCard(
    info: StationTrainOperationalInfo,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    val train = info.train
    val status = info.status
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val statusColor = when (status.status) {
        TrainStatusType.ON_TIME -> StatusSafe
        TrainStatusType.DELAYED -> StatusHighThreat
        TrainStatusType.ARRIVING -> StatusInfoCloud
        TrainStatusType.BOARDING -> StatusMediumThreat
        TrainStatusType.DEPARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        TrainStatusType.CANCELLED -> Color.Gray
    }

    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = train.trainNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onUnsave, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Unsave",
                        tint = StatusHighThreat,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = train.trainName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = "${train.origin} ➔ ${train.destination}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expected ${timeFormat.format(Date(status.predictedArrival))}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (status.arrivalDelayMinutes > 0) "+${status.arrivalDelayMinutes}m Delay" else "On Time",
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PassengerTrainCard(
    info: StationTrainOperationalInfo,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    val train = info.train
    val schedule = info.schedule
    val status = info.status
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val statusColor = when (status.status) {
        TrainStatusType.ON_TIME -> StatusSafe
        TrainStatusType.DELAYED -> StatusHighThreat
        TrainStatusType.ARRIVING -> StatusInfoCloud
        TrainStatusType.BOARDING -> StatusMediumThreat
        TrainStatusType.DEPARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        TrainStatusType.CANCELLED -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Train Number, Name, Heart Favorite Toggle & Delay Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save Train",
                            tint = if (isSaved) StatusHighThreat else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (status.arrivalDelayMinutes > 0) "+${status.arrivalDelayMinutes} MIN DELAY" else status.status.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Route Origin -> Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${train.origin} ➔ ${train.destination}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Timings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Scheduled Arrival", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeFormat.format(Date(schedule.scheduledArrival)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Expected Arrival", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeFormat.format(Date(status.predictedArrival)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.arrivalDelayMinutes > 0) StatusHighThreat else StatusSafe
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Platform", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = schedule.platform,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PassengerTrainDetailDialog(
    info: StationTrainOperationalInfo,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val train = info.train
    val schedule = info.schedule
    val status = info.status
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${train.trainNumber} - ${train.trainName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${train.origin} ➔ ${train.destination}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Save Train",
                        tint = if (isSaved) StatusHighThreat else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PassengerDetailRow(label = "Platform", value = schedule.platform)
                PassengerDetailRow(
                    label = "Scheduled Arrival",
                    value = timeFormat.format(Date(schedule.scheduledArrival))
                )
                PassengerDetailRow(
                    label = "Expected Arrival",
                    value = timeFormat.format(Date(status.predictedArrival))
                )
                PassengerDetailRow(
                    label = "Arrival Delay",
                    value = if (status.arrivalDelayMinutes > 0) "+${status.arrivalDelayMinutes} minutes" else "On Time"
                )
                PassengerDetailRow(
                    label = "Scheduled Departure",
                    value = timeFormat.format(Date(schedule.scheduledDeparture))
                )
                PassengerDetailRow(
                    label = "Expected Departure",
                    value = timeFormat.format(Date(status.predictedDeparture))
                )
                PassengerDetailRow(label = "Current Status", value = status.status.name)
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
fun PassengerDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
