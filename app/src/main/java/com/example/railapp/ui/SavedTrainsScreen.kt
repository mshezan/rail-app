package com.example.railapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.auth.SessionManager
import com.example.railapp.auth.UserRole
import com.example.railapp.data.train.SavedTrainDao
import com.example.railapp.data.train.SavedTrainEntity
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.*
import com.example.railapp.ui.theme.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedTrainsViewModel(
    trainRepository: TrainStatusRepository,
    private val savedTrainDao: SavedTrainDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUser = sessionManager.currentUser

    private val savedTrainEntities: Flow<List<SavedTrainEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.PASSENGER) {
                savedTrainDao.getSavedTrainsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }

    val savedTrainsWithStatus: StateFlow<List<StationTrainOperationalInfo>> = combine(
        savedTrainEntities,
        trainRepository.operationalInfo
    ) { entities, liveInfo ->
        entities.map { entity ->
            liveInfo.find { it.train.id == entity.trainId }
                ?: StationTrainOperationalInfo(
                    train = Train(entity.trainId, entity.trainNumber, entity.trainName, entity.origin, entity.destination),
                    schedule = TrainStationSchedule(entity.trainId, entity.stationId, System.currentTimeMillis() + 1800000, System.currentTimeMillis() + 1800000, "1"),
                    status = TrainStatus(entity.trainId, entity.stationId, System.currentTimeMillis() + 1800000, System.currentTimeMillis() + 1800000, 12, 12, TrainStatusType.DELAYED, 85, System.currentTimeMillis(), DataSourceType.MOCK),
                    prediction = DelayPrediction(entity.trainId, entity.stationId, 12, 12, System.currentTimeMillis() + 1800000, System.currentTimeMillis() + 1800000, 85, PredictionTrend.STABLE, DelayRiskSeverity.MODERATE)
                )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unsaveTrain(trainId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            savedTrainDao.deleteSavedTrain(user.id, trainId)
        }
    }

    companion object {
        fun provideFactory(
            trainRepository: TrainStatusRepository,
            savedTrainDao: SavedTrainDao,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SavedTrainsViewModel::class.java)) {
                    return SavedTrainsViewModel(trainRepository, savedTrainDao, sessionManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

@Composable
fun SavedTrainsScreen(
    viewModel: SavedTrainsViewModel,
    onSelectTrainForTracking: (trainId: String, stationId: String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val savedTrains by viewModel.savedTrainsWithStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SAVED TRAINS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Your saved trains",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (savedTrains.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No saved trains yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Save a train to quickly track its delay and ETA.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateToHome,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("VIEW LIVE TRAINS")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedTrains, key = { it.train.id }) { info ->
                    SavedTrainItemCard(
                        info = info,
                        onClick = { onSelectTrainForTracking(info.train.id, info.schedule.stationId) },
                        onUnsave = { viewModel.unsaveTrain(info.train.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedTrainItemCard(
    info: StationTrainOperationalInfo,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    val train = info.train
    val status = info.status
    val schedule = info.schedule
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onUnsave, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Unsave Train",
                        tint = StatusHighThreat,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Expected ${timeFormat.format(Date(status.predictedArrival))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = delayColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (delayMinutes > 0) "+$delayMinutes min" else "On time",
                            color = delayColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Platform ${schedule.platform}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
