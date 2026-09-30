package com.example.railapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.railapp.di.AppContainer
import com.example.railapp.ui.components.SectionHeader

@Composable
fun SettingsScreen(appContainer: AppContainer? = null) {
    var selectedLanguage by remember { mutableStateOf("English") }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var deviceId by remember { mutableStateOf("RPF_DEVICE_01") }
    var demoMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "System Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Section 1: General
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("GENERAL")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Interface Language", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedLanguage == "English",
                        onClick = { selectedLanguage = "English" }
                    )
                    Text("English", modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.width(24.dp))
                    RadioButton(
                        selected = selectedLanguage == "Hindi",
                        onClick = { selectedLanguage = "Hindi" }
                    )
                    Text("Hindi (हिंदी)", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // Section 2: Alerts
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("THREAT ALERTS")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Audio Sound Alert", style = MaterialTheme.typography.titleMedium)
                            Text("Play alert tone upon threat detection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Vibration Alert", style = MaterialTheme.typography.titleMedium)
                            Text("Haptic feedback on threat detection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                }
            }
        }

        // Section 3: Device Configuration
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("DEVICE IDENTIFICATION")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Assigned Device ID") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Section 4: Demo / Development Mode
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("DEMO & FIELD TESTING")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Force Threat on Next Scan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Simulate a threat result to verify alert workflow",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = demoMode,
                        onCheckedChange = {
                            demoMode = it
                            appContainer?.detectionSensor?.forceThreat = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}
