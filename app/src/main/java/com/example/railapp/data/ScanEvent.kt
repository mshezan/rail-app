package com.example.railapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SubstanceCategory {
    NARCOTIC, EXPLOSIVE, NONE
}

@Serializable
enum class ThreatLevel {
    LOW, MEDIUM, HIGH, NONE
}

@Serializable
@Entity(tableName = "scan_events")
data class ScanEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val substance: String,
    val category: SubstanceCategory,
    val threatLevel: ThreatLevel,
    val confidence: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    @SerialName("device_id")
    val deviceId: String = "DEMO_DEVICE"
)
