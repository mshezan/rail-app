package com.example.railapp.data.train

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTrainDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrain(savedTrain: SavedTrainEntity)

    @Query("DELETE FROM saved_trains WHERE userId = :userId AND trainId = :trainId AND stationId = :stationId")
    suspend fun deleteSavedTrain(userId: String, trainId: String, stationId: String)

    @Query("SELECT * FROM saved_trains WHERE userId = :userId")
    fun getSavedTrainsForUser(userId: String): Flow<List<SavedTrainEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_trains WHERE userId = :userId AND trainId = :trainId AND stationId = :stationId)")
    fun isTrainSaved(userId: String, trainId: String, stationId: String): Flow<Boolean>
}
