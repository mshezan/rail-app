package com.example.railapp.data.train

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTrainDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrain(savedTrain: SavedTrainEntity)

    @Query("DELETE FROM saved_trains WHERE userId = :userId AND trainId = :trainId")
    suspend fun deleteSavedTrain(userId: String, trainId: String)

    @Query("SELECT * FROM saved_trains WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSavedTrainsForUser(userId: String): Flow<List<SavedTrainEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_trains WHERE userId = :userId AND trainId = :trainId)")
    fun isTrainSaved(userId: String, trainId: String): Flow<Boolean>
}
