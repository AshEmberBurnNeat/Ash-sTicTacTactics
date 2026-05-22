package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticsDao {
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistory)

    @Query("DELETE FROM match_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM saved_game WHERE id = 1 LIMIT 1")
    suspend fun getSavedGame(): SavedGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(game: SavedGame)

    @Query("DELETE FROM saved_game WHERE id = 1")
    suspend fun deleteSavedGame()
}
