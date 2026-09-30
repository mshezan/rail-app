package com.example.railapp.data.train

import com.example.railapp.domain.train.ClockProvider
import com.example.railapp.domain.train.PNRJourney
import com.example.railapp.domain.train.SystemClockProvider
import java.util.concurrent.TimeUnit

interface PNRRepository {
    suspend fun lookupPNR(pnr: String): PNRJourney?
}

class MockPNRRepository(
    private val clockProvider: ClockProvider = SystemClockProvider()
) : PNRRepository {

    override suspend fun lookupPNR(pnr: String): PNRJourney? {
        val cleanPnr = pnr.trim()
        if (cleanPnr.length != 10 || !cleanPnr.all { it.isDigit() }) {
            return null
        }
        val now = clockProvider.currentTimeMillis()

        return when (cleanPnr) {
            "1234567890" -> PNRJourney(
                pnr = "1234567890",
                trainId = "trn_12675",
                trainNumber = "12675",
                trainName = "Kovai Express",
                sourceStation = "Chennai Central",
                destinationStation = "Coimbatore Junction",
                sourceStationCode = "MAS",
                destinationStationCode = "CBE",
                scheduledDeparture = now - TimeUnit.MINUTES.toMillis(90),
                expectedArrival = now + TimeUnit.MINUTES.toMillis(150),
                currentDelayMinutes = 20,
                statusLabel = "TRAIN RUNNING",
                journeyDate = "Today"
            )
            "2345678901" -> PNRJourney(
                pnr = "2345678901",
                trainId = "trn_12640",
                trainNumber = "12640",
                trainName = "Brindavan Express",
                sourceStation = "Bengaluru City Junction",
                destinationStation = "Chennai Central",
                sourceStationCode = "SBC",
                destinationStationCode = "MAS",
                scheduledDeparture = now - TimeUnit.MINUTES.toMillis(120),
                expectedArrival = now + TimeUnit.MINUTES.toMillis(70),
                currentDelayMinutes = 12,
                statusLabel = "TRAIN RUNNING",
                journeyDate = "Today"
            )
            "3456789012" -> PNRJourney(
                pnr = "3456789012",
                trainId = "trn_12007",
                trainNumber = "12007",
                trainName = "MYS Shatabdi Express",
                sourceStation = "Chennai Central",
                destinationStation = "Mysuru Junction",
                sourceStationCode = "MAS",
                destinationStationCode = "MYS",
                scheduledDeparture = now - TimeUnit.MINUTES.toMillis(30),
                expectedArrival = now + TimeUnit.MINUTES.toMillis(260),
                currentDelayMinutes = 18,
                statusLabel = "CONFIRMED • ON TIME",
                journeyDate = "Today"
            )
            "9876543210" -> PNRJourney(
                pnr = "9876543210",
                trainId = "trn_12007",
                trainNumber = "12007",
                trainName = "MYS Shatabdi Express",
                sourceStation = "Chennai Central",
                destinationStation = "Mysuru Junction",
                sourceStationCode = "MAS",
                destinationStationCode = "MYS",
                scheduledDeparture = now - TimeUnit.MINUTES.toMillis(30),
                expectedArrival = now + TimeUnit.MINUTES.toMillis(260),
                currentDelayMinutes = 18,
                statusLabel = "CONFIRMED • ON TIME",
                journeyDate = "Today"
            )
            "4567891230" -> PNRJourney(
                pnr = "4567891230",
                trainId = "trn_12640",
                trainNumber = "12640",
                trainName = "Brindavan Express",
                sourceStation = "Bengaluru City Junction",
                destinationStation = "Chennai Central",
                sourceStationCode = "SBC",
                destinationStationCode = "MAS",
                scheduledDeparture = now - TimeUnit.MINUTES.toMillis(120),
                expectedArrival = now + TimeUnit.MINUTES.toMillis(70),
                currentDelayMinutes = 12,
                statusLabel = "TRAIN RUNNING",
                journeyDate = "Today"
            )
            else -> null
        }
    }
}
