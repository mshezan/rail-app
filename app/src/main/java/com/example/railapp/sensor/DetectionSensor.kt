package com.example.railapp.sensor

import com.example.railapp.data.ScanEvent
import com.example.railapp.data.SubstanceCategory
import com.example.railapp.data.ThreatLevel
import kotlinx.coroutines.delay
import kotlin.random.Random

interface DetectionSensor {
    var forceThreat: Boolean
    suspend fun performScan(locationLabel: String, latitude: Double? = null, longitude: Double? = null, deviceId: String = "DEMO_DEVICE"): ScanEvent
}

class SimulatedSensor : DetectionSensor {
    override var forceThreat: Boolean = false

    private val substances = listOf(
        "Heroin" to Pair(SubstanceCategory.NARCOTIC, ThreatLevel.HIGH),
        "Cocaine" to Pair(SubstanceCategory.NARCOTIC, ThreatLevel.HIGH),
        "Methamphetamine" to Pair(SubstanceCategory.NARCOTIC, ThreatLevel.HIGH),
        "Cannabis" to Pair(SubstanceCategory.NARCOTIC, ThreatLevel.MEDIUM),
        "Opium" to Pair(SubstanceCategory.NARCOTIC, ThreatLevel.HIGH),
        "RDX" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "TNT" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "PETN" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "TATP" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "C4" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "Dynamite" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.HIGH),
        "Ammonium Nitrate" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.MEDIUM),
        "Gunpowder" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.MEDIUM),
        "Pyrotechnics" to Pair(SubstanceCategory.EXPLOSIVE, ThreatLevel.LOW)
    )

    override suspend fun performScan(
        locationLabel: String,
        latitude: Double?,
        longitude: Double?,
        deviceId: String
    ): ScanEvent {
        delay(Random.nextLong(1500, 2500))
        
        val isClear = if (forceThreat) {
            forceThreat = false // Reset after one forced threat
            false
        } else {
            Random.nextFloat() < 0.8f
        }
        
        return if (isClear) {
            ScanEvent(
                substance = "Clear",
                category = SubstanceCategory.NONE,
                threatLevel = ThreatLevel.NONE,
                confidence = Random.nextInt(90, 100),
                locationLabel = locationLabel,
                latitude = latitude,
                longitude = longitude,
                deviceId = deviceId
            )
        } else {
            val (substance, pair) = substances.random()
            ScanEvent(
                substance = substance,
                category = pair.first,
                threatLevel = pair.second,
                confidence = Random.nextInt(75, 99),
                locationLabel = locationLabel,
                latitude = latitude,
                longitude = longitude,
                deviceId = deviceId
            )
        }
    }
}
