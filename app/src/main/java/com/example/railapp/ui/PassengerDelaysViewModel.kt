package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.domain.train.PassengerDelayAction
import com.example.railapp.domain.train.PassengerDelayActionStatus
import com.example.railapp.domain.train.StationTrainOperationalInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PassengerDelaysViewModel(
    private val trainRepository: TrainStatusRepository
) : ViewModel() {

    val passengerActions: StateFlow<List<PassengerDelayAction>> = trainRepository.passengerActions
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeActions: StateFlow<List<PassengerDelayAction>> = trainRepository.passengerActions
        .map { list -> list.filter { it.status == PassengerDelayActionStatus.ACTIVE }.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updatePassengerDelay(
        info: StationTrainOperationalInfo,
        newDelayMinutes: Int,
        reason: String = "Delay updated by Station Control"
    ) {
        viewModelScope.launch {
            trainRepository.publishOrUpdatePassengerDelay(info, newDelayMinutes, reason)
        }
    }

    fun revertPassengerDelay(action: PassengerDelayAction, reason: String = "Delay prediction reverted by Admin") {
        viewModelScope.launch {
            trainRepository.revertPassengerDelay(action, reason)
        }
    }

    companion object {
        fun provideFactory(
            trainRepository: TrainStatusRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PassengerDelaysViewModel::class.java)) {
                    return PassengerDelaysViewModel(trainRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
