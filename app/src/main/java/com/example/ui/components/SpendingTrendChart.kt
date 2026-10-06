package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DashboardViewModel
import com.example.ui.theme.EmeraldPrimary

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
    modifier: Modifier = Modifier
) {
    val maxAmount = chartData.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0

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
            // Header Row with Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kharcha Trend Analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timeframe Pill Selector
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

            Spacer(modifier = Modifier.height(20.dp))

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

                    val barColor = if (bar.isHighlighted) {
                        EmeraldPrimary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
                                color = if (bar.isHighlighted) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                .width(if (chartData.size > 5) 20.dp else 28.dp),
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
