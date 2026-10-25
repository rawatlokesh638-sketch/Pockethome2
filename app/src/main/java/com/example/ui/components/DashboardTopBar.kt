package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserLevelTier
import com.example.ui.AppTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    currentTab: AppTab,
    userTier: UserLevelTier = UserLevelTier.IRON,
    userXp: Int = 0,
    onTabSelected: (AppTab) -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Pockethome", fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(userTier.hexColor).copy(alpha = 0.18f),
                    modifier = Modifier.clickable { onTabSelected(AppTab.LEADERBOARD) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(userTier.emoji, fontSize = 12.sp)
                        Text(
                            text = userTier.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(userTier.hexColor)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AppTab.values().forEach { tab ->
                    val label = when (tab) {
                        AppTab.HOME -> "Home 🏠"
                        AppTab.TRANSACTIONS -> "Expenses 💸"
                        AppTab.REPORTS -> "Reports 📊"
                        AppTab.BILLS -> "Bills RECEIPT 🧾"
                        AppTab.BUDGET -> "Budget 🎯"
                        AppTab.CHAT -> "AI Chat 🤖"
                        AppTab.LEADERBOARD -> "Leaderboard & Levels 🏆"
                    }
                    DropdownMenuItem(
                        text = { Text(label, fontWeight = if (tab == currentTab) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            expanded = false
                            onTabSelected(tab)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Privacy Policy & Terms 📜") },
                    onClick = {
                        expanded = false
                        onOpenPrivacyPolicy()
                    }
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenPrivacyPolicy) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Policy & Zero Sharing",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onTabSelected(AppTab.LEADERBOARD) }) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "Leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

