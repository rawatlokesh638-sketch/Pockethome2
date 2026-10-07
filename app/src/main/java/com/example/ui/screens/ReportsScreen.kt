package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpenseCategories
import com.example.ui.DashboardUiState
import com.example.ui.DashboardViewModel
import com.example.ui.ReportsTimeframe
import com.example.ui.theme.*

@Composable
fun ReportsScreen(
    uiState: DashboardUiState,
    isPro: Boolean = false,
    onBack: () -> Unit,
    onTimeframeSelected: (ReportsTimeframe) -> Unit,
    onUpgradeClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("reports_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Spending Insights",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isPro) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable { onUpgradeClicked() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = BrandOrange)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "Unlock Advanced Insights", fontWeight = FontWeight.Bold, color = BrandOrange)
                                    Text(text = "Upgrade to PRO for detailed reports and PDF export.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 1. Timeframe Select
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportPill(
                            label = "Monthly",
                            selected = uiState.reportsTimeframe == ReportsTimeframe.MONTHLY,
                            onClick = { onTimeframeSelected(ReportsTimeframe.MONTHLY) },
                            modifier = Modifier.weight(1f)
                        )
                        ReportPill(
                            label = "Yearly",
                            selected = uiState.reportsTimeframe == ReportsTimeframe.YEARLY,
                            onClick = { onTimeframeSelected(ReportsTimeframe.YEARLY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Spending Trend Area Chart (Recharts Style)
                item {
                    Text(
                        text = "Monthly Spending Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(200.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        SpendingAreaChart(
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }

                // 3. Category Distribution (Donut)
                item {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            breakdown = uiState.categoryBreakdown,
                            totalExpenses = uiState.currentMonthExpenses
                        )
                    }
                }

                // 4. Breakdown Legend
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uiState.categoryBreakdown.isEmpty()) {
                                Text("No data for current period", modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                uiState.categoryBreakdown.forEach { item ->
                                    val catInfo = ExpenseCategories.getCategoryInfo(item.categoryName)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(catInfo.color))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(item.categoryName, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(DashboardViewModel.formatCurrency(item.totalAmount), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingAreaChart(modifier: Modifier = Modifier) {
    // Mock data for trend
    val points = listOf(0.2f, 0.5f, 0.4f, 0.8f, 0.6f, 0.9f, 0.7f)
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = width / (points.size - 1)

        val path = Path()
        val fillPath = Path()
        
        points.forEachIndexed { i, p ->
            val x = i * step
            val y = height - (p * height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw Area Fill (Recharts style)
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(RoyalBlue.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw Line
        drawPath(
            path = path,
            color = RoyalBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw points
        points.forEachIndexed { i, p ->
            val x = i * step
            val y = height - (p * height)
            drawCircle(color = RoyalBlue, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun ReportPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) RoyalBlue else Color.Transparent)
            .border(1.dp, if (selected) RoyalBlue else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color.Gray, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun DonutChart(breakdown: List<com.example.ui.CategorySpend>, totalExpenses: Double) {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 30.dp.toPx()
            if (breakdown.isEmpty()) {
                drawCircle(Color.LightGray.copy(alpha = 0.2f), style = Stroke(strokeWidth))
            } else {
                var start = -90f
                breakdown.forEach { item ->
                    val sweep = (item.percentage / 100f) * 360f
                    drawArc(
                        color = ExpenseCategories.getCategoryInfo(item.categoryName).color,
                        startAngle = start,
                        sweepAngle = sweep - 1f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(DashboardViewModel.formatCurrency(totalExpenses), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Total", style = MaterialTheme.typography.bodySmall)
        }
    }
}
