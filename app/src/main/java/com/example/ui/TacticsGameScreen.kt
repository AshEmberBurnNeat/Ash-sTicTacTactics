package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchHistory
import com.example.ui.theme.*
import com.example.viewmodel.TacticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TacticsGameScreen(
    viewModel: TacticsViewModel,
    modifier: Modifier = Modifier
) {
    val gameMode by viewModel.gameMode.collectAsState()
    val cells by viewModel.cells.collectAsState()
    val boardWinners by viewModel.boardWinners.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val targetBoard by viewModel.targetBoard.collectAsState()
    val freePlay by viewModel.freePlay.collectAsState()
    val gameOver by viewModel.gameOver.collectAsState()
    val winner by viewModel.winner.collectAsState()
    val aiThinking by viewModel.aiThinking.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val showRules by viewModel.showRules.collectAsState()
    val historyLog by viewModel.historyLog.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Background Canvas FX configuration (floating grid lines and neon dust particles)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .drawBehind {
                // Subtle static tech scanning lines
                val scanLineGap = 16f
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color(0x0C8F8FBF),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += scanLineGap
                }

                // Cyber atmospheric ambient neon corners
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x19FF3D6B), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(0f, 0f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1900E5FF), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width, size.height)
                )
            }
    ) {
        Crossfade(
            targetState = gameMode,
            animationSpec = tween(350, easing = LinearOutSlowInEasing),
            label = "screen_transition"
        ) { mode ->
            if (mode == null) {
                // 1. HOME SCREEN / MODE SELECT
                ModeSelectScreen(
                    difficulty = difficulty,
                    history = historyLog,
                    soundEnabled = soundEnabled,
                    onSelectMode = { m ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectMode(m)
                    },
                    onSetDifficulty = { d ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setDifficulty(d)
                    },
                    onToggleSound = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleSound()
                    },
                    onClearStats = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearStats()
                    },
                    onShowRules = { viewModel.showRules() }
                )
            } else {
                // 2. GAME BOARD SCREEN
                GameScreen(
                    cells = cells,
                    boardWinners = boardWinners,
                    currentPlayer = currentPlayer,
                    targetBoard = targetBoard,
                    freePlay = freePlay,
                    aiThinking = aiThinking,
                    gameMode = mode,
                    difficulty = difficulty,
                    historyLog = historyLog,
                    soundEnabled = soundEnabled,
                    onCellClick = { b, c ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectCell(b, c)
                    },
                    onUndo = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.undoMove()
                    },
                    onReset = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.restartGame()
                    },
                    onExit = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.exitToMenu()
                    },
                    onShowRules = { viewModel.showRules() },
                    onToggleSound = { viewModel.toggleSound() }
                )
            }
        }

        // 3. GAME OVER OVERLAY
        AnimatedVisibility(
            visible = gameOver,
            enter = fadeIn(tween(400)) + scaleIn(tween(450, easing = LinearOutSlowInEasing)),
            exit = fadeOut(tween(300)) + scaleOut(tween(300))
        ) {
            WinOverlay(
                winner = winner ?: "draw",
                gameMode = gameMode ?: "pvp",
                onPlayAgain = { viewModel.restartGame() },
                onExit = { viewModel.exitToMenu() }
            )
        }

        // 4. RULES DIALOG OVERLAY
        AnimatedVisibility(
            visible = showRules,
            enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 3 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 3 }
        ) {
            RulesOverlay(
                onDismiss = { viewModel.hideRules() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MODE SELECT SCREEN COPMOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ModeSelectScreen(
    difficulty: String,
    history: List<MatchHistory>,
    soundEnabled: Boolean,
    onSelectMode: (String) -> Unit,
    onSetDifficulty: (String) -> Unit,
    onToggleSound: () -> Unit,
    onClearStats: () -> Unit,
    onShowRules: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App header / Sound Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShowRules,
                modifier = Modifier.testTag("home_rules_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Rules",
                    tint = CyberBorderBright
                )
            }

            IconButton(onClick = onToggleSound) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Sound Controller",
                    tint = if (soundEnabled) NeonO else CyberMuted
                )
            }
        }

        // Cyber Logo Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = "ASH'S TIC TAC",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    color = CyberText,
                    shadow = Shadow(color = NeonXGlow, blurRadius = 12f)
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "TACTICS",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 62.sp,
                    shadow = Shadow(color = NeonOGlow, blurRadius = 30f),
                    brush = Brush.horizontalGradient(listOf(NeonX, Color(0xFFFF9F43), NeonO))
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "9 BOARDS • 81 CELLS • ONE CHAMPION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberMuted,
                    letterSpacing = 3.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Selection Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Local PvP Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectMode("pvp") }
                    .testTag("mode_pvp_button"),
                color = CyberPanel
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "X VS O",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = NeonX,
                                shadow = Shadow(NeonXGlow, blurRadius = 8f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2 Players local hotseat match",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 11.sp,
                                color = CyberMuted
                            )
                        )
                    }
                    Text(
                        text = "▶",
                        style = MaterialTheme.typography.titleLarge.copy(color = NeonX)
                    )
                }
            }

            // VS Computer AI Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectMode("ai") }
                    .testTag("mode_ai_button"),
                color = CyberPanel
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VS SYSTEM AI",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = NeonO,
                                shadow = Shadow(NeonOGlow, blurRadius = 8f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1 Player puzzle — Play as X against Smart AI O",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 11.sp,
                                color = CyberMuted
                            )
                        )
                    }
                    Text(
                        text = "▶",
                        style = MaterialTheme.typography.titleLarge.copy(color = NeonO)
                    )
                }
            }

            // AI DIFFICULTY MODE CHANGER Row
            Text(
                text = "COMPUTER SYSTEMS INTELLECT",
                style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted, letterSpacing = 2.sp),
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("easy", "medium", "hard").forEach { d ->
                    val isActive = d == difficulty
                    val activeColor = when (d) {
                        "easy" -> Color(0xFF6CF08C)
                        "medium" -> NeonGold
                        else -> NeonX
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isActive) activeColor else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSetDifficulty(d) }
                            .padding(vertical = 10.dp)
                            .testTag("diff_${d}_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isActive) activeColor else CyberMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Live Log history / stats terminal panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(180.dp)
                .background(CyberPanel, RoundedCornerShape(10.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIC COMBAT STATS TERMINAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = onClearStats,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Purge stats",
                            tint = NeonX,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO HISTORIC BATTLES REGISTERED YET\nPLAY A MATCH TO LOG DIAGNOSTICS",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CyberMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Compile summaries of stats
                val pvpMatches = history.filter { it.gameMode == "PVP" }
                val aiMatches = history.filter { it.gameMode.startsWith("AI") }

                val pvpWinsX = pvpMatches.count { it.winner == "X" }
                val pvpWinsO = pvpMatches.count { it.winner == "O" }
                val pvpDraws = pvpMatches.count { it.winner == "DRAW" }

                val aiWinsYou = aiMatches.count { it.winner == "X" }
                val aiWinsAI = aiMatches.count { it.winner == "O" }
                val aiDraws = aiMatches.count { it.winner == "DRAW" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "LOCAL NET PVP:",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberText)
                            )
                            Text(
                                "X ($pvpWinsX)  •  O ($pvpWinsO)  •  DRAWS ($pvpDraws)",
                                style = MaterialTheme.typography.labelSmall.copy(color = NeonX)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "SYSTEM ENGINE AI:",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberText)
                            )
                            Text(
                                "YOU ($aiWinsYou)  •  AI ($aiWinsAI)  •  DRAWS ($aiDraws)",
                                style = MaterialTheme.typography.labelSmall.copy(color = NeonO)
                            )
                        }
                    }
                    item {
                        Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    item {
                        Text(
                            "RECENT SYSTEM ENGAGEMENTS logs:",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted, fontSize = 9.sp)
                        )
                    }

                    items(history.take(4)) { match ->
                        val dateString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "[$dateString] ${match.gameMode}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = CyberMuted)
                            )
                            val statusColor = when (match.winner) {
                                "X" -> NeonX
                                "O" -> NeonO
                                else -> CyberMuted
                            }
                            Text(
                                "WINNER: ${match.winner}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = statusColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GAME SCREEN COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GameScreen(
    cells: List<List<String?>>,
    boardWinners: List<String?>,
    currentPlayer: String,
    targetBoard: Int?,
    freePlay: Boolean,
    aiThinking: Boolean,
    gameMode: String,
    difficulty: String,
    historyLog: List<MatchHistory>,
    soundEnabled: Boolean,
    onCellClick: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    onShowRules: () -> Unit,
    onToggleSound: () -> Unit
) {
    // Collect active session score dynamic logic from Room matches
    val currentPvPMatches = historyLog.filter { it.gameMode == "PVP" }
    val currentAiMatches = historyLog.filter { it.gameMode.startsWith("AI") }

    val scoreX = if (gameMode == "pvp") currentPvPMatches.count { it.winner == "X" } else currentAiMatches.count { it.winner == "X" }
    val scoreO = if (gameMode == "pvp") currentPvPMatches.count { it.winner == "O" } else currentAiMatches.count { it.winner == "O" }
    val scoreDraw = if (gameMode == "pvp") currentPvPMatches.count { it.winner == "DRAW" } else currentAiMatches.count { it.winner == "DRAW" }

    val titleText = if (gameMode == "ai") "TIC TAC TACTICS" else "LOCAL MULTIPLAYER PVP"
    val modeChipText = if (gameMode == "ai") "AI (${difficulty.uppercase(Locale.getDefault())})" else "2P PVP"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP CONTROL HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 18.sp,
                        color = Color.White,
                        shadow = Shadow(color = NeonXGlow, blurRadius = 4f)
                    )
                )
                Text(
                    text = "BATTLE ARENA ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted, fontSize = 9.sp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onToggleSound,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Synth Sound Switcher",
                        tint = if (soundEnabled) NeonO else CyberMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, CyberBorderBright),
                    color = CyberPanel,
                    modifier = Modifier.clickable { onExit() }
                ) {
                    Text(
                        text = "EXIT ↩",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonX,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // SCOREBOARD MODULE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .background(CyberPanel, RoundedCornerShape(10.dp))
                .border(2.dp, CyberBorder, RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player X score
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (gameMode == "ai") "YOU" else "PLAYER 1",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$scoreX",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 28.sp,
                        color = NeonX,
                        shadow = Shadow(color = NeonXGlow, blurRadius = 8f)
                    )
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(45.dp)
                    .background(CyberBorder)
            )

            // Draws
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRAWS",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$scoreDraw",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 28.sp,
                        color = CyberMuted
                    )
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(45.dp)
                    .background(CyberBorder)
            )

            // Player O score
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (gameMode == "ai") "SYSTEM AI" else "PLAYER 2",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberMuted)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$scoreO",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 28.sp,
                        color = NeonO,
                        shadow = Shadow(color = NeonOGlow, blurRadius = 8f)
                    )
                )
            }
        }

        // TACTICAL MOVEMENT STATUS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .background(CyberPanel, RoundedCornerShape(8.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Turn representation Pill Left (X)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(
                        if (currentPlayer == "X" && !aiThinking) NeonXDim else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (currentPlayer == "X" && !aiThinking) NeonX else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 16.sp,
                        color = NeonX,
                        shadow = Shadow(NeonXGlow, blurRadius = 6f)
                    )
                )
                Text(
                    text = if (gameMode == "ai") "YOU" else "P1",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 10.sp,
                        color = if (currentPlayer == "X") Color.White else CyberMuted
                    )
                )
            }

            // Intermediate prompt/direction
            Crossfade(
                targetState = Triple(aiThinking, freePlay, targetBoard),
                label = "status_text_crossfade"
            ) { (thinking, isFree, target) ->
                if (thinking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "AI GENERATING STRATEGY",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = NeonO,
                                fontSize = 10.sp,
                                shadow = Shadow(NeonOGlow, blurRadius = 4f)
                            )
                        )
                    }
                } else if (isFree) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x3BFFD166), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "★ FREE MOVE ANYWHERE ★",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = NeonGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(Color(0xBBFFD166), blurRadius = 8f)
                            )
                        )
                    }
                } else if (target != null) {
                    Text(
                        text = "LOCK TARGET BOARD ${target + 1}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = NeonGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    Text(
                        text = "YOUR DEPLOYMENT TURNS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = CyberText,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Turn representation Pill Right (O)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(
                        if (currentPlayer == "O" || aiThinking) NeonODim else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (currentPlayer == "O" || aiThinking) NeonO else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (gameMode == "ai") "AI" else "P2",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 10.sp,
                        color = if (currentPlayer == "O") Color.White else CyberMuted
                    )
                )
                Text(
                    text = "O",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 16.sp,
                        color = NeonO,
                        shadow = Shadow(NeonOGlow, blurRadius = 6f)
                    )
                )
            }
        }

        // MACRO GRID DEPLOYMENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .aspectRatio(1f)
                .background(CyberPanel, RoundedCornerShape(12.dp))
                .border(2.dp, CyberBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw secondary background grid markings directly
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = size.width / 3
                // vertical lines
                drawLine(
                    color = CyberBorder,
                    start = Offset(gridSpacing, 0f),
                    end = Offset(gridSpacing, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = CyberBorder,
                    start = Offset(gridSpacing * 2, 0f),
                    end = Offset(gridSpacing * 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                // horizontal lines
                drawLine(
                    color = CyberBorder,
                    start = Offset(0f, gridSpacing),
                    end = Offset(size.width, gridSpacing),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = CyberBorder,
                    start = Offset(0f, gridSpacing * 2),
                    end = Offset(size.width, gridSpacing * 2),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0 until 3) {
                            val boardIdx = row * 3 + col
                            val isPlayable = (freePlay || targetBoard == boardIdx) &&
                                    boardWinners[boardIdx] == null &&
                                    !aiThinking

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                MiniBoardView(
                                    boardIndex = boardIdx,
                                    cells = cells[boardIdx],
                                    winner = boardWinners[boardIdx],
                                    isPlayable = isPlayable,
                                    currentPlayer = currentPlayer,
                                    onCellClick = { cellIdx -> onCellClick(boardIdx, cellIdx) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM ARENA INTERACTIVE BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onUndo,
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .testTag("undo_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberSurface,
                    disabledContainerColor = CyberSurface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(8.dp),
                enabled = !aiThinking
            ) {
                Text(
                    text = "← UNDO",
                    style = MaterialTheme.typography.labelLarge.copy(color = if (!aiThinking) CyberText else CyberMuted)
                )
            }

            Button(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("reset_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RESET",
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonGold)
                )
            }

            Button(
                onClick = onShowRules,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("rules_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RULES",
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonO)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MINI BOARD GRID COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MiniBoardView(
    boardIndex: Int,
    cells: List<String?>,
    winner: String?,
    isPlayable: Boolean,
    currentPlayer: String,
    onCellClick: (Int) -> Unit
) {
    // Dynamic border color indicator showing playable boards
    val infiniteTransition = rememberInfiniteTransition(label = "playable_pulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_animation"
    )

    val borderStrokeColor = when {
        winner != null -> Color.Transparent
        isPlayable && currentPlayer == "X" -> NeonX.copy(alpha = pulseBorderAlpha)
        isPlayable && currentPlayer == "O" -> NeonO.copy(alpha = pulseBorderAlpha)
        else -> CyberBorder
    }

    val glowBrushColors = when {
        isPlayable && currentPlayer == "X" -> listOf(NeonXDim, Color.Transparent)
        isPlayable && currentPlayer == "O" -> listOf(NeonODim, Color.Transparent)
        else -> listOf(Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(
                borderStrokeWidth(winner, isPlayable),
                borderStrokeColor,
                RoundedCornerShape(8.dp)
            )
            .drawBehind {
                if (isPlayable) {
                    drawCircle(
                        brush = Brush.radialGradient(colors = glowBrushColors),
                        radius = size.width * 0.7f,
                        center = center
                    )
                }
            }
    ) {
        // Draw inner mini lines of tic tac toe
        Canvas(modifier = Modifier.fillMaxSize()) {
            val offset = size.width / 3f
            // horizontal line cuts
            drawLine(
                color = Color(0x1BFFFFFF),
                start = Offset(0f, offset),
                end = Offset(size.width, offset),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0x1BFFFFFF),
                start = Offset(0f, offset * 2),
                end = Offset(size.width, offset * 2),
                strokeWidth = 1.dp.toPx()
            )

            // vertical line cuts
            drawLine(
                color = Color(0x1BFFFFFF),
                start = Offset(offset, 0f),
                end = Offset(offset, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0x1BFFFFFF),
                start = Offset(offset * 2, 0f),
                end = Offset(offset * 2, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Cells grid layout logic
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in 0 until 3) {
                        val cellIdx = r * 3 + c
                        val symbol = cells[cellIdx]

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .clickable(
                                    enabled = symbol == null && winner == null && isPlayable,
                                    onClick = { onCellClick(cellIdx) }
                                )
                                .testTag("cell_${boardIndex}_${cellIdx}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (symbol != null) {
                                val symbolColor = if (symbol == "X") NeonX else NeonO
                                val shadowColor = if (symbol == "X") NeonXGlow else NeonOGlow
                                Text(
                                    text = symbol,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = symbolColor,
                                        shadow = Shadow(color = shadowColor, blurRadius = 7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // LARGE SYMBOL OVERLAY WHENEVER THIS MINI BOARD IS WON BY PLAYER
        if (winner != null) {
            val textValue = if (winner == "draw") "—" else winner
            val winTextColor = when (winner) {
                "X" -> NeonX
                "O" -> NeonO
                else -> CyberMuted
            }
            val winGlowColor = when (winner) {
                "X" -> NeonXGlow
                "O" -> NeonOGlow
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (winner == "draw") 0.dp else 0.5.dp)
                    .background(
                        when (winner) {
                            "X" -> NeonXDim
                            "O" -> NeonODim
                            else -> CyberPanel.copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = textValue,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Black,
                        color = winTextColor,
                        shadow = Shadow(color = winGlowColor, blurRadius = 24f),
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
        }
    }
}

private fun borderStrokeWidth(winner: String?, isPlayable: Boolean): androidx.compose.ui.unit.Dp {
    return when {
        winner != null -> 0.dp
        isPlayable -> 2.dp
        else -> 1.dp
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WIN OVERLAY / DIALOG COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WinOverlay(
    winner: String,
    gameMode: String,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE005050C))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .background(CyberPanel, RoundedCornerShape(16.dp))
                .border(2.dp, CyberBorderBright, RoundedCornerShape(16.dp))
                .padding(28.dp)
        ) {
            val symbolColor = when (winner) {
                "X" -> NeonX
                "O" -> NeonO
                else -> CyberMuted
            }
            val symbolGlow = when (winner) {
                "X" -> NeonXGlow
                "O" -> NeonOGlow
                else -> Color.Transparent
            }

            Text(
                text = if (winner == "draw") "—" else winner,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    color = symbolColor,
                    shadow = Shadow(color = symbolGlow, blurRadius = 45f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            val winHeadline = when {
                winner == "draw" -> "STALEMATE"
                gameMode == "ai" && winner == "X" -> "YOU WIN!"
                gameMode == "ai" && winner == "O" -> "AI SYSTEM WINS"
                winner == "X" -> "X WINS THE BATTLE"
                else -> "O WINS THE BATTLE"
            }

            val winSubtitle = when {
                winner == "draw" -> "AN ABSOLUTE STRATEGIC STANDOFF"
                gameMode == "ai" && winner == "X" -> "ASH APPROVES 🔥"
                gameMode == "ai" && winner == "O" -> "METICULOUSLY CALCULATED BEAT"
                else -> "WELL CONTESTED ACTION"
            }

            Text(
                text = winHeadline,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    color = symbolColor,
                    shadow = Shadow(symbolGlow, blurRadius = 8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = winSubtitle.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberMuted,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("overlay_play_again"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonO),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PLAY AGAIN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = CyberBg,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Button(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("overlay_change_mode"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CHANGE MODE",
                        style = MaterialTheme.typography.labelLarge.copy(color = CyberText)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RULES OVERLAY / INFO DIALOG COPMOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RulesOverlay(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF205050C))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .background(CyberPanel, RoundedCornerShape(16.dp))
                .border(2.dp, CyberBorderBright, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "TACTICAL DIRECTIVES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 20.sp,
                    color = NeonGold,
                    shadow = Shadow(Color(0x7FFFFD166), blurRadius = 8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RuleBulletItem(text = "The ultimate macroboard houses 9 microboards. Claim victory in 3 aligned microboards to claim the master arena.")
                RuleBulletItem(text = "The targeted microboard quadrant corresponds EXACTLY to the cell quadrant selected by your opponent on their previous turn (e.g. top-right cell triggers top-right board).")
                RuleBulletItem(text = "If sent to a microboard that has ALREADY been closed, drawn, or fully filled, you obtain a FREE MOVE — permit deployment ANYWHERE.")
                RuleBulletItem(text = "If all boards fill up completely without an active alignment, the core software registers a DRAW.")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("dismiss_rules_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonO),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "UNDERSTOOD",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CyberBg,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun RuleBulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "▶",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = NeonGold,
                fontSize = 11.sp,
                shadow = Shadow(Color(0x8DFFD166), blurRadius = 4f)
            ),
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 11.5.sp,
                lineHeight = 16.5.sp,
                color = CyberText
            )
        )
    }
}
