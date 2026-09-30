package com.example.railapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.railapp.data.ScanEvent
import com.example.railapp.data.ScanRepository
import com.example.railapp.data.ThreatLevel
import kotlinx.coroutines.flow.*

enum class EventFilter {
    ALL, THREATS, UNSYNCED
}

class EventLogViewModel(
    repository: ScanRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(EventFilter.ALL)
    val selectedFilter: StateFlow<EventFilter> = _selectedFilter.asStateFlow()

    val events: StateFlow<List<ScanEvent>> = combine(
        repository.allEvents,
        _selectedFilter
    ) { allEvents, filter ->
        when (filter) {
            EventFilter.ALL -> allEvents
            EventFilter.THREATS -> allEvents.filter { it.threatLevel != ThreatLevel.NONE }
            EventFilter.UNSYNCED -> allEvents.filter { !it.synced }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: EventFilter) {
        _selectedFilter.value = filter
    }

    companion object {
        fun provideFactory(
            repository: ScanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EventLogViewModel::class.java)) {
                    return EventLogViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
