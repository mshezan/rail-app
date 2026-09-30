package com.example.railapp.domain.train

import java.util.UUID

data class Station(
    val id: String,
    val name: String,
    val code: String,
    val latitude: Double,
    val longitude: Double
)

data class Train(
    val id: String,
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String
)

data class TrainStationSchedule(
    val trainId: String,
    val stationId: String,
    val scheduledArrival: Long,
    val scheduledDeparture: Long,
    val platform: String
)

enum class TrainStatusType {
    ON_TIME, DELAYED, ARRIVING, BOARDING, DEPARTED, CANCELLED
}

enum class DataSourceType {
    LIVE, MOCK, OFFLINE
}

enum class PredictionTrend {
    STABLE, WORSENING, IMPROVING
}

enum class DelayRiskSeverity {
    LOW, MODERATE, HIGH
}

enum class PassengerDelayActionType {
    PUBLISH, UPDATE, REVERT
}

enum class PassengerDelayActionStatus {
    ACTIVE, REVERTED
}

enum class StopStatus {
    COMPLETED, CURRENT, UPCOMING
}

enum class ForecastSource {
    ML_FORECAST, SCHEDULE_BASED
}

data class EtaUpdate(
    val oldTimeFormatted: String,
    val newTimeFormatted: String,
    val delayDeltaMinutes: Int,
    val timeAgo: String,
    val reason: String
)

data class RouteStationStop(
    val stationId: String,
    val stationName: String,
    val stationCode: String,
    val scheduledTime: Long,
    val expectedTime: Long,
    val delayMinutes: Int,
    val platform: String,
    val status: StopStatus,
    val confidenceMarginMinutes: Int = 0
)

data class TrainLiveTracking(
    val trainId: String,
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String,
    val currentStationName: String,
    val nextStationName: String,
    val latitude: Double,
    val longitude: Double,
    val progressPercent: Float,
    val speedKmh: Int = 82,
    val currentDelayMinutes: Int,
    val predictedDelayMinutes: Int,
    val confidence: Int,
    val trend: PredictionTrend,
    val riskSeverity: DelayRiskSeverity,
    val lastUpdated: Long = System.currentTimeMillis(),
    val routeStops: List<RouteStationStop>,
    val operationalFactors: OperationalFactors = OperationalFactors(),
    val forecastSource: ForecastSource = ForecastSource.ML_FORECAST,
    val etaUpdates: List<EtaUpdate> = emptyList(),
    val updatedMinutesAgo: Int = 2
)

data class PNRJourney(
    val pnr: String,
    val trainId: String,
    val trainNumber: String,
    val trainName: String,
    val sourceStation: String,
    val destinationStation: String,
    val sourceStationCode: String,
    val destinationStationCode: String,
    val scheduledDeparture: Long,
    val expectedArrival: Long,
    val currentDelayMinutes: Int,
    val statusLabel: String = "ON THE WAY",
    val journeyDate: String = "Today"
)

data class PassengerDelayAction(
    val id: String = UUID.randomUUID().toString(),
    val trainId: String,
    val stationId: String,
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String,
    val previousDelayMinutes: Int,
    val newDelayMinutes: Int,
    val actionType: PassengerDelayActionType,
    val reason: String = "",
    val createdBy: String = "Station Control Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val status: PassengerDelayActionStatus = PassengerDelayActionStatus.ACTIVE
)

data class DelayPrediction(
    val trainId: String,
    val stationId: String,
    val currentDelayMinutes: Int,
    val predictedDelayMinutes: Int,
    val predictedArrival: Long,
    val predictedDeparture: Long,
    val confidence: Int,
    val trend: PredictionTrend,
    val riskSeverity: DelayRiskSeverity,
    val predictionTimestamp: Long = System.currentTimeMillis(),
    val modelVersion: String = "v2.4.1",
    val dataSource: String = "LIVE_TELEMETRY",
    val operationalFactors: OperationalFactors = OperationalFactors()
)

data class TrainStatus(
    val trainId: String,
    val stationId: String,
    val predictedArrival: Long,
    val predictedDeparture: Long,
    val arrivalDelayMinutes: Int,
    val departureDelayMinutes: Int,
    val status: TrainStatusType,
    val confidence: Int,
    val lastUpdated: Long,
    val dataSource: DataSourceType
)

data class StationTrainOperationalInfo(
    val train: Train,
    val schedule: TrainStationSchedule,
    val status: TrainStatus,
    val prediction: DelayPrediction,
    val activePassengerDelayMinutes: Int? = null
)
