package com.example.railapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                appContainer.pnrRepository,
                appContainer.database.savedTrainDao(),
                appContainer.sessionManager
            )
        )

        val savedTrainsViewModel: SavedTrainsViewModel = viewModel(
            factory = SavedTrainsViewModel.provideFactory(
                appContainer.trainStatusRepository,
                appContainer.database.savedTrainDao(),
                appContainer.sessionManager
            )
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        val isTrackingScreen = currentRoute?.startsWith("train_tracking") == true

        Scaffold(
            topBar = {
                if (!isTrackingScreen) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (currentRole) {
                                        UserRole.ADMIN -> "Indian Railways Control"
                                        UserRole.PASSENGER -> "Indian Railways"
                                        UserRole.GUEST -> "Indian Railways"
                                        null -> "Indian Railways"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { sessionManager.logout() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = if (currentRole == UserRole.GUEST) "Exit Guest Mode / Sign In" else "Log Out"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                if (!isTrackingScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        if (currentRole == UserRole.ADMIN) {
                            // Admin Navigation
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
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        } else if (currentRole == UserRole.PASSENGER) {
                            // Authenticated Passenger Navigation
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DirectionsRailway, contentDescription = "Home") },
                                label = { Text("Home") },
                                selected = currentDestination?.hierarchy?.any { it.route == "passenger_home" } == true,
                                onClick = {
                                    navController.navigate("passenger_home") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved Trains") },
                                label = { Text("Saved Trains") },
                                selected = currentDestination?.hierarchy?.any { it.route == "saved_trains" } == true,
                                onClick = {
                                    navController.navigate("saved_trains") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        } else {
                            // Guest Navigation
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DirectionsRailway, contentDescription = "Trains") },
                                label = { Text("Trains") },
                                selected = currentDestination?.hierarchy?.any { it.route == "passenger_home" } == true,
                                onClick = {
                                    navController.navigate("passenger_home") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
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
                    composable("control_room") {
                        ControlRoomScreen(
                            viewModel = controlRoomViewModel,
                            passengerDelaysViewModel = passengerDelaysViewModel,
                            onSelectTrainForTracking = { trainId, stationId ->
                                navController.navigate("train_tracking/$trainId/$stationId")
                            }
                        )
                    }
                    composable("passenger_delays") { PassengerDelaysScreen(passengerDelaysViewModel) }
                } else {
                    composable("passenger_home") {
                        PassengerHomeScreen(
                            viewModel = passengerHomeViewModel,
                            onSelectTrainForTracking = { trainId, stationId ->
                                navController.navigate("train_tracking/$trainId/$stationId")
                            }
                        )
                    }
                    composable("saved_trains") {
                        SavedTrainsScreen(
                            viewModel = savedTrainsViewModel,
                            onSelectTrainForTracking = { trainId, stationId ->
                                navController.navigate("train_tracking/$trainId/$stationId")
                            },
                            onNavigateToHome = {
                                navController.navigate("passenger_home") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
                composable("settings") { SettingsScreen(appContainer) }
                composable(
                    route = "train_tracking/{trainId}/{stationId}",
                    arguments = listOf(
                        navArgument("trainId") { type = NavType.StringType },
                        navArgument("stationId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val trainId = backStackEntry.arguments?.getString("trainId") ?: ""
                    val stationId = backStackEntry.arguments?.getString("stationId") ?: ""

                    val trackingViewModel: TrainTrackingViewModel = viewModel(
                        factory = TrainTrackingViewModel.provideFactory(
                            appContainer.trainTrackingRepository,
                            appContainer.trainStatusRepository,
                            appContainer.database.savedTrainDao(),
                            appContainer.sessionManager,
                            trainId,
                            stationId
                        )
                    )

                    TrainTrackingScreen(
                        viewModel = trackingViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
