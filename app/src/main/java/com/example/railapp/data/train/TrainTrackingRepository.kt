package com.example.railapp.data.train

import com.example.railapp.domain.train.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TrainTrackingRepository(
    private val trainStatusRepository: TrainStatusRepository? = null,
    private val clockProvider: ClockProvider = SystemClockProvider()
) {
    fun getLiveTracking(trainId: String, targetStationId: String): Flow<TrainLiveTracking?> = flow {
        while (true) {
            val now = clockProvider.currentTimeMillis()
            val tracking = buildTrackingForTrain(trainId, targetStationId, now)
            emit(tracking)
            delay(3000)
        }
    }

    private fun buildTrackingForTrain(trainId: String, targetStationId: String, now: Long): TrainLiveTracking {
        val (rawStops, forecastSource, rawEtaUpdates) = when (trainId) {
            "trn_12640" -> {
                val delayMin = 12
                val list = listOf(
                    createStop("stn_sbc", "Bengaluru City Junction", "SBC", now - TimeUnit.MINUTES.toMillis(180), 0, "5", StopStatus.COMPLETED, 0),
                    createStop("stn_bnc", "Bengaluru Cantt", "BNC", now - TimeUnit.MINUTES.toMillis(165), 0, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_kjm", "Krishnarajapuram", "KJM", now - TimeUnit.MINUTES.toMillis(145), 0, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_bwt", "Bangarapet Junction", "BWT", now - TimeUnit.MINUTES.toMillis(90), 15, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_jtj", "Jolarpettai Junction", "JTJ", now - TimeUnit.MINUTES.toMillis(10), delayMin, "1", StopStatus.CURRENT, 0),
                    createStop("stn_kpd", "Katpadi Junction", "KPD", now + TimeUnit.MINUTES.toMillis(35), delayMin, "1", StopStatus.UPCOMING, 3),
                    createStop("stn_ajj", "Arakkonam Junction", "AJJ", now + TimeUnit.MINUTES.toMillis(75), delayMin, "2", StopStatus.UPCOMING, 5),
                    createStop("stn_per", "Perambur", "PER", now + TimeUnit.MINUTES.toMillis(110), delayMin, "3", StopStatus.UPCOMING, 8),
                    createStop("stn_mas", "Chennai Central", "MAS", now + TimeUnit.MINUTES.toMillis(125), delayMin, "1", StopStatus.UPCOMING, 10)
                )
                val updates = listOf(
                    EtaUpdate("20:03", "20:15", 12, "2 min ago", "Speed restriction"),
                    EtaUpdate("19:50", "20:03", 13, "15 min ago", "Signal halt"),
                    EtaUpdate("19:35", "19:50", 15, "35 min ago", "Congestion ahead"),
                    EtaUpdate("19:20", "19:35", 15, "55 min ago", "Weather"),
                    EtaUpdate("19:25", "19:20", -5, "1 hr 20 min ago", "Recovered time")
                )
                Triple(list, ForecastSource.ML_FORECAST, updates)
            }
            "trn_12007" -> {
                val delayMin = 18
                val list = listOf(
                    createStop("stn_mas", "Chennai Central", "MAS", now - TimeUnit.MINUTES.toMillis(150), 0, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_per", "Perambur", "PER", now - TimeUnit.MINUTES.toMillis(135), 0, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_ajj", "Arakkonam Junction", "AJJ", now - TimeUnit.MINUTES.toMillis(100), 5, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_kpd", "Katpadi Junction", "KPD", now - TimeUnit.MINUTES.toMillis(60), 10, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_jtj", "Jolarpettai Junction", "JTJ", now - TimeUnit.MINUTES.toMillis(15), delayMin, "2", StopStatus.CURRENT, 0),
                    createStop("stn_bwt", "Bangarapet Junction", "BWT", now + TimeUnit.MINUTES.toMillis(30), delayMin, "1", StopStatus.UPCOMING, 3),
                    createStop("stn_sbc", "Bengaluru City Junction", "SBC", now + TimeUnit.MINUTES.toMillis(80), delayMin, "1", StopStatus.UPCOMING, 6),
                    createStop("stn_mys", "Mysuru Junction", "MYS", now + TimeUnit.MINUTES.toMillis(170), delayMin, "6", StopStatus.UPCOMING, 10)
                )
                val updates = listOf(
                    EtaUpdate("18:10", "18:28", 18, "5 min ago", "Signal halt"),
                    EtaUpdate("18:00", "18:10", 10, "20 min ago", "Congestion ahead"),
                    EtaUpdate("17:45", "18:00", 15, "45 min ago", "Speed restriction"),
                    EtaUpdate("17:30", "17:45", 15, "1 hr ago", "Weather"),
                    EtaUpdate("17:35", "17:30", -5, "1 hr 30 min ago", "Recovered time")
                )
                Triple(list, ForecastSource.SCHEDULE_BASED, updates)
            }
            "trn_12675" -> {
                val delayMin = 20
                val list = listOf(
                    createStop("stn_mas", "Chennai Central", "MAS", now - TimeUnit.MINUTES.toMillis(160), 0, "4", StopStatus.COMPLETED, 0),
                    createStop("stn_ajj", "Arakkonam Junction", "AJJ", now - TimeUnit.MINUTES.toMillis(120), 0, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_kpd", "Katpadi Junction", "KPD", now - TimeUnit.MINUTES.toMillis(80), 5, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_jtj", "Jolarpettai Junction", "JTJ", now - TimeUnit.MINUTES.toMillis(40), 10, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_sa", "Salem Junction", "SA", now - TimeUnit.MINUTES.toMillis(5), delayMin, "3", StopStatus.CURRENT, 0),
                    createStop("stn_ed", "Erode Junction", "ED", now + TimeUnit.MINUTES.toMillis(45), delayMin, "2", StopStatus.UPCOMING, 3),
                    createStop("stn_tup", "Tiruppur", "TUP", now + TimeUnit.MINUTES.toMillis(85), delayMin, "1", StopStatus.UPCOMING, 7),
                    createStop("stn_cbe", "Coimbatore Junction", "CBE", now + TimeUnit.MINUTES.toMillis(125), delayMin, "1", StopStatus.UPCOMING, 10)
                )
                val updates = listOf(
                    EtaUpdate("14:30", "14:50", 20, "8 min ago", "Speed restriction"),
                    EtaUpdate("14:15", "14:30", 15, "30 min ago", "Congestion ahead"),
                    EtaUpdate("14:00", "14:15", 15, "50 min ago", "Signal halt"),
                    EtaUpdate("13:50", "14:00", 10, "1 hr 10 min ago", "Weather"),
                    EtaUpdate("13:55", "13:50", -5, "1 hr 40 min ago", "Recovered time")
                )
                Triple(list, ForecastSource.ML_FORECAST, updates)
            }
            "trn_12621" -> {
                val delayMin = 5
                val list = listOf(
                    createStop("stn_mas", "Chennai Central", "MAS", now - TimeUnit.MINUTES.toMillis(180), 0, "5", StopStatus.COMPLETED, 0),
                    createStop("stn_bza", "Vijayawada Junction", "BZA", now - TimeUnit.MINUTES.toMillis(120), 0, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_ngp", "Nagpur Junction", "NGP", now - TimeUnit.MINUTES.toMillis(60), 5, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_bpl", "Bhopal Junction", "BPL", now - TimeUnit.MINUTES.toMillis(5), delayMin, "1", StopStatus.CURRENT, 0),
                    createStop("stn_agc", "Agra Cantt", "AGC", now + TimeUnit.MINUTES.toMillis(50), delayMin, "3", StopStatus.UPCOMING, 3),
                    createStop("stn_ndls", "New Delhi Railway Station", "NDLS", now + TimeUnit.MINUTES.toMillis(110), delayMin, "16", StopStatus.UPCOMING, 5)
                )
                val updates = listOf(
                    EtaUpdate("16:20", "16:25", 5, "12 min ago", "Signal halt"),
                    EtaUpdate("16:00", "16:20", 20, "40 min ago", "Speed restriction"),
                    EtaUpdate("15:40", "16:00", 20, "1 hr ago", "Weather")
                )
                Triple(list, ForecastSource.ML_FORECAST, updates)
            }
            else -> {
                val repoTrain = trainStatusRepository?.operationalInfo?.value
                    ?.find { it.train.id == trainId }?.train
                val originName = repoTrain?.origin?.substringBefore(" (") ?: "Origin Station"
                val destName = repoTrain?.destination?.substringBefore(" (") ?: "Terminus Station"
                val delayMin = 15
                val list = listOf(
                    createStop("stn_dep", originName, "DEP", now - TimeUnit.MINUTES.toMillis(140), 0, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_s1", "Station One", "ST1", now - TimeUnit.MINUTES.toMillis(110), 0, "2", StopStatus.COMPLETED, 0),
                    createStop("stn_s2", "Station Two", "ST2", now - TimeUnit.MINUTES.toMillis(80), 5, "1", StopStatus.COMPLETED, 0),
                    createStop("stn_s3", "Station Three", "ST3", now - TimeUnit.MINUTES.toMillis(50), 10, "3", StopStatus.COMPLETED, 0),
                    createStop("stn_mid", "Midway Junction", "MID", now - TimeUnit.MINUTES.toMillis(5), delayMin, "2", StopStatus.CURRENT, 0),
                    createStop("stn_s4", "Station Four", "ST4", now + TimeUnit.MINUTES.toMillis(35), delayMin, "1", StopStatus.UPCOMING, 3),
                    createStop("stn_s5", "Station Five", "ST5", now + TimeUnit.MINUTES.toMillis(70), delayMin, "2", StopStatus.UPCOMING, 7),
                    createStop("stn_arr", destName, "ARR", now + TimeUnit.MINUTES.toMillis(110), delayMin, "3", StopStatus.UPCOMING, 10)
                )
                val updates = listOf(
                    EtaUpdate("10:15", "10:30", 15, "10 min ago", "Speed restriction"),
                    EtaUpdate("10:00", "10:15", 15, "25 min ago", "Signal halt"),
                    EtaUpdate("09:45", "10:00", 15, "45 min ago", "Congestion ahead"),
                    EtaUpdate("09:30", "09:45", 15, "1 hr ago", "Weather"),
                    EtaUpdate("09:35", "09:30", -5, "1 hr 20 min ago", "Recovered time")
                )
                Triple(list, ForecastSource.ML_FORECAST, updates)
            }
        }

        val activeOverride = trainStatusRepository?.passengerActions?.value
            ?.find { it.trainId == trainId && it.status == PassengerDelayActionStatus.ACTIVE }

        val (stops, etaUpdates) = if (activeOverride != null) {
            val overrideDelay = activeOverride.newDelayMinutes
            val adjustedStops = rawStops.map { stop ->
                if (stop.status == StopStatus.CURRENT || stop.status == StopStatus.UPCOMING) {
                    val exp = stop.scheduledTime + TimeUnit.MINUTES.toMillis(overrideDelay.toLong())
                    stop.copy(delayMinutes = overrideDelay, expectedTime = exp)
                } else stop
            }
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val scheduledArr = adjustedStops.last().scheduledTime
            val prevFormatted = timeFormat.format(Date(scheduledArr + TimeUnit.MINUTES.toMillis(activeOverride.previousDelayMinutes.toLong())))
            val newFormatted = timeFormat.format(Date(scheduledArr + TimeUnit.MINUTES.toMillis(activeOverride.newDelayMinutes.toLong())))

            val overrideUpdate = EtaUpdate(
                oldTimeFormatted = prevFormatted,
                newTimeFormatted = newFormatted,
                delayDeltaMinutes = activeOverride.newDelayMinutes,
                timeAgo = "Just now",
                reason = activeOverride.reason
            )
            Pair(adjustedStops, listOf(overrideUpdate) + rawEtaUpdates.filterNot { it.reason == activeOverride.reason })
        } else {
            Pair(rawStops, rawEtaUpdates)
        }

        val currentStop = stops.find { it.status == StopStatus.CURRENT } ?: stops.first()
        val nextStop = stops.find { it.status == StopStatus.UPCOMING } ?: stops.last()

        val repoTrain = trainStatusRepository?.operationalInfo?.value
            ?.find { it.train.id == trainId }?.train

        val name = repoTrain?.trainName ?: when (trainId) {
            "trn_12675" -> "Kovai Express"
            "trn_12640" -> "Brindavan Express"
            "trn_12007" -> "MYS Shatabdi Express"
            "trn_12621" -> "Tamil Nadu Express"
            "trn_12028" -> "SBC Shatabdi Express"
            "trn_12628" -> "Karnataka Express"
            "trn_12951" -> "Mumbai Rajdhani Express"
            "trn_12301" -> "Howrah Rajdhani Express"
            else -> "Express Train"
        }

        val num = repoTrain?.trainNumber ?: when (trainId) {
            "trn_12675" -> "12675"
            "trn_12640" -> "12640"
            "trn_12007" -> "12007"
            "trn_12621" -> "12621"
            "trn_12028" -> "12028"
            "trn_12628" -> "12628"
            "trn_12951" -> "12951"
            "trn_12301" -> "12301"
            else -> trainId.removePrefix("trn_")
        }

        val originStr = repoTrain?.origin ?: stops.first().stationName
        val destStr = repoTrain?.destination ?: stops.last().stationName

        return TrainLiveTracking(
            trainId = trainId,
            trainNumber = num,
            trainName = name,
            origin = originStr,
            destination = destStr,
            currentStationName = currentStop.stationName,
            nextStationName = nextStop.stationName,
            latitude = 12.9780,
            longitude = 77.5696,
            progressPercent = 0.55f,
            speedKmh = 84,
            currentDelayMinutes = currentStop.delayMinutes,
            predictedDelayMinutes = nextStop.delayMinutes,
            confidence = 88,
            trend = if (nextStop.delayMinutes > currentStop.delayMinutes) PredictionTrend.WORSENING else PredictionTrend.STABLE,
            riskSeverity = if (nextStop.delayMinutes > 20) DelayRiskSeverity.HIGH else DelayRiskSeverity.MODERATE,
            lastUpdated = now,
            routeStops = stops,
            forecastSource = forecastSource,
            etaUpdates = etaUpdates,
            updatedMinutesAgo = 2
        )
    }

    private fun createStop(
        id: String,
        name: String,
        code: String,
        scheduledTime: Long,
        delayMin: Int,
        platform: String,
        status: StopStatus,
        confidenceMargin: Int
    ): RouteStationStop {
        val expectedTime = scheduledTime + TimeUnit.MINUTES.toMillis(delayMin.toLong())
        return RouteStationStop(
            stationId = id,
            stationName = name,
            stationCode = code,
            scheduledTime = scheduledTime,
            expectedTime = expectedTime,
            delayMinutes = delayMin,
            platform = platform,
            status = status,
            confidenceMarginMinutes = confidenceMargin
        )
    }
}
