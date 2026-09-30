package com.example.railapp.data.train

import com.example.railapp.domain.train.*
import java.util.concurrent.TimeUnit

interface TrainDataSource {
    suspend fun getStations(): List<Station>
    suspend fun getOperationalInfoForStation(stationId: String, isDemoMode: Boolean = false): List<StationTrainOperationalInfo>
}

class MockTrainDataSource(
    private val delayEngine: DelayPredictionEngine = DelayPredictionEngine(),
    private val clockProvider: ClockProvider = SystemClockProvider()
) : TrainDataSource {

    private val stations = listOf(
        Station("stn_mas", "Chennai Central", "MAS", 13.0827, 80.2707),
        Station("stn_sbc", "Bengaluru City Junction", "SBC", 12.9780, 77.5696),
        Station("stn_ndls", "New Delhi Railway Station", "NDLS", 28.6430, 77.2194),
        Station("stn_mmct", "Mumbai Central", "MMCT", 18.9696, 72.8193),
        Station("stn_hwh", "Howrah Junction", "HWH", 22.5839, 88.3427)
    )

    private val trains = listOf(
        Train("trn_12675", "12675", "Kovai Express", "Chennai Central (MAS)", "Coimbatore (CBE)"),
        Train("trn_12007", "12007", "MYS Shatabdi Express", "Chennai Central (MAS)", "Mysuru (MYS)"),
        Train("trn_12621", "12621", "Tamil Nadu Express", "Chennai Central (MAS)", "New Delhi (NDLS)"),
        Train("trn_12640", "12640", "Brindavan Express", "Bengaluru City (SBC)", "Chennai Central (MAS)"),
        Train("trn_12028", "12028", "SBC Shatabdi Express", "Bengaluru City (SBC)", "Chennai Central (MAS)"),
        Train("trn_12628", "12628", "Karnataka Express", "Bengaluru City (SBC)", "New Delhi (NDLS)"),
        Train("trn_12951", "12951", "Mumbai Rajdhani Express", "Mumbai Central (MMCT)", "New Delhi (NDLS)"),
        Train("trn_12301", "12301", "Howrah Rajdhani Express", "Howrah Junction (HWH)", "New Delhi (NDLS)")
    )

    override suspend fun getStations(): List<Station> = stations

    override suspend fun getOperationalInfoForStation(
        stationId: String,
        isDemoMode: Boolean
    ): List<StationTrainOperationalInfo> {
        val now = clockProvider.currentTimeMillis()

        return when (stationId) {
            "stn_mas" -> listOf(
                createOpInfo("trn_12675", "stn_mas", now + TimeUnit.MINUTES.toMillis(20), now + TimeUnit.MINUTES.toMillis(35), "Platform 4", 12, 22, isDemoMode),
                createOpInfo("trn_12007", "stn_mas", now + TimeUnit.MINUTES.toMillis(45), now + TimeUnit.MINUTES.toMillis(60), "Platform 2", 18, 18, isDemoMode),
                createOpInfo("trn_12621", "stn_mas", now + TimeUnit.MINUTES.toMillis(90), now + TimeUnit.MINUTES.toMillis(110), "Platform 7", 5, 5, isDemoMode),
                createOpInfo("trn_12640", "stn_mas", now - TimeUnit.MINUTES.toMillis(10), now + TimeUnit.MINUTES.toMillis(5), "Platform 1", 25, 12, isDemoMode),
                createOpInfo("trn_12028", "stn_mas", now + TimeUnit.MINUTES.toMillis(150), now + TimeUnit.MINUTES.toMillis(165), "Platform 3", 0, 0, isDemoMode)
            )
            "stn_sbc" -> listOf(
                createOpInfo("trn_12640", "stn_sbc", now - TimeUnit.MINUTES.toMillis(40), now - TimeUnit.MINUTES.toMillis(25), "Platform 5", 2, 2, isDemoMode),
                createOpInfo("trn_12028", "stn_sbc", now + TimeUnit.MINUTES.toMillis(10), now + TimeUnit.MINUTES.toMillis(25), "Platform 1", 8, 16, isDemoMode),
                createOpInfo("trn_12628", "stn_sbc", now + TimeUnit.MINUTES.toMillis(75), now + TimeUnit.MINUTES.toMillis(90), "Platform 3", 20, 28, isDemoMode)
            )
            "stn_ndls" -> listOf(
                createOpInfo("trn_12621", "stn_ndls", now + TimeUnit.MINUTES.toMillis(200), now + TimeUnit.MINUTES.toMillis(220), "Platform 16", 10, 10, isDemoMode),
                createOpInfo("trn_12628", "stn_ndls", now + TimeUnit.MINUTES.toMillis(180), now + TimeUnit.MINUTES.toMillis(200), "Platform 8", 22, 35, isDemoMode),
                createOpInfo("trn_12951", "stn_ndls", now + TimeUnit.MINUTES.toMillis(30), now + TimeUnit.MINUTES.toMillis(45), "Platform 1", 0, 0, isDemoMode),
                createOpInfo("trn_12301", "stn_ndls", now + TimeUnit.MINUTES.toMillis(120), now + TimeUnit.MINUTES.toMillis(135), "Platform 9", 0, 0, isDemoMode)
            )
            "stn_mmct" -> listOf(
                createOpInfo("trn_12951", "stn_mmct", now - TimeUnit.MINUTES.toMillis(15), now + TimeUnit.MINUTES.toMillis(5), "Platform 2", 0, 0, isDemoMode)
            )
            "stn_hwh" -> listOf(
                createOpInfo("trn_12301", "stn_hwh", now - TimeUnit.MINUTES.toMillis(30), now - TimeUnit.MINUTES.toMillis(10), "Platform 10", 5, 8, isDemoMode)
            )
            else -> emptyList()
        }
    }

    private fun createOpInfo(
        trainId: String,
        stationId: String,
        schedArr: Long,
        schedDep: Long,
        platform: String,
        currentDelay: Int,
        predictedDelay: Int,
        isDemoMode: Boolean
    ): StationTrainOperationalInfo {
        val train = trains.first { it.id == trainId }
        val schedule = TrainStationSchedule(
            trainId = trainId,
            stationId = stationId,
            scheduledArrival = schedArr,
            scheduledDeparture = schedDep,
            platform = platform
        )
        val prediction = delayEngine.generatePrediction(
            schedule = schedule,
            currentDelayMinutes = currentDelay,
            predictedDelayMinutes = predictedDelay,
            dataSource = DataSourceType.MOCK,
            isDemoMode = isDemoMode
        )
        val status = delayEngine.predictStatus(
            schedule = schedule,
            prediction = prediction,
            dataSource = DataSourceType.MOCK
        )

        return StationTrainOperationalInfo(
            train = train,
            schedule = schedule,
            status = status,
            prediction = prediction
        )
    }
}
