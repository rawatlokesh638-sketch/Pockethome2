package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpenseCategories
import com.example.model.Transaction
import com.example.ui.DashboardUiState
import com.example.ui.DashboardViewModel
import com.example.ui.HomeTimeframe
import com.example.ui.components.BudgetOverviewCard
import com.example.ui.components.CategoryBreakdownSection
import com.example.ui.components.ChartBarData
import com.example.ui.components.ChartTimeframe
import com.example.ui.components.FinancialSummaryCards
import com.example.ui.components.SpendingTrendChart
import com.example.ui.components.UpcomingBillsSection
import com.example.ui.theme.BalanceBlue
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoyalBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun HomeScreen(
    uiState: DashboardUiState,
    proStatus: com.example.model.ProStatus,
    chartData: List<ChartBarData>,
    currentChartTimeframe: ChartTimeframe,
    onChartTimeframeChanged: (ChartTimeframe) -> Unit,
    onTimeframeChanged: (HomeTimeframe) -> Unit,
    onAddExpenseClicked: () -> Unit,
    onAddIncomeClicked: () -> Unit,
    onAddBillClicked: () -> Unit,
    onSetBudgetClicked: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onClearAllData: () -> Unit,
    onOpenProSheet: () -> Unit,
    onRequestSmsPermissions: () -> Unit,
    onTransactionClicked: (Transaction) -> Unit,
    onToggleBillPaid: (String, Boolean) -> Unit,
    onDeleteBill: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        val isPro = proStatus.isPro || proStatus.status == "ACTIVE"
        val statusText = when {
            isPro -> "PRO Member ⭐"
            proStatus.status == "PENDING" -> "Approval Pending... ⏳"
            else -> "Free Version"
        }

        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Profile & Account")
                }
            },
            text = { 
                Column {
                    Text(text = "User: ${uiState.userName}", fontWeight = FontWeight.Bold)
                    Text(text = "Email: ${Firebase.auth.currentUser?.email ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPro) MoneyGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPro) MoneyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Are you sure you want to logout?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onLogout()
                    showLogoutConfirm = false
                }) {
                    Text(text = "Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: "Hi User! 👋" + Profile Avatar + Pro Member Badge
        item(key = "header") {
            HomeTopHeader(
                userName = uiState.userName,
                dateDisplay = uiState.currentDateDisplay,
                proStatus = proStatus,
                onProClicked = onOpenProSheet,
                onProfileClicked = { showLogoutConfirm = true }
            )
        }

        // 2. All Transactions SMS Auto-Track Banner
        item(key = "sms_auto_track_banner") {
            AutoTrackSmsCard(
                isPro = proStatus.isPro || proStatus.status == "ACTIVE",
                hasSms = uiState.hasSmsPermission,
                scannedCount = uiState.scannedSmsCount,
                onRequestPermissions = {
                    if (proStatus.isPro || proStatus.status == "ACTIVE") {
                        onRequestSmsPermissions()
                    } else {
                        onOpenProSheet()
                    }
                }
            )
        }

        // 3. Timeframe Switcher: [ This Month ]  [ This Year ]
        item(key = "timeframe_pills") {
            TimeframePills(
                selectedTimeframe = uiState.homeTimeframe,
                onSelectTimeframe = onTimeframeChanged
            )
        }

        // 4. Financial Summary Cards (Income, Expense, Balance, Today's Spend & MoM comparison)
        item(key = "financial_summary") {
            FinancialSummaryCards(
                currentMonthExpenses = uiState.currentMonthExpenses,
                totalIncome = uiState.currentMonthIncome,
                remainingBalance = uiState.remainingBalance,
                todaySpending = uiState.todaySpending,
                monthOverMonthDiff = uiState.monthOverMonthDiff,
                monthOverMonthPercent = uiState.monthOverMonthPercent,
                isSpendHigherThanLastMonth = uiState.isSpendHigherThanLastMonth
            )
        }

        // 5. Monthly Budget Overview Card with Progress Bar and Edit Budget
        item(key = "budget_overview") {
            BudgetOverviewCard(
                currentMonthExpenses = uiState.currentMonthExpenses,
                monthlyBudget = uiState.monthlyBudget,
                budgetUsedPercent = uiState.budgetUsedPercent,
                onEditBudgetClicked = onSetBudgetClicked
            )
        }

        // 6. Quick Action 4 Buttons (Add Expense, Add Income, Add Bill, Set Budget)
        item(key = "quick_actions") {
            QuickActionsGrid(
                onAddExpense = onAddExpenseClicked,
                onAddIncome = onAddIncomeClicked,
                onAddBill = onAddBillClicked,
                onSetBudget = onSetBudgetClicked
            )
        }

        // 7. Spending Trend Overview Bar Chart (Daily / Weekly / Monthly)
        item(key = "spending_trend") {
            SpendingTrendChart(
                chartData = chartData,
                currentTimeframe = currentChartTimeframe,
                onTimeframeSelected = onChartTimeframeChanged
            )
        }

        // 8. Category-wise Spending Breakdown
        item(key = "category_breakdown") {
            CategoryBreakdownSection(
                breakdown = uiState.categoryBreakdown
            )
        }

        // 9. Upcoming Bills Reminders Section
        item(key = "upcoming_bills") {
            UpcomingBillsSection(
                bills = uiState.upcomingBills,
                onToggleBillPaid = onToggleBillPaid,
                onDeleteBill = onDeleteBill,
                onAddBillClicked = onAddBillClicked
            )
        }

        // 10. Recent Transactions Header
        item(key = "recent_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Haleeya kharche aur aay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onViewAllTransactions) {
                    Text(
                        text = "View All (${uiState.filteredTransactions.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue
                    )
                }
            }
        }

        // 11. Recent Transactions List or Clean Zero State
        if (uiState.recentTransactions.isEmpty()) {
            item(key = "empty_transactions") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Zero State",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sara data 0 hai! 0️⃣",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Naya kharcha jodein ya Bank SMS auto-sync karein.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    onClick = { onTransactionClicked(tx) }
                )
            }
        }

        // 12. "Reset All Data to 0" Button
        item(key = "reset_section") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = onClearAllData,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("reset_all_data_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset All Data to 0 (Sara Data 0 Karein)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoTrackSmsCard(
    isPro: Boolean,
    hasSms: Boolean,
    scannedCount: Int,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) RoyalBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPro) RoyalBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPro) RoyalBlue.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPro) Icons.Default.Sms else Icons.Default.Lock,
                        contentDescription = "SMS Sync",
                        tint = if (isPro) RoyalBlue else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPro) "All Transactions SMS Auto-Sync" else "Auto SMS Sync (PRO)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPro) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (hasSms && isPro) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = MoneyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isPro) {
                            "Bank, UPI, Amazon, Wallets & Cards ke sabhi messages auto-track honge!"
                        } else {
                            "Unlocks automatic tracking for Bank, Amazon, Wallets & Cards."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPro) RoyalBlue else MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("grant_sms_permissions_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPro) Icons.Default.Sms else Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPro) {
                            if (hasSms) "Scan & Sync All SMS ($scannedCount synced)" else "Allow SMS Access to Auto-Track"
                        } else {
                            "Upgrade to PRO to Activate"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    userName: String,
    dateDisplay: String,
    proStatus: com.example.model.ProStatus,
    onProClicked: () -> Unit,
    onProfileClicked: () -> Unit
) {
    val isPro = proStatus.isPro || proStatus.status == "ACTIVE"
    val isPending = proStatus.status == "PENDING"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hi $userName! 👋",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isPro -> MoneyGreen.copy(alpha = 0.15f)
                    isPending -> BrandOrange.copy(alpha = 0.15f)
                    else -> RoyalBlue.copy(alpha = 0.12f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    when {
                        isPro -> MoneyGreen
                        isPending -> BrandOrange
                        else -> RoyalBlue
                    }
                ),
                modifier = Modifier
                    .clickable(onClick = onProClicked)
                    .testTag("open_pro_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.AccessTime else Icons.Default.AutoAwesome,
                        contentDescription = "Pro",
                        tint = when {
                            isPro -> MoneyGreen
                            isPending -> BrandOrange
                            else -> RoyalBlue
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isPro -> "PRO Active ⭐"
                            isPending -> "Pending..."
                            else -> "PRO @ ₹25/mo"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when {
                                isPro -> MoneyGreen
                                isPending -> BrandOrange
                                else -> RoyalBlue
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoyalBlue.copy(alpha = 0.15f))
                    .border(2.dp, RoyalBlue, CircleShape)
                    .clickable(onClick = onProfileClicked),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).ifEmpty { "U" }.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue
                    )
                )
            }
        }
    }
}

