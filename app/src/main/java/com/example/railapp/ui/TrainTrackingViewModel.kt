package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.auth.SessionManager
import com.example.railapp.auth.UserRole
import com.example.railapp.data.train.SavedTrainDao
import com.example.railapp.data.train.SavedTrainEntity
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.data.train.TrainTrackingRepository
import com.example.railapp.domain.train.PassengerDelayAction
import com.example.railapp.domain.train.PassengerDelayActionStatus
import com.example.railapp.domain.train.TrainLiveTracking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrainTrackingViewModel(
    private val trainTrackingRepository: TrainTrackingRepository,
    private val trainStatusRepository: TrainStatusRepository,
    private val savedTrainDao: SavedTrainDao,
    private val sessionManager: SessionManager,
    private val trainId: String,
    private val stationId: String
) : ViewModel() {

    val currentUser = sessionManager.currentUser
    val currentRole = sessionManager.currentRole

    val trackingData: StateFlow<TrainLiveTracking?> = trainTrackingRepository
        .getLiveTracking(trainId, stationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePassengerAction: StateFlow<PassengerDelayAction?> = trainStatusRepository
        .passengerActions
        .map { actions ->
            actions.find { it.trainId == trainId && it.status == PassengerDelayActionStatus.ACTIVE }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSaved: StateFlow<Boolean> = currentUser.flatMapLatest { user ->
        if (user != null && user.role == UserRole.PASSENGER) {
            savedTrainDao.isTrainSaved(user.id, trainId)
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showGuestPrompt = MutableStateFlow(false)
    val showGuestPrompt: StateFlow<Boolean> = _showGuestPrompt.asStateFlow()

    fun updatePassengerDelay(newDelayMinutes: Int, reason: String) {
        val tracking = trackingData.value ?: return
        viewModelScope.launch {
            trainStatusRepository.publishOrUpdatePassengerDelay(
                trainId = trainId,
                stationId = stationId,
                trainNumber = tracking.trainNumber,
                trainName = tracking.trainName,
                origin = tracking.origin,
                destination = tracking.destination,
                currentPredictedDelayMinutes = tracking.predictedDelayMinutes,
                newDelayMinutes = newDelayMinutes,
                reason = reason
            )
        }
    }

    fun toggleSaveTrain() {
        val role = currentRole.value
        if (role == UserRole.GUEST || role == null) {
            _showGuestPrompt.value = true
            return
        }

        val user = currentUser.value ?: return
        val currentlySaved = isSaved.value
        val tracking = trackingData.value ?: return

        viewModelScope.launch {
            if (currentlySaved) {
                savedTrainDao.deleteSavedTrain(user.id, trainId)
            } else {
                savedTrainDao.saveTrain(
                    SavedTrainEntity(
                        userId = user.id,
                        trainId = trainId,
                        stationId = stationId,
                        trainNumber = tracking.trainNumber,
                        trainName = tracking.trainName,
                        origin = tracking.origin,
                        destination = tracking.destination
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
            trainTrackingRepository: TrainTrackingRepository,
            trainStatusRepository: TrainStatusRepository,
            savedTrainDao: SavedTrainDao,
            sessionManager: SessionManager,
            trainId: String,
            stationId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TrainTrackingViewModel::class.java)) {
                    return TrainTrackingViewModel(
                        trainTrackingRepository,
                        trainStatusRepository,
                        savedTrainDao,
                        sessionManager,
                        trainId,
                        stationId
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
