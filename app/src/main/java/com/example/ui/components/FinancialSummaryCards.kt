package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DashboardViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePrimary
import kotlin.math.abs

@Composable
fun FinancialSummaryCards(
    currentMonthExpenses: Double,
    totalIncome: Double,
    remainingBalance: Double,
    todaySpending: Double,
    monthOverMonthDiff: Double,
    monthOverMonthPercent: Float,
    isSpendHigherThanLastMonth: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_summary_section")
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Balance & Expense Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Expenses Card
            SummaryCard(
                title = "Monthly Expense",
                hindiSubtitle = "Iss Mahine ka Kharcha",
                amount = currentMonthExpenses,
                icon = Icons.Default.ArrowDownward,
                accentColor = RosePrimary,
                modifier = Modifier.weight(1f)
            )

            // Income Card
            SummaryCard(
                title = "Total Income",
                hindiSubtitle = "Kul Aay / Vetan",
                amount = totalIncome,
                icon = Icons.Default.ArrowUpward,
                accentColor = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Row: Remaining Balance & Today's Spending
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Remaining Balance Card
            val balanceColor = if (remainingBalance >= 0) EmeraldPrimary else RosePrimary
            SummaryCard(
                title = "Remaining Balance",
                hindiSubtitle = if (remainingBalance >= 0) "Bachat / Balance" else "Kharcha zyada hua",
                amount = remainingBalance,
                icon = Icons.Default.Savings,
                accentColor = balanceColor,
                modifier = Modifier.weight(1f)
            )

            // Today's Spending Card
            SummaryCard(
                title = "Today's Spend",
                hindiSubtitle = "Aaj ka Kharcha",
                amount = todaySpending,
                icon = Icons.Default.CalendarToday,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // This Month vs Last Month Comparison Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("month_comparison_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val compIcon = if (isSpendHigherThanLastMonth) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                    val compColor = if (isSpendHigherThanLastMonth) RosePrimary else EmeraldPrimary
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(compColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = compIcon,
                            contentDescription = "Trend Icon",
                            tint = compColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vs. Last Month",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val statusText = if (isSpendHigherThanLastMonth) {
                            "Spent ${DashboardViewModel.formatCurrency(abs(monthOverMonthDiff))} more"
                        } else {
                            "Saved ${DashboardViewModel.formatCurrency(abs(monthOverMonthDiff))} compared to last month"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Percent pill
                val pctPrefix = if (isSpendHigherThanLastMonth) "+" else "-"
                val pctBg = if (isSpendHigherThanLastMonth) RosePrimary.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                val pctColor = if (isSpendHigherThanLastMonth) RosePrimary else EmeraldPrimary
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pctBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pctPrefix${String.format("%.1f", abs(monthOverMonthPercent))}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = pctColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    hindiSubtitle: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = DashboardViewModel.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = hindiSubtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
