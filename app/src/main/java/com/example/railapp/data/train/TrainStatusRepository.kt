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

    private val _passengerActions = MutableStateFlow<List<PassengerDelayAction>>(
        listOf(
            PassengerDelayAction(
                trainId = "trn_12640",
                stationId = "stn_mas",
                trainNumber = "12640",
                trainName = "Brindavan Express",
                origin = "Bengaluru City Junction (SBC)",
                destination = "Chennai Central (MAS)",
                previousDelayMinutes = 0,
                newDelayMinutes = 15,
                actionType = PassengerDelayActionType.PUBLISH,
                reason = "Congestion ahead",
                createdAt = System.currentTimeMillis() - 3600000,
                status = PassengerDelayActionStatus.REVERTED
            ),
            PassengerDelayAction(
                trainId = "trn_12640",
                stationId = "stn_mas",
                trainNumber = "12640",
                trainName = "Brindavan Express",
                origin = "Bengaluru City Junction (SBC)",
                destination = "Chennai Central (MAS)",
                previousDelayMinutes = 15,
                newDelayMinutes = 13,
                actionType = PassengerDelayActionType.UPDATE,
                reason = "Signal halt",
                createdAt = System.currentTimeMillis() - 2100000,
                status = PassengerDelayActionStatus.REVERTED
            ),
            PassengerDelayAction(
                trainId = "trn_12640",
                stationId = "stn_mas",
                trainNumber = "12640",
                trainName = "Brindavan Express",
                origin = "Bengaluru City Junction (SBC)",
                destination = "Chennai Central (MAS)",
                previousDelayMinutes = 13,
                newDelayMinutes = 12,
                actionType = PassengerDelayActionType.UPDATE,
                reason = "Speed restriction",
                createdAt = System.currentTimeMillis() - 900000,
                status = PassengerDelayActionStatus.ACTIVE
            ),
            PassengerDelayAction(
                trainId = "trn_12675",
                stationId = "stn_mas",
                trainNumber = "12675",
                trainName = "Kovai Express",
                origin = "Chennai Central (MAS)",
                destination = "Coimbatore Junction (CBE)",
                previousDelayMinutes = 0,
                newDelayMinutes = 15,
                actionType = PassengerDelayActionType.PUBLISH,
                reason = "Congestion ahead",
                createdAt = System.currentTimeMillis() - 2700000,
                status = PassengerDelayActionStatus.REVERTED
            ),
            PassengerDelayAction(
                trainId = "trn_12675",
                stationId = "stn_mas",
                trainNumber = "12675",
                trainName = "Kovai Express",
                origin = "Chennai Central (MAS)",
                destination = "Coimbatore Junction (CBE)",
                previousDelayMinutes = 15,
                newDelayMinutes = 20,
                actionType = PassengerDelayActionType.UPDATE,
                reason = "Speed restriction",
                createdAt = System.currentTimeMillis() - 600000,
                status = PassengerDelayActionStatus.ACTIVE
            )
        )
    )
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
        trainId: String,
        stationId: String,
        trainNumber: String,
        trainName: String,
        origin: String,
        destination: String,
        currentPredictedDelayMinutes: Int,
        newDelayMinutes: Int,
        reason: String = "Delay updated by Station Control",
        adminName: String = "Station Control Admin"
    ) {
        val currentActions = _passengerActions.value
        val activeAction = currentActions.find {
            it.trainId == trainId && it.status == PassengerDelayActionStatus.ACTIVE
        }

        val previousDelay = activeAction?.newDelayMinutes ?: currentPredictedDelayMinutes
        val actionType = if (activeAction != null) PassengerDelayActionType.UPDATE else PassengerDelayActionType.PUBLISH

        val effectiveStationId = if (stationId.isNotBlank()) stationId else (activeAction?.stationId ?: "stn_mas")

        val newAction = PassengerDelayAction(
            trainId = trainId,
            stationId = effectiveStationId,
            trainNumber = trainNumber,
            trainName = trainName,
            origin = origin,
            destination = destination,
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
            if (action.trainId == trainId && action.status == PassengerDelayActionStatus.ACTIVE) {
                action.copy(status = PassengerDelayActionStatus.REVERTED)
            } else {
                action
            }
        } + newAction

        _passengerActions.value = updatedList
        applyPassengerOverrides()
    }

    fun publishOrUpdatePassengerDelay(
        info: StationTrainOperationalInfo,
        newDelayMinutes: Int,
        reason: String = "Delay updated by Station Control",
        adminName: String = "Station Control Admin"
    ) {
        publishOrUpdatePassengerDelay(
            trainId = info.train.id,
            stationId = info.schedule.stationId,
            trainNumber = info.train.trainNumber,
            trainName = info.train.trainName,
            origin = info.train.origin,
            destination = info.train.destination,
            currentPredictedDelayMinutes = info.prediction.predictedDelayMinutes,
            newDelayMinutes = newDelayMinutes,
            reason = reason,
            adminName = adminName
        )
    }

    fun revertPassengerDelay(
        actionToRevert: PassengerDelayAction,
        reason: String = "Delay prediction changed / Reverted by Admin",
        adminName: String = "Station Control Admin"
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

        val activeByKey = actions
            .filter { it.status == PassengerDelayActionStatus.ACTIVE }
            .associateBy { "${it.trainId}_${it.stationId}" }

        val activeByTrainId = actions
            .filter { it.status == PassengerDelayActionStatus.ACTIVE }
            .associateBy { it.trainId }

        _operationalInfo.value = rawList.map { info ->
            val key = "${info.train.id}_${info.schedule.stationId}"
            val activeAction = activeByKey[key] ?: activeByTrainId[info.train.id]
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
