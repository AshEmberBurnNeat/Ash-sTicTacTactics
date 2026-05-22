package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameMode: String, // "PVP" or "AI_EASY", "AI_MEDIUM", "AI_HARD"
    val winner: String, // "X" (Player 1 / You), "O" (Player 2 / AI), "DRAW"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_game")
data class SavedGame(
    @PrimaryKey val id: Int = 1,
    val cellsState: String, // String of 81 characters: 'X', 'O', '.'
    val boardWinnersState: String, // String of 9 characters: 'X', 'O', 'D', '.'
    val currentPlayer: String, // "X" or "O"
    val targetBoard: Int?, // -1 or null to represent free play/no target, or 0..8
    val freePlay: Boolean,
    val gameMode: String, // "ai" or "pvp"
    val difficulty: String, // "easy", "medium", "hard"
    val undoHistoryState: String // Joined states of cell strings separated by '|' for undoing
)
