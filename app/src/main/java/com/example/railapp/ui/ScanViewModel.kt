package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.data.ScanEvent
import com.example.railapp.data.ScanRepository
import com.example.railapp.sensor.DetectionSensor
import com.example.railapp.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Result(val event: ScanEvent) : ScanState()
}

class ScanViewModel(
    private val sensor: DetectionSensor,
    private val repository: ScanRepository,
    private val syncManager: SyncManager? = null
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _locationLabel = MutableStateFlow("Platform 1")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    fun updateLocationLabel(label: String) {
        _locationLabel.value = label
    }

    fun startScan(latitude: Double? = null, longitude: Double? = null, deviceId: String = "DEMO_DEVICE") {
        if (_scanState.value is ScanState.Scanning) return
        
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            val result = sensor.performScan(
                locationLabel = _locationLabel.value,
                latitude = latitude,
                longitude = longitude,
                deviceId = deviceId
            )
            
            // Save to database
            repository.insert(result)
            
            // Trigger sync
            syncManager?.triggerSync()
            
            _scanState.value = ScanState.Result(result)
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    companion object {
        fun provideFactory(
            sensor: DetectionSensor,
            repository: ScanRepository,
            syncManager: SyncManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                    return ScanViewModel(sensor, repository, syncManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
