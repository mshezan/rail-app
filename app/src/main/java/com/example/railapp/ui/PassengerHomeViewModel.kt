package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.auth.SessionManager
import com.example.railapp.auth.UserRole
import com.example.railapp.data.train.SavedTrainDao
import com.example.railapp.data.train.SavedTrainEntity
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PassengerHomeViewModel(
    private val trainRepository: TrainStatusRepository,
    private val savedTrainDao: SavedTrainDao,
    private val sessionManager: SessionManager,
    private val clockProvider: ClockProvider = SystemClockProvider()
) : ViewModel() {

    val currentUser = sessionManager.currentUser
    val currentRole = sessionManager.currentRole

    val stations: StateFlow<List<Station>> = trainRepository.stations
    val selectedStation: StateFlow<Station?> = trainRepository.selectedStation
    val isOffline: StateFlow<Boolean> = trainRepository.isOffline
    val lastUpdatedTime: StateFlow<Long> = trainRepository.lastUpdatedTime

    private val _showGuestPrompt = MutableStateFlow(false)
    val showGuestPrompt: StateFlow<Boolean> = _showGuestPrompt.asStateFlow()

    // Saved train entities for current user
    val savedTrainEntities: StateFlow<List<SavedTrainEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.PASSENGER) {
                savedTrainDao.getSavedTrainsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTrainKeys: StateFlow<Set<String>> = savedTrainEntities
        .map { list -> list.map { "${it.trainId}_${it.stationId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Saved trains joined with live operational info
    val savedTrainsWithStatus: StateFlow<List<StationTrainOperationalInfo>> = combine(
        savedTrainEntities,
        trainRepository.operationalInfo
    ) { entities, liveInfo ->
        entities.mapNotNull { entity ->
            liveInfo.find { it.train.id == entity.trainId && it.schedule.stationId == entity.stationId }
                ?: StationTrainOperationalInfo(
                    train = Train(entity.trainId, entity.trainNumber, entity.trainName, entity.origin, entity.destination),
                    schedule = TrainStationSchedule(entity.trainId, entity.stationId, System.currentTimeMillis(), System.currentTimeMillis(), "Platform 1"),
                    status = TrainStatus(entity.trainId, entity.stationId, System.currentTimeMillis(), System.currentTimeMillis(), 0, 0, TrainStatusType.ON_TIME, 85, System.currentTimeMillis(), DataSourceType.MOCK),
                    prediction = DelayPrediction(entity.trainId, entity.stationId, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), 85, PredictionTrend.STABLE, DelayRiskSeverity.LOW)
                )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relevantTrains: StateFlow<List<StationTrainOperationalInfo>> = combine(
        trainRepository.operationalInfo,
        trainRepository.selectedStation
    ) { operationalList, currentStation ->
        if (currentStation == null) return@combine emptyList()

        val now = clockProvider.currentTimeMillis()
        val pastCutoff = now - TimeUnit.MINUTES.toMillis(30)
        val futureCutoff = now + TimeUnit.HOURS.toMillis(6)

        operationalList.filter { info ->
            val predictedDep = info.status.predictedDeparture
            val predictedArr = info.status.predictedArrival
            (predictedDep >= pastCutoff && predictedArr <= futureCutoff) || (predictedArr in pastCutoff..futureCutoff)
        }.sortedBy { it.status.predictedArrival }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            trainRepository.initialize()
        }
    }

    fun selectStation(stationId: String, isDemoMode: Boolean = false) {
        viewModelScope.launch {
            trainRepository.selectStation(stationId, isDemoMode)
        }
    }

    fun toggleSaveTrain(info: StationTrainOperationalInfo) {
        val role = currentRole.value
        if (role == UserRole.GUEST || role == null) {
            _showGuestPrompt.value = true
            return
        }

        val user = currentUser.value ?: return
        val key = "${info.train.id}_${info.schedule.stationId}"
        val isSaved = savedTrainKeys.value.contains(key)

        viewModelScope.launch {
            if (isSaved) {
                savedTrainDao.deleteSavedTrain(user.id, info.train.id, info.schedule.stationId)
            } else {
                savedTrainDao.saveTrain(
                    SavedTrainEntity(
                        userId = user.id,
                        trainId = info.train.id,
                        stationId = info.schedule.stationId,
                        trainNumber = info.train.trainNumber,
                        trainName = info.train.trainName,
                        origin = info.train.origin,
                        destination = info.train.destination
                    )
                )
            }
        }
    }

    fun dismissGuestPrompt() {
        _showGuestPrompt.value = false
    }

    companion object {
        fun provideFactory(
            trainRepository: TrainStatusRepository,
            savedTrainDao: SavedTrainDao,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PassengerHomeViewModel::class.java)) {
                    return PassengerHomeViewModel(trainRepository, savedTrainDao, sessionManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
