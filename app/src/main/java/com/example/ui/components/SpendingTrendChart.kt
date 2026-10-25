package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChartDataType
import com.example.ui.DashboardViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoyalBlue

data class ChartBarData(
    val label: String,
    val amount: Double,
    val isHighlighted: Boolean = false
)

enum class ChartTimeframe {
    DAILY, WEEKLY, MONTHLY
}

@Composable
fun SpendingTrendChart(
    chartData: List<ChartBarData>,
    currentTimeframe: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    currentDataType: ChartDataType = ChartDataType.EXPENSE,
    onDataTypeSelected: (ChartDataType) -> Unit = {},
    totalTransactionsCount: Int = 0,
    incomeTransactionCount: Int = 0,
    expenseTransactionCount: Int = 0,
    onAddExpenseClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val maxAmount = chartData.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0
    val isIncomeMode = currentDataType == ChartDataType.INCOME

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("spending_trend_chart"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title & Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isIncomeMode) "Income Overview" else "Spending Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isIncomeMode) "Kamai / Aay Trend Analysis" else "Kharcha Trend Analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timeframe Pill Selector [Daily | Weekly | Monthly]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    TimeframePill(
                        label = "Daily",
                        isSelected = currentTimeframe == ChartTimeframe.DAILY,
                        onClick = { onTimeframeSelected(ChartTimeframe.DAILY) }
                    )
                    TimeframePill(
                        label = "Weekly",
                        isSelected = currentTimeframe == ChartTimeframe.WEEKLY,
                        onClick = { onTimeframeSelected(ChartTimeframe.WEEKLY) }
                    )
                    TimeframePill(
                        label = "Monthly",
                        isSelected = currentTimeframe == ChartTimeframe.MONTHLY,
                        onClick = { onTimeframeSelected(ChartTimeframe.MONTHLY) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-bar: Type Selector [📉 Kharcha (Expense)] and [📈 Kamai (Income)]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val expenseCountLabel = if (expenseTransactionCount > 0) " ($expenseTransactionCount)" else ""
                val incomeCountLabel = if (incomeTransactionCount > 0) " ($incomeTransactionCount)" else ""

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isIncomeMode) RosePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isIncomeMode) RosePrimary else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDataTypeSelected(ChartDataType.EXPENSE) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📉 Kharcha$expenseCountLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isIncomeMode) RosePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isIncomeMode) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isIncomeMode) EmeraldPrimary else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDataTypeSelected(ChartDataType.INCOME) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📈 Kamai$incomeCountLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isIncomeMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val hasAnySpending = chartData.any { it.amount > 0 }

            if (!hasAnySpending) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isIncomeMode) "No income data for this period" else "No spending data for this period",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (!isIncomeMode && incomeTransactionCount > 0) {
                            Text(
                                text = "Aapke paas $incomeTransactionCount Income (Kamai) transactions darz hain!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onDataTypeSelected(ChartDataType.INCOME) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kamai Chart Dekhein 📈", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        } else if (isIncomeMode && expenseTransactionCount > 0) {
                            Text(
                                text = "Aapke paas $expenseTransactionCount Kharcha transactions darz hain!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = RosePrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onDataTypeSelected(ChartDataType.EXPENSE) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kharcha Chart Dekhein 📉", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        } else {
                            Text(
                                text = "Naya transaction jodne par trend analysis graph yahan dikhega.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onAddExpenseClicked,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kharcha Jodein", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                // Bars Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEach { bar ->
                        val fraction = (bar.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                        val animatedHeight by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(durationMillis = 600),
                            label = "barHeight_${bar.label}"
                        )

                        val barColor = if (isIncomeMode) {
                            if (bar.isHighlighted) EmeraldPrimary else EmeraldPrimary.copy(alpha = 0.5f)
                        } else {
                            if (bar.isHighlighted) RosePrimary else RoyalBlue.copy(alpha = 0.6f)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Amount label above bar
                            if (bar.amount > 0) {
                                Text(
                                    text = formatShortAmount(bar.amount),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (bar.isHighlighted) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (bar.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Visual Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.72f)
                                    .width(if (chartData.size > 5) 22.dp else 30.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background track
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                )

                                // Foreground animated bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(animatedHeight)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Label below bar
                            Text(
                                text = bar.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (bar.isHighlighted) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (bar.isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeframePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = textCol
        )
    }
}

private fun formatShortAmount(amount: Double): String {
    return when {
        amount >= 100000 -> "${String.format("%.1f", amount / 100000)}L"
        amount >= 1000 -> "${String.format("%.1f", amount / 1000)}k"
        else -> "₹${amount.toInt()}"
    }
}
