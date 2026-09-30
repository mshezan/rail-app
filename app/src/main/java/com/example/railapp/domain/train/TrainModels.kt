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
    val createdBy: String = "RPF Admin",
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
    val modelVersion: String = "prototype-v1",
    val dataSource: String = "MOCK_ML"
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
