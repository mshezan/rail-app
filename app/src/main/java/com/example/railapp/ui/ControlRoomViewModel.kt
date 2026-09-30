package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.DelayRiskSeverity
import com.example.railapp.domain.train.Station
import com.example.railapp.domain.train.StationTrainOperationalInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminOperationsSummary(
    val approachingCount: Int = 0,
    val currentAvgDelayMinutes: Int = 0,
    val predictedAvgDelayMinutes: Int = 0,
    val highRiskCount: Int = 0
)

class ControlRoomViewModel(
    private val trainRepository: TrainStatusRepository
) : ViewModel() {

    val stations: StateFlow<List<Station>> = trainRepository.stations
    val operatingStation: StateFlow<Station?> = trainRepository.selectedStation
    val isOffline: StateFlow<Boolean> = trainRepository.isOffline
    val lastUpdatedTime: StateFlow<Long> = trainRepository.lastUpdatedTime

    val approachingTrains: StateFlow<List<StationTrainOperationalInfo>> = trainRepository.operationalInfo
        .map { list ->
            list.sortedBy { it.prediction.predictedArrival }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val opsSummary: StateFlow<AdminOperationsSummary> = approachingTrains.map { list ->
        if (list.isEmpty()) {
            AdminOperationsSummary()
        } else {
            val totalCurrentDelay = list.sumOf { it.prediction.currentDelayMinutes }
            val totalPredictedDelay = list.sumOf { it.prediction.predictedDelayMinutes }
            val highRiskCount = list.count { it.prediction.riskSeverity == DelayRiskSeverity.HIGH }

            AdminOperationsSummary(
                approachingCount = list.size,
                currentAvgDelayMinutes = totalCurrentDelay / list.size,
                predictedAvgDelayMinutes = totalPredictedDelay / list.size,
                highRiskCount = highRiskCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminOperationsSummary())

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