@Composable
private fun TimeframePills(
    selectedTimeframe: HomeTimeframe,
    onSelectTimeframe: (HomeTimeframe) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val isMonth = selectedTimeframe == HomeTimeframe.THIS_MONTH
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isMonth) RoyalBlue else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isMonth) RoyalBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onSelectTimeframe(HomeTimeframe.THIS_MONTH) }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isMonth) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isMonth) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (!isMonth) RoyalBlue else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (!isMonth) RoyalBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onSelectTimeframe(HomeTimeframe.THIS_YEAR) }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This Year",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (!isMonth) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (!isMonth) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddBill: () -> Unit,
    onSetBudget: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(
            label = "Add Expense",
            icon = Icons.Default.Add,
            bgColor = MoneyGreen,
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Add Income",
            icon = Icons.Default.Description,
            bgColor = BalanceBlue,
            onClick = onAddIncome,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Scan Bill",
            icon = Icons.Default.Receipt,
            bgColor = PurpleAccent,
            onClick = onAddBill,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Set Budget",
            icon = Icons.Default.TrackChanges,
            bgColor = RoyalBlue,
            onClick = onSetBudget,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val catInfo = ExpenseCategories.getCategoryInfo(transaction.category)
    val isExpense = transaction.isExpense()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catInfo.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catInfo.icon,
                        contentDescription = transaction.category,
                        tint = catInfo.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${transaction.date} • ${transaction.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.notes.isNotEmpty()) {
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            val prefix = if (isExpense) "" else "+"
            val color = if (isExpense) MaterialTheme.colorScheme.onSurface else MoneyGreen
            Text(
                text = "$prefix${DashboardViewModel.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}
