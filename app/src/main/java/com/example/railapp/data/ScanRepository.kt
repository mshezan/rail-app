package com.example.railapp.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanEventDao: ScanEventDao) {
    val allEvents: Flow<List<ScanEvent>> = scanEventDao.getAllEvents()

    suspend fun insert(event: ScanEvent) {
        scanEventDao.insert(event)
    }
}
