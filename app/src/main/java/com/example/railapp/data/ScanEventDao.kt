package com.example.railapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanEventDao {
    @Insert
    suspend fun insert(scanEvent: ScanEvent)

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ScanEvent>>
    
    @Query("SELECT * FROM scan_events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<ScanEvent>
    
    @Query("UPDATE scan_events SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
