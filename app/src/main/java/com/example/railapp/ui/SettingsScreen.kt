package com.example.railapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.railapp.auth.UserRole
import com.example.railapp.di.AppContainer
import com.example.railapp.ui.components.SectionHeader
import com.example.railapp.ui.theme.StatusHighThreat

@Composable
fun SettingsScreen(appContainer: AppContainer? = null) {
    val sessionManager = appContainer?.sessionManager
    val currentUser by sessionManager?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentRole by sessionManager?.currentRole?.collectAsState() ?: remember { mutableStateOf(null) }

    var selectedLanguage by remember { mutableStateOf("English") }
    var soundEnabled by remember { mutableStateOf(true) }
    var demoTrainData by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Section 1: Account Info
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("ACCOUNT INFORMATION")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.email ?: "Guest User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Role: ${currentRole?.name ?: "GUEST"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Preferences
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("PREFERENCES")
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Audio Alerts", style = MaterialTheme.typography.titleMedium)
                            Text("Play sound tone for delay updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
            }
        }

        // Section 3: Network Simulation
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("SYSTEM & NETWORK SIMULATION")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Simulate Live Delay Updates", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Simulate realtime delay changes for network verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = demoTrainData,
                        onCheckedChange = { demoTrainData = it }
                    )
                }
            }
        }

        // Section 4: Account / Session Actions (Logout / Exit Guest Mode)
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(if (currentRole == UserRole.GUEST) "GUEST SESSION" else "ACCOUNT ACTIONS")
                Spacer(modifier = Modifier.height(8.dp))

                if (currentRole == UserRole.GUEST) {
                    Text(
                        text = "Sign in to save favorite trains, customize preferred stations, and receive personal delay updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { sessionManager?.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXIT GUEST MODE / SIGN IN", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { sessionManager?.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusHighThreat)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOG OUT", fontWeight = FontWeight.Bold)
                    }
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
