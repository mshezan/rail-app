package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.auth.SessionManager
import com.example.railapp.auth.UserRole
import com.example.railapp.data.train.PNRRepository
import com.example.railapp.data.train.SavedTrainDao
import com.example.railapp.data.train.SavedTrainEntity
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class SearchMode {
    PNR, TRAIN_NUMBER
}

data class RecentSearchItem(
    val trainId: String,
    val stationId: String,
    val trainNumber: String,
    val trainName: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class PassengerHomeViewModel(
    private val trainRepository: TrainStatusRepository,
    private val pnrRepository: PNRRepository,
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

    private val _searchMode = MutableStateFlow(SearchMode.PNR)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pnrError = MutableStateFlow<String?>(null)
    val pnrError: StateFlow<String?> = _pnrError.asStateFlow()

    private val _trainError = MutableStateFlow<String?>(null)
    val trainError: StateFlow<String?> = _trainError.asStateFlow()

    private val _pnrResult = MutableStateFlow<PNRJourney?>(null)
    val pnrResult: StateFlow<PNRJourney?> = _pnrResult.asStateFlow()

    private val _trainNumberResult = MutableStateFlow<StationTrainOperationalInfo?>(null)
    val trainNumberResult: StateFlow<StationTrainOperationalInfo?> = _trainNumberResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<RecentSearchItem>>(
        listOf(
            RecentSearchItem("trn_12675", "stn_mas", "12675", "Kovai Express"),
            RecentSearchItem("trn_12640", "stn_mas", "12640", "Brindavan Express"),
            RecentSearchItem("trn_12007", "stn_mas", "12007", "MYS Shatabdi Express")
        )
    )
    val recentSearches: StateFlow<List<RecentSearchItem>> = _recentSearches.asStateFlow()

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
        .map { list -> list.map { it.trainId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        _pnrError.value = null
        _trainError.value = null
        _pnrResult.value = null
        _trainNumberResult.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _pnrError.value = null
        _trainError.value = null
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        _pnrError.value = null
        _trainError.value = null

        if (query.isBlank()) {
            if (_searchMode.value == SearchMode.PNR) {
                _pnrError.value = "Please enter 10-digit PNR"
            } else {
                _trainError.value = "Please enter train number or name"
            }
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            delay(300)

            if (_searchMode.value == SearchMode.PNR) {
                if (query.length != 10 || !query.all { it.isDigit() }) {
                    _pnrError.value = "PNR must be exactly 10 digits"
                    _pnrResult.value = null
                    _trainNumberResult.value = null
                } else {
                    val result = pnrRepository.lookupPNR(query)
                    if (result == null) {
                        _pnrError.value = "PNR not found. Please check the number and try again."
                        _pnrResult.value = null
                    } else {
                        _pnrResult.value = result
                        _pnrError.value = null
                        _trainNumberResult.value = null
                        addRecentSearch(result.trainId, "stn_mas", result.trainNumber, result.trainName)
                    }
                }
            } else {
                _pnrResult.value = null
                val allOp = trainRepository.operationalInfo.value
                val match = allOp.find {
                    it.train.trainNumber.equals(query, ignoreCase = true) ||
                    it.train.trainName.contains(query, ignoreCase = true) ||
                    it.train.trainNumber.contains(query, ignoreCase = true)
                }

                if (match == null) {
                    _trainError.value = "Train not found. Try another train number."
                    _trainNumberResult.value = null
                } else {
                    _trainNumberResult.value = match
                    _trainError.value = null
                    addRecentSearch(match.train.id, match.schedule.stationId, match.train.trainNumber, match.train.trainName)
                }
            }
            _isSearching.value = false
        }
    }

    private fun addRecentSearch(trainId: String, stationId: String, trainNumber: String, trainName: String) {
        val newItem = RecentSearchItem(trainId, stationId, trainNumber, trainName)
        val currentList = _recentSearches.value.filterNot { it.trainNumber == trainNumber }
        _recentSearches.value = listOf(newItem) + currentList.take(4)
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
        val isSaved = savedTrainKeys.value.contains(info.train.id)

        viewModelScope.launch {
            if (isSaved) {
                savedTrainDao.deleteSavedTrain(user.id, info.train.id)
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

    fun savePNRJourney(journey: PNRJourney) {
        val role = currentRole.value
        if (role == UserRole.GUEST || role == null) {
            _showGuestPrompt.value = true
            return
        }

        val user = currentUser.value ?: return

        viewModelScope.launch {
            savedTrainDao.saveTrain(
                SavedTrainEntity(
                    userId = user.id,
                    trainId = journey.trainId,
                    stationId = "stn_mas",
                    trainNumber = journey.trainNumber,
                    trainName = journey.trainName,
                    origin = journey.sourceStation,
                    destination = journey.destinationStation
                )
            )
        }
    }

    fun dismissGuestPrompt() {
        _showGuestPrompt.value = false
    }

    companion object {
        fun provideFactory(
            trainRepository: TrainStatusRepository,
            pnrRepository: PNRRepository,
            savedTrainDao: SavedTrainDao,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PassengerHomeViewModel::class.java)) {
                    return PassengerHomeViewModel(trainRepository, pnrRepository, savedTrainDao, sessionManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
