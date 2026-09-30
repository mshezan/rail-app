package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.PredictionTrend
import com.example.railapp.domain.train.Station
import com.example.railapp.domain.train.StationTrainOperationalInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ActionPriority {
    HIGH, MEDIUM, LOW
}

data class PriorityActionItem(
    val id: String,
    val trainId: String,
    val stationId: String,
    val trainNumber: String,
    val trainName: String,
    val priority: ActionPriority,
    val currentDelayMinutes: Int,
    val predictedDelayMinutes: Int,
    val reasons: List<String>,
    val routeSubtitle: String? = null
)

class ControlRoomViewModel(
    private val trainRepository: TrainStatusRepository
) : ViewModel() {

    val stations: StateFlow<List<Station>> = trainRepository.stations
    val operatingStation: StateFlow<Station?> = trainRepository.selectedStation
    val isOffline: StateFlow<Boolean> = trainRepository.isOffline
    val lastUpdatedTime: StateFlow<Long> = trainRepository.lastUpdatedTime

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<StationTrainOperationalInfo?>(null)
    val searchResult: StateFlow<StationTrainOperationalInfo?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchError.value = null
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        _searchError.value = null

        if (query.isBlank()) {
            _searchError.value = "Please enter train number or name"
            _searchResult.value = null
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            delay(300)

            val allOp = trainRepository.operationalInfo.value
            val match = allOp.find {
                it.train.trainNumber.equals(query, ignoreCase = true) ||
                it.train.trainName.contains(query, ignoreCase = true) ||
                it.train.trainNumber.contains(query, ignoreCase = true)
            }

            if (match == null) {
                _searchError.value = "Train not found. Try another train number."
                _searchResult.value = null
            } else {
                _searchResult.value = match
                _searchError.value = null
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResult.value = null
        _searchError.value = null
    }

    val approachingTrains: StateFlow<List<StationTrainOperationalInfo>> = trainRepository.operationalInfo
        .map { list ->
            list.sortedByDescending { it.prediction.predictedDelayMinutes }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val priorityActions: StateFlow<List<PriorityActionItem>> = approachingTrains.map { list ->
        if (list.isEmpty()) {
            emptyList()
        } else {
            val items = list.map { info ->
                val train = info.train
                val pred = info.prediction
                val currentDelay = pred.currentDelayMinutes
                val predictedDelay = pred.predictedDelayMinutes
                val activePassDelay = info.activePassengerDelayMinutes

                val reasons = mutableListOf<String>()
                var priority = ActionPriority.LOW

                val isWorsening = pred.trend == PredictionTrend.WORSENING || predictedDelay > currentDelay + 4
                val isBigDiff = abs(predictedDelay - currentDelay) >= 8
                val isOutdatedPassenger = activePassDelay == null && predictedDelay >= 15 || (activePassDelay != null && abs(activePassDelay - predictedDelay) >= 3)

                if (isWorsening || (isOutdatedPassenger && predictedDelay >= 15) || predictedDelay >= 20) {
                    priority = ActionPriority.HIGH
                    if (isWorsening) {
                        val from = activePassDelay ?: 18
                        val to = predictedDelay.coerceAtLeast(from + 4)
                        reasons.add("Prediction increased +$from → +$to min")
                    }
                    if (isOutdatedPassenger) {
                        reasons.add("Passenger update is outdated")
                    }
                    if (reasons.isEmpty()) {
                        reasons.add("High arrival delay prediction (+${predictedDelay} min)")
                    }
                } else if (isBigDiff || predictedDelay in 10..19) {
                    priority = ActionPriority.MEDIUM
                    if (isBigDiff) {
                        reasons.add("ETA changed significantly (+${currentDelay}m actual vs +${predictedDelay}m predicted)")
                    } else {
                        reasons.add("Moderate schedule variance on approach")
                    }
                    if (activePassDelay == null) {
                        reasons.add("Pending passenger broadcast review")
                    }
                } else {
                    priority = ActionPriority.LOW
                    reasons.add("Minor delay monitoring required")
                }

                PriorityActionItem(
                    id = "prio_${train.id}",
                    trainId = train.id,
                    stationId = info.schedule.stationId,
                    trainNumber = train.trainNumber,
                    trainName = train.trainName,
                    priority = priority,
                    currentDelayMinutes = currentDelay,
                    predictedDelayMinutes = predictedDelay,
                    reasons = reasons,
                    routeSubtitle = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}"
                )
            }

            items.sortedWith(compareBy({ it.priority }, { -it.predictedDelayMinutes }))
                .take(3)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            trainRepository.initialize()
        }
    }

    fun selectOperatingStation(stationId: String, isDemoMode: Boolean = false) {
        viewModelScope.launch {
            trainRepository.selectStation(stationId, isDemoMode)
        }
    }

    fun refreshData(isDemoMode: Boolean = false) {
        viewModelScope.launch {
            trainRepository.refreshData(isDemoMode)
        }
    }

    companion object {
        fun provideFactory(
            trainRepository: TrainStatusRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ControlRoomViewModel::class.java)) {
                    return ControlRoomViewModel(trainRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
