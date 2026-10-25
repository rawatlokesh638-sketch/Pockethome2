package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeaderboardEntry
import com.example.model.UserLevelTier
import com.example.ui.DashboardUiState
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    uiState: DashboardUiState,
    onBack: () -> Unit,
    onOpenProSheet: () -> Unit
) {
    val currentTier = uiState.userTier
    val userXp = uiState.userXp
    val leaderboard = uiState.leaderboardEntries
    val top3 = leaderboard.take(3)
    val remainingLeaderboard = leaderboard.drop(3)

    val nextTierXp = if (currentTier.maxXp == Int.MAX_VALUE) currentTier.minXp else currentTier.maxXp
    val progressPercent = if (currentTier.maxXp == Int.MAX_VALUE) 1.0f else {
        ((userXp - currentTier.minXp).toFloat() / (currentTier.maxXp - currentTier.minXp).toFloat()).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Financial Level & Leaderboard 🏆", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("leaderboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Level & XP Progress Hero Card
            item(key = "current_level_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(currentTier.hexColor).copy(alpha = 0.2f))
                                        .border(2.dp, Color(currentTier.hexColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = currentTier.emoji, fontSize = 26.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentTier.displayName,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(currentTier.hexColor).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Lvl ${currentTier.levelNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(currentTier.hexColor),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentTier.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$userXp XP",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = RoyalBlue
                                )
                                Text(
                                    text = "Score: ${uiState.financialHealthScoreInt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Next Tier Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (currentTier == UserLevelTier.GOD) "MAX LEVEL ⚡" else "$userXp / $nextTierXp XP",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(currentTier.hexColor),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoyalBlue.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Earn +150 XP by saving 20%+ of your income & keeping budget on track!",
                                style = MaterialTheme.typography.bodySmall,
                                color = RoyalBlue
                            )
                        }
                    }
                }
            }

            // 2. All 10 Level Tiers Roadmap Carousel
            item(key = "level_tiers_carousel") {
                Column {
                    Text(
                        text = "10 Financial Level Tiers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(UserLevelTier.values()) { tier ->
                            val isUnlocked = userXp >= tier.minXp
                            val isCurrent = tier == currentTier

                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) Color(tier.hexColor) else if (isUnlocked) Color(tier.hexColor).copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(tier.hexColor).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) Color(tier.hexColor).copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isUnlocked) {
                                            Text(tier.emoji, fontSize = 20.sp)
                                        } else {
                                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = tier.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = "Lvl ${tier.levelNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(tier.hexColor)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${tier.minXp} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Top Podium Section (Pure Firebase RTDB Users)
            item(key = "podium_section") {
                if (top3.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Firebase Live Rankings 🏆",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MoneyGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Firebase RTDB Live",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MoneyGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 2nd Place
                            if (top3.size >= 2) {
                                PodiumCard(entry = top3[1], place = 2, modifier = Modifier.weight(1f))
                            }
                            // 1st Place (Center, Tallest)
                            if (top3.isNotEmpty()) {
                                PodiumCard(entry = top3[0], place = 1, modifier = Modifier.weight(1.1f))
                            }
                            // 3rd Place
                            if (top3.size >= 3) {
                                PodiumCard(entry = top3[2], place = 3, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 4. Remaining Leaderboard List
            item(key = "leaderboard_header") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leaderboard Ranks (${leaderboard.size} Registered Users)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoyalBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Synced via Firebase",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RoyalBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (remainingLeaderboard.isEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Firebase Realtime Database par naye users ke sign-up/transactions karte hi unki live ranks yahan auto-sync hongi! 🚀",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            items(remainingLeaderboard, key = { it.userId }) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
private fun PodiumCard(entry: LeaderboardEntry, place: Int, modifier: Modifier = Modifier) {
    val (badgeColor, borderBrush, placeEmoji) = when (place) {
        1 -> Triple(Color(0xFFFFD700), Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))), "🥇 1st")
        2 -> Triple(Color(0xFFC0C0C0), Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E))), "🥈 2nd")
        else -> Triple(Color(0xFFCD7F32), Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFA1887F))), "🥉 3rd")
    }

    Card(
        modifier = modifier
            .border(2.dp, borderBrush, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = placeEmoji, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = badgeColor)

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = entry.tierEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.handleName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${entry.xpPoints} XP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = RoyalBlue
            )
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val isUser = entry.isCurrentUser
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUser) Modifier.border(1.5.dp, RoyalBlue, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) RoyalBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isUser) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = entry.tierEmoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.handleName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoyalBlue
                            ) {
                                Text(
                                    text = "YOU",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${entry.tierName} • Health Score ${entry.financialHealthScore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${entry.xpPoints} XP",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RoyalBlue
            )
        }
    }
}
