package com.example.railapp.data.train

import androidx.room.Entity

@Entity(tableName = "saved_trains", primaryKeys = ["userId", "trainId"])
data class SavedTrainEntity(
    val userId: String,
    val trainId: String,
    val stationId: String = "stn_mas",
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String,
    val createdAt: Long = System.currentTimeMillis()
)
