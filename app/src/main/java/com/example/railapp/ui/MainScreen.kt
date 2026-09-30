package com.example.railapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.railapp.auth.UserRole
import com.example.railapp.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(appContainer: AppContainer) {
    val sessionManager = appContainer.sessionManager
    val currentRole by sessionManager.currentRole.collectAsState()

    if (currentRole == null) {
        LoginScreen(
            sessionManager = sessionManager,
            onLoginSuccess = { }
        )
    } else {
        val navController = rememberNavController()

        val scanViewModel: ScanViewModel = viewModel(
            factory = ScanViewModel.provideFactory(
                appContainer.detectionSensor,
                appContainer.scanRepository
            )
        )

        val eventLogViewModel: EventLogViewModel = viewModel(
            factory = EventLogViewModel.provideFactory(
                appContainer.scanRepository
            )
        )

        val controlRoomViewModel: ControlRoomViewModel = viewModel(
            factory = ControlRoomViewModel.provideFactory(
                appContainer.trainStatusRepository
            )
        )

        val passengerDelaysViewModel: PassengerDelaysViewModel = viewModel(
            factory = PassengerDelaysViewModel.provideFactory(
                appContainer.trainStatusRepository
            )
        )

        val passengerHomeViewModel: PassengerHomeViewModel = viewModel(
            factory = PassengerHomeViewModel.provideFactory(
                appContainer.trainStatusRepository,
                appContainer.database.savedTrainDao(),
                appContainer.sessionManager
            )
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (currentRole) {
                                    UserRole.ADMIN -> "INDIAN RAILWAYS CONTROL"
                                    UserRole.PASSENGER -> "PASSENGER RAIL PORTAL"
                                    UserRole.GUEST -> "INDIAN RAILWAYS GUEST"
                                    null -> "INDIAN RAILWAYS"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (currentRole) {
                                    UserRole.ADMIN -> "Station Operations & Delay Prediction Terminal"
                                    UserRole.PASSENGER -> "Live Station Departures & Schedules"
                                    UserRole.GUEST -> "Station Schedule Viewer"
                                    null -> "Rail Portal"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { sessionManager.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    if (currentRole == UserRole.ADMIN) {
                        // Admin Navigation 3 Primary Operational Tabs (Control Room, Passenger Delays, Security)
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Monitor, contentDescription = "Control Room") },
                            label = { Text("Control Room") },
                            selected = currentDestination?.hierarchy?.any { it.route == "control_room" } == true,
                            onClick = {
                                navController.navigate("control_room") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Campaign, contentDescription = "Passenger Delays") },
                            label = { Text("Passenger Delays") },
                            selected = currentDestination?.hierarchy?.any { it.route == "passenger_delays" } == true,
                            onClick = {
                                navController.navigate("passenger_delays") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Shield, contentDescription = "Security") },
                            label = { Text("Security") },
                            selected = currentDestination?.hierarchy?.any { it.route == "security" } == true,
                            onClick = {
                                navController.navigate("security") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    } else {
                        // Passenger / Guest Navigation Tabs
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DirectionsRailway, contentDescription = "Home") },
                            label = { Text("Live Trains") },
                            selected = currentDestination?.hierarchy?.any { it.route == "passenger_home" } == true,
                            onClick = {
                                navController.navigate("passenger_home") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (currentRole == UserRole.ADMIN) "control_room" else "passenger_home",
                modifier = Modifier.padding(innerPadding)
            ) {
                if (currentRole == UserRole.ADMIN) {
                    composable("control_room") { ControlRoomScreen(controlRoomViewModel, passengerDelaysViewModel) }
                    composable("passenger_delays") { PassengerDelaysScreen(passengerDelaysViewModel) }
                    composable("security") { SecurityScreen(scanViewModel, eventLogViewModel) }
                } else {
                    composable("passenger_home") { PassengerHomeScreen(passengerHomeViewModel) }
                }
                composable("settings") { SettingsScreen(appContainer) }
            }
        }
    }
}
