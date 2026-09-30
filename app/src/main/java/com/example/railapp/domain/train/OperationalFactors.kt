package com.example.railapp.domain.train

enum class DelayCauseCategory {
    SPEED_RESTRICTION,
    NETWORK_CONGESTION,
    UNEXPECTED_STOPPAGE,
    SIGNAL_HALT,
    HISTORICAL_PATTERN,
    MAINTENANCE_BLOCK,
    WEATHER_CONDITION
}

data class OperationalFactors(
    val operationalZone: String = "Southern Railway (SR)",
    val primaryCause: DelayCauseCategory = DelayCauseCategory.NETWORK_CONGESTION,
    val primaryCauseDescription: String = "Section congestion near junction approach",
    val speedRestrictionKmh: Int? = 30,
    val weatherCondition: String = "Monsoon Clear",
    val gpsAccuracyMeters: Float = 4.2f,
    val activeFactors: List<String> = listOf("Signal Interlocking Delay", "Corridor Capacity Bottleneck")
)
