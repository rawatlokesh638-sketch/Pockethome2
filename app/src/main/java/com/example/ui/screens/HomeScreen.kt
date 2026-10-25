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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpenseCategories
import com.example.model.Transaction
import com.example.service.AiWealthGrowthAdvisor
import com.example.ui.CategorySpend
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RosePrimary
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
    currentChartDataType: com.example.ui.ChartDataType = com.example.ui.ChartDataType.EXPENSE,
    onChartDataTypeChanged: (com.example.ui.ChartDataType) -> Unit = {},
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
    onToggleTransactionType: (String) -> Unit = {},
    onToggleBillPaid: (String, Boolean) -> Unit,
    onDeleteBill: (String) -> Unit,
    onOpenAiGrowthAdvisor: () -> Unit = {},
    onOpenSmartAdd: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
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
                    onRequestSmsPermissions()
                },
                onOpenProSheet = onOpenProSheet
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

        if (uiState.homeTimeframe == HomeTimeframe.THIS_MONTH && uiState.currentMonthExpenses == 0.0 && uiState.totalAllTimeExpenses > 0) {
            item(key = "all_time_hint_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeframeChanged(HomeTimeframe.ALL_TIME) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Is mahine ka kharcha ₹0 hai. Purana kharcha (${DashboardViewModel.formatCurrency(uiState.totalAllTimeExpenses)}) dekhne ke liye tap karein 👆",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RoyalBlue
                        )
                    }
                }
            }
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

        // 5.5 Smart Voice & Photo Receipt Order Add Card
        item(key = "smart_add_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable(onClick = onOpenSmartAdd),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PurpleAccent.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PurpleAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bolke ya Photo se Hisaab Jodein 📸",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Voice command, bill receipt photo ya order copy-paste karein",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSmartAdd,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Smart Add",
                            tint = Color.White
                        )
                    }
                }
            }
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
                onTimeframeSelected = onChartTimeframeChanged,
                currentDataType = currentChartDataType,
                onDataTypeSelected = onChartDataTypeChanged,
                totalTransactionsCount = uiState.totalTransactionCount,
                incomeTransactionCount = uiState.incomeTransactionCount,
                expenseTransactionCount = uiState.expenseTransactionCount,
                onAddExpenseClicked = onAddExpenseClicked
            )
        }

        // 8. Category-wise Spending Breakdown
        item(key = "category_breakdown") {
            CategoryBreakdownSection(
                breakdown = uiState.categoryBreakdown,
                incomeBreakdown = uiState.incomeCategoryBreakdown,
                allTimeBreakdown = uiState.allTimeCategoryBreakdown,
                incomeCount = uiState.incomeTransactionCount,
                expenseCount = uiState.expenseTransactionCount,
                onViewTransactionsClicked = onViewAllTransactions
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
                    onClick = { onTransactionClicked(tx) },
                    onToggleType = { onToggleTransactionType(tx.id) }
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

        // 13. Privacy & Terms Footer Card with Zero Sharing Promise & Support Email
        item(key = "privacy_terms_footer") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable(onClick = onOpenPrivacyPolicy),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100% Private & Secure • Zero Data Sharing",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ham aapka data kisi third party ke sath share nahi karte. Safe on-device analysis.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Help Support: rawatlokesh638@gmail.com • Terms & Privacy 📜",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue,
                        textAlign = TextAlign.Center
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
    onRequestPermissions: () -> Unit,
    onOpenProSheet: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) RoyalBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPro) RoyalBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                        .background(RoyalBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS Sync",
                        tint = RoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All Transactions SMS Auto-Sync",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPro) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF1D4ED8).copy(alpha = 0.15f),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = if (isPro) "PRO ACTIVE" else "PRO ⭐ (₹15/mo)",
                            color = if (isPro) Color(0xFF10B981) else Color(0xFF1D4ED8),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = if (isPro) "Bank, UPI, Amazon, Wallets & Cards ke sabhi messages auto-track honge!" else "Bank & UPI SMS automatic continuous tracker (Included in PRO)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasSms) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Real-Time Auto Sync Active (No Button Needed)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            )
                            Text(
                                text = "Naya bank/UPI SMS aane par apne aap real-time me entry add hoti rahegi. ($scannedCount synced)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (isPro) {
                Button(
                    onClick = onRequestPermissions,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("grant_sms_permissions_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enable Real-Time SMS Auto-Track",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestPermissions,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text("Enable SMS Auto-Sync", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onOpenProSheet,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text("Unlock PRO (₹15)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
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
                            else -> "PRO @ ₹15/mo"
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
        val options = listOf(
            HomeTimeframe.THIS_MONTH to "This Month",
            HomeTimeframe.THIS_YEAR to "This Year",
            HomeTimeframe.ALL_TIME to "All Time"
        )

        options.forEach { (timeframe, label) ->
            val isSelected = selectedTimeframe == timeframe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) RoyalBlue else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) RoyalBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectTimeframe(timeframe) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            bgColor = RosePrimary,
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Add Income",
            icon = Icons.Default.Description,
            bgColor = EmeraldPrimary,
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
    onClick: () -> Unit,
    onToggleType: () -> Unit = {}
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

            val prefix = if (isExpense) "- " else "+ "
            val color = if (isExpense) RosePrimary else EmeraldPrimary

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isExpense) RosePrimary.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$prefix${DashboardViewModel.formatCurrency(transaction.amount)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    )
                }

                IconButton(
                    onClick = onToggleType,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Toggle Expense/Income",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMoneyGrowthBannerCard(
    isPro: Boolean,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    categorySpends: List<CategorySpend>,
    onOpenAdvisor: () -> Unit,
    onOpenProSheet: () -> Unit
) {
    val report = remember(monthlyIncome, monthlyExpenses, categorySpends) {
        AiWealthGrowthAdvisor.generateDailyReport(monthlyIncome, monthlyExpenses, categorySpends)
    }
    var selectedTab by remember { mutableStateOf("ALLOCATION") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("ai_money_growth_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF132F23).copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isPro) MoneyGreen.copy(alpha = 0.45f) else RoyalBlue.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MoneyGreen, RoyalBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "AI Money Growth Advisor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPro) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF1D4ED8).copy(alpha = 0.15f),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = if (isPro) "PRO ACTIVE ⭐" else "PRO ⭐ (₹15/mo)",
                                color = if (isPro) Color(0xFF10B981) else Color(0xFF1D4ED8),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "Daily Wealth Strategy • Kaha Lagao, Kaha Bachao",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Daily Strategy Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MoneyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily AI Advice (${report.todayDate})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MoneyGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = report.dailyHeadline,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.dailyActionTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Interactive Tabs: [ 🚀 Kaha Lagao ] [ 🛑 Kaha Bachao ] [ 📈 Kitna Grow Hoga ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GrowthTabPill(
                    title = "🚀 Kaha Lagao",
                    isSelected = selectedTab == "ALLOCATION",
                    onClick = { selectedTab = "ALLOCATION" },
                    modifier = Modifier.weight(1f),
                    activeColor = RoyalBlue
                )
                GrowthTabPill(
                    title = "🛑 Kaha Bachao",
                    isSelected = selectedTab == "LEAKS",
                    onClick = { selectedTab = "LEAKS" },
                    modifier = Modifier.weight(1f),
                    activeColor = BrandOrange
                )
                GrowthTabPill(
                    title = "📈 Kitna Grow Hoga",
                    isSelected = selectedTab == "GROWTH",
                    onClick = { selectedTab = "GROWTH" },
                    modifier = Modifier.weight(1f),
                    activeColor = MoneyGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                "ALLOCATION" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.recommendedAllocations.forEach { alloc ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${alloc.percentage}% ${alloc.assetName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "CAGR: ${alloc.expectedReturnRate} • ${alloc.recommendedWhere}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = RoyalBlue.copy(alpha = 0.1f)) {
                                        Text(
                                            text = "₹${alloc.allocatedAmount.toInt()}/mo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RoyalBlue),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "LEAKS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.moneyLeaks.forEach { leak ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚠️ ${leak.category}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = BrandOrange)
                                        )
                                        Text(
                                            text = "Bachao: ₹${leak.potentialMonthlySavings.toInt()}/mo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandOrange)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = leak.actionStep,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                "GROWTH" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.growthTimeline.forEach { milestone ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🗓️ ${milestone.timelineLabel}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Invest: ₹${milestone.totalInvested.toInt()} → Profit: +₹${milestone.totalProfit.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = DashboardViewModel.formatCurrency(milestone.projectedValue),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = MoneyGreen)
                                        )
                                        Text(
                                            text = "Projected",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MoneyGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { if (isPro) onOpenAdvisor() else onOpenProSheet() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPro) MoneyGreen else RoyalBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPro) "Open Full Wealth Simulator (SIP Calculator) ➔" else "Unlock AI Wealth Advisor (PRO ₹15/mo) ➔",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun GrowthTabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else activeColor.copy(alpha = 0.12f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else activeColor
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

