package com.example.railapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.railapp.data.ScanEvent
import com.example.railapp.data.ThreatLevel
import com.example.railapp.ui.components.EmptyStateView
import com.example.railapp.ui.components.SyncBadge
import com.example.railapp.ui.components.ThreatStatusBadge
import com.example.railapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventLogScreen(viewModel: EventLogViewModel) {
    val events by viewModel.events.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var selectedEventForDetail by remember { mutableStateOf<ScanEvent?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == EventFilter.ALL,
                onClick = { viewModel.setFilter(EventFilter.ALL) },
                label = { Text("All Logs") }
            )
            FilterChip(
                selected = selectedFilter == EventFilter.THREATS,
                onClick = { viewModel.setFilter(EventFilter.THREATS) },
                label = { Text("Threats Only") },
                leadingIcon = {
                    if (selectedFilter == EventFilter.THREATS) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )
            FilterChip(
                selected = selectedFilter == EventFilter.UNSYNCED,
                onClick = { viewModel.setFilter(EventFilter.UNSYNCED) },
                label = { Text("Pending Sync") }
            )
        }

        if (events.isEmpty()) {
            EmptyStateView(
                title = "NO LOGGED EVENTS",
                description = when (selectedFilter) {
                    EventFilter.THREATS -> "No threat records found in local logs."
                    EventFilter.UNSYNCED -> "All event records are fully synced with cloud."
                    else -> "No scan events recorded yet. Perform a scan to log events."
                },
                icon = Icons.AutoMirrored.Filled.ListAlt,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(events) { event ->
                    EventLogItem(event, onClick = { selectedEventForDetail = event })
                }
            }
        }
    }

    selectedEventForDetail?.let { event ->
        EventDetailDialog(event = event, onDismiss = { selectedEventForDetail = null })
    }
}

@Composable
fun EventLogItem(event: ScanEvent, onClick: () -> Unit) {
    val isThreat = event.threatLevel != ThreatLevel.NONE
    val dark = isSystemInDarkTheme()

    val cardBg = if (isThreat) {
        when (event.threatLevel) {
            ThreatLevel.HIGH -> if (dark) StatusHighThreatDarkContainer.copy(alpha = 0.5f) else StatusHighThreatContainer
            ThreatLevel.MEDIUM -> if (dark) StatusMediumThreatDarkContainer.copy(alpha = 0.5f) else StatusMediumThreatContainer
            else -> if (dark) StatusLowThreatDarkContainer.copy(alpha = 0.5f) else StatusLowThreatContainer
        }
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThreatStatusBadge(threatLevel = event.threatLevel)
                SyncBadge(isSynced = event.synced)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.substance,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isThreat) {
                    Text(
                        text = "${event.confidence}% Conf.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.locationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val dateFormat = SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EventDetailDialog(event: ScanEvent, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Incident Report Details", style = MaterialTheme.typography.titleLarge)
                ThreatStatusBadge(threatLevel = event.threatLevel)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Event Info
                DetailSectionHeader("EVENT ANALYSIS")
                DetailRow(icon = Icons.Default.Science, label = "Substance", value = event.substance)
                DetailRow(icon = Icons.Default.Category, label = "Category", value = event.category.name)
                DetailRow(icon = Icons.Default.Speed, label = "Confidence", value = "${event.confidence}%")

                HorizontalDivider()

                // Section 2: Location
                DetailSectionHeader("GEOLOCATION DATA")
                DetailRow(icon = Icons.Default.Place, label = "Location Point", value = event.locationLabel)
                DetailRow(
                    icon = Icons.Default.GpsFixed,
                    label = "Coordinates",
                    value = if (event.latitude != null && event.longitude != null) {
                        "${String.format(Locale.getDefault(), "%.4f", event.latitude)}, ${String.format(Locale.getDefault(), "%.4f", event.longitude)}"
                    } else "N/A (GPS Unavailable)"
                )

                HorizontalDivider()

                // Section 3: Device & System Metadata
                DetailSectionHeader("DEVICE METADATA")
                DetailRow(icon = Icons.Default.Smartphone, label = "Device ID", value = event.deviceId)
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Timestamp",
                    value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
                )
                DetailRow(
                    icon = Icons.Default.CloudSync,
                    label = "Sync Status",
                    value = if (event.synced) "Synced to Supabase" else "Stored Locally (Pending)"
                )
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
fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
