package com.example.data

import kotlinx.coroutines.flow.Flow

class TacticsRepository(private val tacticsDao: TacticsDao) {
    val allHistory: Flow<List<MatchHistory>> = tacticsDao.getAllHistory()

    suspend fun insertMatch(match: MatchHistory) {
        tacticsDao.insertMatch(match)
    }

    suspend fun clearHistory() {
        tacticsDao.clearHistory()
    }

    suspend fun getSavedGame(): SavedGame? {
        return tacticsDao.getSavedGame()
    }

    suspend fun saveGame(game: SavedGame) {
        tacticsDao.saveGame(game)
    }

    suspend fun deleteSavedGame() {
        tacticsDao.deleteSavedGame()
    }
}
