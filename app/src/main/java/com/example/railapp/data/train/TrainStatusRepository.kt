package com.example.railapp.data.train

import com.example.railapp.domain.train.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class TrainStatusRepository(
    private val dataSource: TrainDataSource = MockTrainDataSource()
) {
    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _rawOperationalInfo = MutableStateFlow<List<StationTrainOperationalInfo>>(emptyList())

    private val _passengerActions = MutableStateFlow<List<PassengerDelayAction>>(emptyList())
    val passengerActions: StateFlow<List<PassengerDelayAction>> = _passengerActions.asStateFlow()

    private val _operationalInfo = MutableStateFlow<List<StationTrainOperationalInfo>>(emptyList())
    val operationalInfo: StateFlow<List<StationTrainOperationalInfo>> = _operationalInfo.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow(System.currentTimeMillis())
    val lastUpdatedTime: StateFlow<Long> = _lastUpdatedTime.asStateFlow()

    suspend fun initialize() {
        val fetchedStations = dataSource.getStations()
        _stations.value = fetchedStations
        if (fetchedStations.isNotEmpty() && _selectedStation.value == null) {
            selectStation(fetchedStations.first().id)
        }
    }

    suspend fun selectStation(stationId: String, isDemoMode: Boolean = false) {
        val station = _stations.value.find { it.id == stationId }
        _selectedStation.value = station
        refreshData(isDemoMode)
    }

    suspend fun refreshData(isDemoMode: Boolean = false) {
        val currentStation = _selectedStation.value ?: return
        try {
            val raw = dataSource.getOperationalInfoForStation(currentStation.id, isDemoMode)
            _rawOperationalInfo.value = raw
            applyPassengerOverrides()
            _isOffline.value = false
            _lastUpdatedTime.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            serveOffline()
            _isOffline.value = true
        }
    }

    private fun serveOffline() {
        applyPassengerOverrides()
    }

    fun publishOrUpdatePassengerDelay(
        info: StationTrainOperationalInfo,
        newDelayMinutes: Int,
        reason: String = "Delay updated by Station Control",
        adminName: String = "RPF Admin"
    ) {
        val currentActions = _passengerActions.value
        val activeAction = currentActions.find {
            it.trainId == info.train.id && it.stationId == info.schedule.stationId && it.status == PassengerDelayActionStatus.ACTIVE
        }

        val previousDelay = activeAction?.newDelayMinutes ?: info.prediction.predictedDelayMinutes
        val actionType = if (activeAction != null) PassengerDelayActionType.UPDATE else PassengerDelayActionType.PUBLISH

        val newAction = PassengerDelayAction(
            trainId = info.train.id,
            stationId = info.schedule.stationId,
            trainNumber = info.train.trainNumber,
            trainName = info.train.trainName,
            origin = info.train.origin,
            destination = info.train.destination,
            previousDelayMinutes = previousDelay,
            newDelayMinutes = newDelayMinutes,
            actionType = actionType,
            reason = reason,
            createdBy = adminName,
            createdAt = System.currentTimeMillis(),
            status = PassengerDelayActionStatus.ACTIVE
        )

        // Mark previous active action as updated/superseded
        val updatedList = currentActions.map { action ->
            if (action.trainId == info.train.id && action.stationId == info.schedule.stationId && action.status == PassengerDelayActionStatus.ACTIVE) {
                action.copy(status = PassengerDelayActionStatus.REVERTED)
            } else {
                action
            }
        } + newAction

        _passengerActions.value = updatedList
        applyPassengerOverrides()
    }

    fun revertPassengerDelay(
        actionToRevert: PassengerDelayAction,
        reason: String = "Delay prediction changed / Reverted by Admin",
        adminName: String = "RPF Admin"
    ) {
        val currentActions = _passengerActions.value

        val revertAction = PassengerDelayAction(
            trainId = actionToRevert.trainId,
            stationId = actionToRevert.stationId,
            trainNumber = actionToRevert.trainNumber,
            trainName = actionToRevert.trainName,
            origin = actionToRevert.origin,
            destination = actionToRevert.destination,
            previousDelayMinutes = actionToRevert.newDelayMinutes,
            newDelayMinutes = actionToRevert.previousDelayMinutes,
            actionType = PassengerDelayActionType.REVERT,
            reason = reason,
            createdBy = adminName,
            createdAt = System.currentTimeMillis(),
            status = PassengerDelayActionStatus.REVERTED
        )

        val updatedList = currentActions.map {
            if (it.id == actionToRevert.id) it.copy(status = PassengerDelayActionStatus.REVERTED) else it
        } + revertAction

        _passengerActions.value = updatedList
        applyPassengerOverrides()
    }

    private fun applyPassengerOverrides() {
        val actions = _passengerActions.value
        val rawList = _rawOperationalInfo.value

        val activeMap = actions
            .filter { it.status == PassengerDelayActionStatus.ACTIVE }
            .associateBy { "${it.trainId}_${it.stationId}" }

        _operationalInfo.value = rawList.map { info ->
            val key = "${info.train.id}_${info.schedule.stationId}"
            val activeAction = activeMap[key]
            val overrideDelay = activeAction?.newDelayMinutes

            if (overrideDelay != null) {
                val delayMillis = TimeUnit.MINUTES.toMillis(overrideDelay.toLong())
                info.copy(
                    activePassengerDelayMinutes = overrideDelay,
                    status = info.status.copy(
                        predictedArrival = info.schedule.scheduledArrival + delayMillis,
                        predictedDeparture = info.schedule.scheduledDeparture + delayMillis,
                        arrivalDelayMinutes = overrideDelay,
                        departureDelayMinutes = overrideDelay
                    )
                )
            } else {
                info
            }
        }
    }
}
