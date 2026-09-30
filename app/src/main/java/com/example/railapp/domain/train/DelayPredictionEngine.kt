package com.example.railapp.domain.train

import java.util.concurrent.TimeUnit

class DelayPredictionEngine(
    private val clockProvider: ClockProvider = SystemClockProvider()
) {
    fun generatePrediction(
        schedule: TrainStationSchedule,
        currentDelayMinutes: Int,
        predictedDelayMinutes: Int,
        dataSource: DataSourceType,
        isDemoMode: Boolean = false
    ): DelayPrediction {
        val now = clockProvider.currentTimeMillis()
        val effectiveCurrentDelay = if (isDemoMode) currentDelayMinutes + 10 else currentDelayMinutes
        val effectivePredictedDelay = if (isDemoMode) predictedDelayMinutes + 15 else predictedDelayMinutes

        val delayMillis = TimeUnit.MINUTES.toMillis(effectivePredictedDelay.toLong())
        val predictedArrival = schedule.scheduledArrival + delayMillis
        val predictedDeparture = schedule.scheduledDeparture + delayMillis

        val trend = when {
            effectivePredictedDelay > effectiveCurrentDelay + 2 -> PredictionTrend.WORSENING
            effectivePredictedDelay < effectiveCurrentDelay - 2 -> PredictionTrend.IMPROVING
            else -> PredictionTrend.STABLE
        }

        val riskSeverity = when {
            effectivePredictedDelay > 20 -> DelayRiskSeverity.HIGH
            effectivePredictedDelay >= 10 -> DelayRiskSeverity.MODERATE
            else -> DelayRiskSeverity.LOW
        }

        val timeUntilArrivalMinutes = TimeUnit.MILLISECONDS.toMinutes(predictedArrival - now)
        val confidence = calculateConfidence(dataSource, timeUntilArrivalMinutes)

        return DelayPrediction(
            trainId = schedule.trainId,
            stationId = schedule.stationId,
            currentDelayMinutes = effectiveCurrentDelay,
            predictedDelayMinutes = effectivePredictedDelay,
            predictedArrival = predictedArrival,
            predictedDeparture = predictedDeparture,
            confidence = confidence,
            trend = trend,
            riskSeverity = riskSeverity,
            predictionTimestamp = now,
            modelVersion = "v2.4.1",
            dataSource = if (isDemoMode) "TELEMETRY_SIM" else "LIVE_TELEMETRY"
        )
    }

    fun predictStatus(
        schedule: TrainStationSchedule,
        prediction: DelayPrediction,
        dataSource: DataSourceType
    ): TrainStatus {
        val now = clockProvider.currentTimeMillis()

        val status = determineStatus(
            now = now,
            predictedArrival = prediction.predictedArrival,
            predictedDeparture = prediction.predictedDeparture,
            delayMinutes = prediction.predictedDelayMinutes
        )

        return TrainStatus(
            trainId = schedule.trainId,
            stationId = schedule.stationId,
            predictedArrival = prediction.predictedArrival,
            predictedDeparture = prediction.predictedDeparture,
            arrivalDelayMinutes = prediction.predictedDelayMinutes,
            departureDelayMinutes = prediction.predictedDelayMinutes,
            status = status,
            confidence = prediction.confidence,
            lastUpdated = now,
            dataSource = dataSource
        )
    }

    private fun determineStatus(
        now: Long,
        predictedArrival: Long,
        predictedDeparture: Long,
        delayMinutes: Int
    ): TrainStatusType {
        if (now > predictedDeparture + TimeUnit.MINUTES.toMillis(10)) {
            return TrainStatusType.DEPARTED
        }
        if (now in (predictedArrival - TimeUnit.MINUTES.toMillis(3))..predictedDeparture) {
            return if (now >= predictedArrival) TrainStatusType.BOARDING else TrainStatusType.ARRIVING
        }
        if (now in (predictedArrival - TimeUnit.MINUTES.toMillis(15))..<predictedArrival) {
            return TrainStatusType.ARRIVING
        }
        return if (delayMinutes > 5) TrainStatusType.DELAYED else TrainStatusType.ON_TIME
    }

    private fun calculateConfidence(
        dataSource: DataSourceType,
        timeUntilArrivalMinutes: Long
    ): Int {
        val baseConfidence = when (dataSource) {
            DataSourceType.LIVE -> 92
            DataSourceType.MOCK -> 84
            DataSourceType.OFFLINE -> 72
        }

        val proximityBonus = when {
            timeUntilArrivalMinutes in 0..30 -> 6
            timeUntilArrivalMinutes in 31..120 -> 2
            else -> -4
        }

        return (baseConfidence + proximityBonus).coerceIn(55, 98)
    }
}
