package com.example.railapp.api

import com.example.railapp.domain.train.StationTrainOperationalInfo

data class StationDisplayFeedItem(
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String,
    val scheduledTime: String,
    val expectedTime: String,
    val delayMinutes: Int,
    val platform: String,
    val status: String,
    val primaryCause: String
)

data class ControlRoomApiFeed(
    val operatingStationCode: String,
    val activeZone: String,
    val totalApproachingTrains: Int,
    val currentAvgDelayMinutes: Int,
    val predictedAvgDelayMinutes: Int,
    val highRiskTrainsCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class RailApiIntegration {

    fun generateStationDisplaySummary(trains: List<StationTrainOperationalInfo>): List<StationDisplayFeedItem> {
        return trains.map { info ->
            StationDisplayFeedItem(
                trainNumber = info.train.trainNumber,
                trainName = info.train.trainName,
                origin = info.train.origin,
                destination = info.train.destination,
                scheduledTime = info.schedule.scheduledArrival.toString(),
                expectedTime = info.prediction.predictedArrival.toString(),
                delayMinutes = info.prediction.predictedDelayMinutes,
                platform = info.schedule.platform,
                status = info.status.status.name,
                primaryCause = info.prediction.operationalFactors.primaryCauseDescription
            )
        }
    }

    fun generateControlRoomFeed(stationCode: String, zone: String, approachingCount: Int, currentAvgDelay: Int, predictedAvgDelay: Int, highRiskCount: Int): ControlRoomApiFeed {
        return ControlRoomApiFeed(
            operatingStationCode = stationCode,
            activeZone = zone,
            totalApproachingTrains = approachingCount,
            currentAvgDelayMinutes = currentAvgDelay,
            predictedAvgDelayMinutes = predictedAvgDelay,
            highRiskTrainsCount = highRiskCount
        )
    }
}
