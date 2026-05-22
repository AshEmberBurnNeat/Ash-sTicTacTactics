package com.example.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MatchHistory
import com.example.data.SavedGame
import com.example.data.TacticsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class TacticsViewModel(
    application: Application,
    private val repository: TacticsRepository
) : AndroidViewModel(application) {

    // Move history item representing previous states for Undo
    data class HistoryItem(
        val cells: List<List<String?>>,
        val boardWinners: List<String?>,
        val currentPlayer: String,
        val targetBoard: Int?,
        val freePlay: Boolean
    )

    private val _cells = MutableStateFlow<List<List<String?>>>(List(9) { List(9) { null } })
    val cells: StateFlow<List<List<String?>>> = _cells.asStateFlow()

    private val _boardWinners = MutableStateFlow<List<String?>>(List(9) { null })
    val boardWinners: StateFlow<List<String?>> = _boardWinners.asStateFlow()

    private val _currentPlayer = MutableStateFlow("X")
    val currentPlayer: StateFlow<String> = _currentPlayer.asStateFlow()

    private val _targetBoard = MutableStateFlow<Int?>(null)
    val targetBoard: StateFlow<Int?> = _targetBoard.asStateFlow()

    private val _freePlay = MutableStateFlow(true)
    val freePlay: StateFlow<Boolean> = _freePlay.asStateFlow()

    private val _gameOver = MutableStateFlow(false)
    val gameOver: StateFlow<Boolean> = _gameOver.asStateFlow()

    private val _winner = MutableStateFlow<String?>(null) // "X", "O", "draw", or null
    val winner: StateFlow<String?> = _winner.asStateFlow()

    private val _aiThinking = MutableStateFlow(false)
    val aiThinking: StateFlow<Boolean> = _aiThinking.asStateFlow()

    private val _gameMode = MutableStateFlow<String?>(null) // "pvp", "ai", or null (home menu)
    val gameMode: StateFlow<String?> = _gameMode.asStateFlow()

    private val _difficulty = MutableStateFlow("easy") // "easy", "medium", "hard"
    val difficulty: StateFlow<String> = _difficulty.asStateFlow()

    private val _showRules = MutableStateFlow(false)
    val showRules: StateFlow<Boolean> = _showRules.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val moveHistory = mutableListOf<HistoryItem>()

    // Expose histories from Room
    val historyLog: StateFlow<List<MatchHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            this.viewModelScope.run { SharingStarted.WhileSubscribed(5000) },
            initialValue = emptyList()
        )

    private val winLines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
        listOf(0, 4, 8), listOf(2, 4, 6) // diagonals
    )

    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorFailed = false

    init {
        loadSavedGame()
    }

    private fun playSynthSound(toneType: Int, durationMs: Int) {
        if (!_soundEnabled.value || toneGeneratorFailed) return
        viewModelScope.launch {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                }
                toneGenerator?.startTone(toneType, durationMs)
            } catch (t: Throwable) {
                toneGeneratorFailed = true
                Log.e("TacticsVM", "ToneGenerator failed or unsupported on this platform; sound plays disabled.", t)
                try {
                    toneGenerator?.release()
                } catch (ignored: Throwable) {}
                toneGenerator = null
            }
        }
    }

    fun playMoveSound() {
        playSynthSound(ToneGenerator.TONE_PROP_BEEP, 80)
    }

    fun playWinMicroSound() {
        playSynthSound(ToneGenerator.TONE_CDMA_PIP, 150)
    }

    fun playWinGameSound() {
        playSynthSound(ToneGenerator.TONE_CDMA_HIGH_L, 400)
    }

    fun playDrawSound() {
        playSynthSound(ToneGenerator.TONE_PROP_PROMPT, 300)
    }

    fun playErrorSound() {
        playSynthSound(ToneGenerator.TONE_PROP_NACK, 120)
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    fun selectMode(mode: String) {
        _gameMode.value = mode
        if (mode == "pvp") {
            initNewGame()
        }
        playMoveSound()
    }

    fun setDifficulty(diff: String) {
        _difficulty.value = diff
        playMoveSound()
    }

    fun showRules() {
        _showRules.value = true
        playMoveSound()
    }

    fun hideRules() {
        _showRules.value = false
        playMoveSound()
    }

    fun exitToMenu() {
        _gameMode.value = null
        _gameOver.value = false
        _winner.value = null
        moveHistory.clear()
        playMoveSound()
    }

    fun restartGame() {
        initNewGame()
        playMoveSound()
    }

    private fun initNewGame() {
        _cells.value = List(9) { List(9) { null } }
        _boardWinners.value = List(9) { null }
        _currentPlayer.value = "X"
        _targetBoard.value = null
        _freePlay.value = true
        _gameOver.value = false
        _winner.value = null
        _aiThinking.value = false
        moveHistory.clear()
        viewModelScope.launch {
            repository.deleteSavedGame()
        }
    }

    fun selectCell(boardIndex: Int, cellIndex: Int) {
        if (_gameOver.value || _aiThinking.value) return
        if (_gameMode.value == "ai" && _currentPlayer.value != "X") return

        val currentBoardCells = _cells.value[boardIndex]
        if (currentBoardCells[cellIndex] != null || _boardWinners.value[boardIndex] != null) {
            playErrorSound()
            return
        }

        if (!isBoardPlayable(boardIndex)) {
            playErrorSound()
            return
        }

        applyMove(boardIndex, cellIndex, _currentPlayer.value)
    }

    private fun isBoardPlayable(boardIndex: Int): Boolean {
        if (_boardWinners.value[boardIndex] != null) return false
        if (_freePlay.value || _targetBoard.value == null) return true
        return boardIndex == _targetBoard.value
    }

    private fun applyMove(boardIndex: Int, cellIndex: Int, player: String) {
        // Record current state into history before making move
        moveHistory.add(
            HistoryItem(
                cells = _cells.value.map { it.toList() },
                boardWinners = _boardWinners.value.toList(),
                currentPlayer = _currentPlayer.value,
                targetBoard = _targetBoard.value,
                freePlay = _freePlay.value
            )
        )

        // Apply move
        val updatedCells = _cells.value.map { board -> board.toMutableList() }
        updatedCells[boardIndex][cellIndex] = player
        _cells.value = updatedCells.map { it.toList() }
        playMoveSound()

        // Check micro winner
        val microWin = checkBoardWinner(updatedCells[boardIndex])
        val originalWinner = _boardWinners.value[boardIndex]
        if (microWin != null) {
            val updatedWinners = _boardWinners.value.toMutableList()
            updatedWinners[boardIndex] = microWin
            _boardWinners.value = updatedWinners.toList()
            if (originalWinner != microWin) {
                playWinMicroSound()
            }
        } else if (updatedCells[boardIndex].all { it != null }) {
            val updatedWinners = _boardWinners.value.toMutableList()
            updatedWinners[boardIndex] = "draw"
            _boardWinners.value = updatedWinners.toList()
            playDrawSound()
        }

        // Check macro winner
        val macroWin = checkBoardWinner(_boardWinners.value)
        if (macroWin != null && macroWin != "draw") {
            endGame(macroWin)
            return
        } else if (_boardWinners.value.all { it != null }) {
            endGame("draw")
            return
        }

        // Toggle player
        val nextPlayer = if (player == "X") "O" else "X"
        _currentPlayer.value = nextPlayer

        // Set target board for next player
        if (_boardWinners.value[cellIndex] != null) {
            _targetBoard.value = null
            _freePlay.value = true
        } else {
            _targetBoard.value = cellIndex
            _freePlay.value = false
        }

        // Auto save game after every move that hasn't ended the game
        saveGameProgress()

        // If Mode is AI and it's O's turn, launch computer move
        if (_gameMode.value == "ai" && nextPlayer == "O" && !_gameOver.value) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        _aiThinking.value = true
        viewModelScope.launch {
            val delayDuration = when (_difficulty.value) {
                "hard" -> 850L
                "medium" -> 600L
                else -> 400L
            }
            delay(delayDuration)
            executeAiMove()
        }
    }

    private fun executeAiMove() {
        if (_gameOver.value) {
            _aiThinking.value = false
            return
        }

        val legalMoves = getLegalMoves()
        if (legalMoves.isEmpty()) {
            _aiThinking.value = false
            return
        }

        val move = when (_difficulty.value) {
            "easy" -> legalMoves[Random.nextInt(legalMoves.size)]
            "medium" -> {
                if (Random.nextFloat() < 0.5f) {
                    pickSmartMove(legalMoves)
                } else {
                    legalMoves[Random.nextInt(legalMoves.size)]
                }
            }
            else -> pickSmartMove(legalMoves) // hard
        }

        _aiThinking.value = false
        applyMove(move.boardIndex, move.cellIndex, "O")
    }

    data class Move(val boardIndex: Int, val cellIndex: Int)

    private fun getLegalMoves(): List<Move> {
        val moves = mutableListOf<Move>()
        for (b in 0 until 9) {
            if (!isBoardPlayable(b)) continue
            for (c in 0 until 9) {
                if (_cells.value[b][c] == null) {
                    moves.add(Move(b, c))
                }
            }
        }
        return moves
    }

    private fun pickSmartMove(legal: List<Move>): Move {
        // 1. Win a mini-board if possible
        for (m in legal) {
            val sim = _cells.value[m.boardIndex].toMutableList()
            sim[m.cellIndex] = "O"
            if (checkBoardWinner(sim) == "O") return m
        }

        // 2. Block opponent from winning a mini-board
        for (m in legal) {
            val sim = _cells.value[m.boardIndex].toMutableList()
            sim[m.cellIndex] = "X"
            if (checkBoardWinner(sim) == "X") return m
        }

        // 3. Prefer center of any board
        val centers = legal.filter { it.cellIndex == 4 }
        if (centers.isNotEmpty()) {
            return centers[Random.nextInt(centers.size)]
        }

        // 4. Random from legal
        return legal[Random.nextInt(legal.size)]
    }

    fun undoMove() {
        if (_gameOver.value || _aiThinking.value || moveHistory.isEmpty()) return

        if (_gameMode.value == "ai") {
            // Must go back 2 moves: discard O's state, load pre-X state
            if (moveHistory.size < 2) return
            moveHistory.removeAt(moveHistory.size - 1) // pop O's step
        }

        val prev = moveHistory.removeAt(moveHistory.size - 1) // pop X's step
        _cells.value = prev.cells
        _boardWinners.value = prev.boardWinners
        _currentPlayer.value = prev.currentPlayer
        _targetBoard.value = prev.targetBoard
        _freePlay.value = prev.freePlay
        _gameOver.value = false
        _winner.value = null

        playMoveSound()
        saveGameProgress()
    }

    private fun checkBoardWinner(board: List<String?>): String? {
        for (line in winLines) {
            val a = board[line[0]]
            val b = board[line[1]]
            val c = board[line[2]]
            if (a != null && a != "draw" && a == b && a == c) {
                return a
            }
        }
        return null
    }

    private fun endGame(winningSymbol: String) {
        _gameOver.value = true
        _winner.value = winningSymbol

        if (winningSymbol == "X" || winningSymbol == "O") {
            playWinGameSound()
        } else {
            playDrawSound()
        }

        viewModelScope.launch {
            // Delete saved game state because match is completed
            repository.deleteSavedGame()

            // Save match to database logs
            val modeForLogs = if (_gameMode.value == "ai") {
                "AI_${_difficulty.value.uppercase()}"
            } else {
                "PVP"
            }
            repository.insertMatch(
                MatchHistory(
                    gameMode = modeForLogs,
                    winner = winningSymbol
                )
            )
        }
    }

    private fun saveGameProgress() {
        viewModelScope.launch {
            val cellsStateStr = _cells.value.flatten().map { it ?: "." }.joinToString("")
            val winnersStateStr = _boardWinners.value.map { it?.firstOrNull()?.uppercaseChar() ?: '.' }.joinToString("")
            val undoHistoryStr = moveHistory.joinToString("|") { item ->
                item.cells.flatten().map { it ?: "." }.joinToString("")
            }

            repository.saveGame(
                SavedGame(
                    cellsState = cellsStateStr,
                    boardWinnersState = winnersStateStr,
                    currentPlayer = _currentPlayer.value,
                    targetBoard = _targetBoard.value ?: -1,
                    freePlay = _freePlay.value,
                    gameMode = _gameMode.value ?: "pvp",
                    difficulty = _difficulty.value,
                    undoHistoryState = undoHistoryStr
                )
            )
        }
    }

    private fun loadSavedGame() {
        viewModelScope.launch {
            try {
                val saved = repository.getSavedGame() ?: return@launch

                _difficulty.value = saved.difficulty
                _gameMode.value = saved.gameMode
                _currentPlayer.value = saved.currentPlayer
                _freePlay.value = saved.freePlay
                _targetBoard.value = if (saved.targetBoard == -1) null else saved.targetBoard

                // Load board grid
                val listCells = mutableListOf<List<String?>>()
                for (b in 0 until 9) {
                    val sub = mutableListOf<String?>()
                    for (c in 0 until 9) {
                        val char = saved.cellsState.getOrNull(b * 9 + c) ?: '.'
                        sub.add(if (char == '.') null else char.toString())
                    }
                    listCells.add(sub)
                }
                _cells.value = listCells

                // Load board winners
                val listWinners = saved.boardWinnersState.map { char ->
                    when (char) {
                        '.' -> null
                        'X' -> "X"
                        'O' -> "O"
                        'D' -> "draw"
                        else -> null
                    }
                }
                _boardWinners.value = listWinners

                // Rebuild history list
                moveHistory.clear()
                if (saved.undoHistoryState.isNotEmpty()) {
                    val itemStrList = saved.undoHistoryState.split("|")
                    for (itemStr in itemStrList) {
                        if (itemStr.length != 81) continue
                        val histCells = mutableListOf<List<String?>>()
                        for (b in 0 until 9) {
                            val sub = mutableListOf<String?>()
                            for (c in 0 until 9) {
                                val char = itemStr.getOrNull(b * 9 + c) ?: '.'
                                sub.add(if (char == '.') null else char.toString())
                            }
                            histCells.add(sub)
                        }
                        // For historical items, guess turn and target boards sequence (approx, mainly cells are important)
                        moveHistory.add(
                            HistoryItem(
                                cells = histCells,
                                boardWinners = guessWinnersForCells(histCells),
                                currentPlayer = if (saved.gameMode == "ai") "X" else "O", // placeholders but safe
                                targetBoard = null,
                                freePlay = true
                            )
                        )
                    }
                }

                // Ensure we handle AI state if turn got saved during AI turn
                if (saved.gameMode == "ai" && saved.currentPlayer == "O") {
                    triggerAiMove()
                }
            } catch (t: Throwable) {
                Log.e("TacticsVM", "Failed to load saved game securely: ${t.message}")
            }
        }
    }

    private fun guessWinnersForCells(grids: List<List<String?>>): List<String?> {
        return grids.map { grid ->
            val w = checkBoardWinner(grid)
            if (w != null) w
            else if (grid.all { it != null }) "draw"
            else null
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            try {
                repository.clearHistory()
                playDrawSound()
            } catch (t: Throwable) {
                Log.e("TacticsVM", "Failed to clear statistics securely: ${t.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}

class TacticsViewModelFactory(
    private val application: Application,
    private val repository: TacticsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TacticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TacticsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
