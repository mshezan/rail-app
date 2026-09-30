package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railapp.auth.UserRole
import com.example.railapp.domain.train.StationTrainOperationalInfo
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(
    viewModel: PassengerHomeViewModel,
    onSelectTrainForTracking: (trainId: String, stationId: String) -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val relevantTrains by viewModel.relevantTrains.collectAsState()
    val savedTrainKeys by viewModel.savedTrainKeys.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()
    val showGuestPrompt by viewModel.showGuestPrompt.collectAsState()

    val searchMode by viewModel.searchMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pnrResult by viewModel.pnrResult.collectAsState()
    val trainNumberResult by viewModel.trainNumberResult.collectAsState()
    val pnrError by viewModel.pnrError.collectAsState()
    val trainError by viewModel.trainError.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    var stationPickerExpanded by remember { mutableStateOf(false) }

    val isPassenger = currentRole == UserRole.PASSENGER

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
            Text(
                text = if (isPassenger) "Your Journey" else "Trains",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
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

        Spacer(modifier = Modifier.height(12.dp))

        // MY JOURNEY / SEARCH AREA
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MY JOURNEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Segmented control (PNR / Train Number) - Only show PNR tab for authenticated Passengers
                if (isPassenger) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = searchMode == SearchMode.PNR,
                            onClick = { viewModel.setSearchMode(SearchMode.PNR) },
                            label = { Text("PNR Lookup") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = searchMode == SearchMode.TRAIN_NUMBER,
                            onClick = { viewModel.setSearchMode(SearchMode.TRAIN_NUMBER) },
                            label = { Text("Train Number") },
                            leadingIcon = { Icon(Icons.Default.Train, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Input Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                if (isPassenger && searchMode == SearchMode.PNR) "Enter 10-digit PNR"
                                else "Enter Train number or name"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isPassenger && searchMode == SearchMode.PNR) Icons.Default.ConfirmationNumber else Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPassenger && searchMode == SearchMode.PNR) KeyboardType.Number else KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { viewModel.performSearch() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("SEARCH")
                    }
                }

                // Inline Error Messages
                pnrError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                trainError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Searching...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // PNR Search Result Card
                pnrResult?.let { pnr ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PNR ${pnr.pnr}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = StatusSafeContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = pnr.statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSafe,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${pnr.trainNumber} ${pnr.trainName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${pnr.sourceStation} (${pnr.sourceStationCode}) ➔ ${pnr.destinationStation} (${pnr.destinationStationCode})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Journey: ${pnr.journeyDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Status: Running +${pnr.currentDelayMinutes} min", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusHighThreat)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onSelectTrainForTracking(pnr.trainId, "stn_mas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("WHERE IS MY TRAIN?")
                    }
                }

                // Train Number Search Result Card
                trainNumberResult?.let { info ->
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("${info.train.trainNumber} ${info.train.trainName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${info.train.origin} ➔ ${info.train.destination}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ETA: ${timeFormat.format(Date(info.status.predictedArrival))}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Current: +${info.prediction.currentDelayMinutes} min", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusHighThreat)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onSelectTrainForTracking(info.train.id, info.schedule.stationId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TRACK TRAIN")
                    }
                }
            }
        }

        // RECENT SEARCHES SECTION
        if (recentSearches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Recent:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(recentSearches) { item ->
                        SuggestionChip(
                            onClick = { onSelectTrainForTracking(item.trainId, item.stationId) },
                            label = { Text("${item.trainNumber} ${item.trainName}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact Station Selector
        ExposedDropdownMenuBox(
            expanded = stationPickerExpanded,
            onExpandedChange = { stationPickerExpanded = !stationPickerExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedStation?.let { "${it.name} (${it.code})" } ?: "Select Station",
                onValueChange = {},
                readOnly = true,
                label = { Text("YOUR STATION") },
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

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Upcoming Trains",
            style = MaterialTheme.typography.titleMedium,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(relevantTrains) { info ->
                    val isSaved = savedTrainKeys.contains(info.train.id)

                    PassengerTrainCard(
                        info = info,
                        isSaved = isSaved,
                        onToggleSave = { viewModel.toggleSaveTrain(info) },
                        onClick = { onSelectTrainForTracking(info.train.id, info.schedule.stationId) }
                    )
                }
            }
        }
    }

    // Guest Prompt Modal
    if (showGuestPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGuestPrompt() },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sign In Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Sign in as a passenger to save your favorite trains and receive personalized updates.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
fun PassengerTrainCard(
    info: StationTrainOperationalInfo,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    val train = info.train
    val schedule = info.schedule
    val status = info.status
    val delayMinutes = info.activePassengerDelayMinutes ?: status.arrivalDelayMinutes
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val delayColor = when {
        delayMinutes > 15 -> StatusHighThreat
        delayMinutes > 5 -> StatusMediumThreat
        else -> StatusSafe
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Large ETA time in delay colour with small scheduled time struck through if delayed
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = timeFormat.format(Date(status.predictedArrival)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (delayMinutes > 0) delayColor else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = timeFormat.format(Date(schedule.scheduledArrival)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (delayMinutes > 0) TextDecoration.LineThrough else TextDecoration.None
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // MIDDLE: Train number, name, route
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = train.trainNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = train.trainName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT: Delay chip, platform & Heart toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = delayColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (delayMinutes > 0) "+$delayMinutes min" else "On time",
                            color = delayColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Platform ${schedule.platform}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onToggleSave, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Save Train",
                        tint = if (isSaved) StatusHighThreat else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
