package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpenseCategories
import com.example.ui.CategorySpend
import com.example.ui.DashboardUiState
import com.example.ui.DashboardViewModel
import com.example.ui.ReportsTimeframe
import com.example.ui.theme.*

enum class ReportCategoryMode {
    EXPENSES,
    INCOME,
    BILLS
}

@Composable
fun ReportsScreen(
    uiState: DashboardUiState,
    isPro: Boolean = false,
    onBack: () -> Unit,
    onTimeframeSelected: (ReportsTimeframe) -> Unit,
    onUpgradeClicked: () -> Unit = {},
    onOpenAiAdvisor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeMode by remember { mutableStateOf(ReportCategoryMode.EXPENSES) }

    // Compute period totals accurately from filtered transactions or fall back to uiState totals
    val filteredTxns = uiState.filteredTransactions
    val periodExpenses = if (filteredTxns.isNotEmpty()) {
        filteredTxns.filter { it.isExpense() }.sumOf { it.amount }
    } else {
        uiState.currentMonthExpenses.ifZero { uiState.totalAllTimeExpenses }
    }

    val periodIncome = if (filteredTxns.isNotEmpty()) {
        filteredTxns.filter { it.isIncome() }.sumOf { it.amount }
    } else {
        uiState.currentMonthIncome.ifZero { uiState.totalAllTimeIncome }
    }

    val periodNetBalance = periodIncome - periodExpenses
    val totalBillsAmount = uiState.upcomingBills.sumOf { it.amount }
    val totalBillsCount = uiState.upcomingBills.size

    val activeBreakdown: List<CategorySpend> = when (activeMode) {
        ReportCategoryMode.EXPENSES -> {
            if (uiState.categoryBreakdown.isNotEmpty()) uiState.categoryBreakdown
            else uiState.allTimeCategoryBreakdown
        }
        ReportCategoryMode.INCOME -> uiState.incomeCategoryBreakdown
        ReportCategoryMode.BILLS -> {
            uiState.upcomingBills.groupBy { it.category }.map { (cat, bills) ->
                CategorySpend(
                    categoryName = cat,
                    totalAmount = bills.sumOf { it.amount },
                    percentage = if (totalBillsAmount > 0) ((bills.sumOf { it.amount } / totalBillsAmount) * 100).toFloat() else 0f,
                    count = bills.size
                )
            }.sortedByDescending { it.totalAmount }
        }
    }

    val activeTotal = when (activeMode) {
        ReportCategoryMode.EXPENSES -> periodExpenses
        ReportCategoryMode.INCOME -> periodIncome
        ReportCategoryMode.BILLS -> totalBillsAmount
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("reports_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar & Title
            item(key = "reports_top_bar") {
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
                    Column {
                        Text(
                            text = "Spending & Income Reports 📊",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aapka poora kharcha, aamadani aur bills ki detailed report",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. AI Money Growth Advisor Banner
            item(key = "ai_advisor_banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { onOpenAiAdvisor() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MoneyGreen.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MoneyGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MoneyGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = MoneyGreen, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "AI Money Growth & Wealth Advisor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = MoneyGreen.copy(alpha = 0.15f)) {
                                    Text(text = "LIVE", color = MoneyGreen, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(text = "Kaha lagayein, kaha se bachaayein aur kitna grow hoga!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MoneyGreen)
                    }
                }
            }

            // 3. Timeframe Pills: [ Monthly ] [ Yearly ]
            item(key = "timeframe_select") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportPill(
                        label = "Monthly Report 📅",
                        selected = uiState.reportsTimeframe == ReportsTimeframe.MONTHLY,
                        onClick = { onTimeframeSelected(ReportsTimeframe.MONTHLY) },
                        modifier = Modifier.weight(1f)
                    )
                    ReportPill(
                        label = "Yearly Report 📆",
                        selected = uiState.reportsTimeframe == ReportsTimeframe.YEARLY,
                        onClick = { onTimeframeSelected(ReportsTimeframe.YEARLY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Report Financial Overview Cards (Expenses, Income, Bills, Net Balance)
            item(key = "report_summary_cards") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Report Period Summary",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Expenses Card
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(RosePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = RosePrimary, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kul Kharcha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DashboardViewModel.formatCurrency(periodExpenses),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 16.sp)
                                )
                            }

                            // Income Card
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kul Kamai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DashboardViewModel.formatCurrency(periodIncome),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 16.sp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Bills Summary
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(BrandOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upcoming Bills ($totalBillsCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DashboardViewModel.formatCurrency(totalBillsAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandOrange, fontSize = 15.sp)
                                )
                            }

                            // Net Savings
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Net Savings / Surplus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DashboardViewModel.formatCurrency(periodNetBalance),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (periodNetBalance >= 0) EmeraldPrimary else RosePrimary,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Export to PDF / Excel Card
            item(key = "export_report_card") {
                val context = LocalContext.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPro) RoyalBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export Report (PDF & Excel)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPro) "Download clean financial summary for ${uiState.activeMonthDisplay}" else "Download clean monthly PDF / Excel spreadsheet (Available in PRO)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!isPro) {
                                        onUpgradeClicked()
                                    } else {
                                        val file = com.example.util.ReportExporter.generatePdf(context, filteredTxns)
                                        val uri = com.example.util.ReportExporter.getFileUri(context, file)
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Report via"))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Button(
                                onClick = {
                                    if (!isPro) {
                                        onUpgradeClicked()
                                    } else {
                                        val file = com.example.util.ReportExporter.generateCsv(context, filteredTxns)
                                        val uri = com.example.util.ReportExporter.getFileUri(context, file)
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Excel / CSV via"))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Excel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 6. Spending Trend Area Chart
            item(key = "spending_trend_chart") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Monthly Spending Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        SpendingAreaChart(
                            transactions = filteredTxns,
                            totalExpenses = periodExpenses,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }
            }

            // 7. Category Breakdown Mode Switcher: [ Kharcha 💸 ] [ Kamai 💰 ] [ Bills 🧾 ]
            item(key = "category_mode_switcher") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Category Breakdown & Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(ReportCategoryMode.EXPENSES, "Kharcha 💸", RosePrimary),
                            Triple(ReportCategoryMode.INCOME, "Kamai 💰", EmeraldPrimary),
                            Triple(ReportCategoryMode.BILLS, "Bills 🧾", BrandOrange)
                        ).forEach { (mode, label, color) ->
                            val isSelected = activeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { activeMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 8. Category Distribution Donut Chart
            item(key = "donut_chart_section") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        breakdown = activeBreakdown,
                        totalExpenses = activeTotal
                    )
                }
            }

            // 9. Category Breakdown Details Legend
            item(key = "category_legend_card") {
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
                        if (activeBreakdown.isEmpty()) {
                            Text("No category data recorded for this mode", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall)
                        } else {
                            activeBreakdown.forEach { item ->
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
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(${item.count})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun Double.ifZero(fallback: () -> Double): Double {
    return if (this <= 0.0) fallback() else this
}

@Composable
fun SpendingAreaChart(
    transactions: List<com.example.model.Transaction>,
    totalExpenses: Double,
    modifier: Modifier = Modifier
) {
    val expenseTxns = transactions.filter { it.isExpense() }
    val calcTotal = if (totalExpenses > 0) totalExpenses else expenseTxns.sumOf { it.amount }

    if (calcTotal <= 0.0 || expenseTxns.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No spending data for this period",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Expenses record hote hi spending trend chart generate hoga.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    // Compute actual 7 data points from transactions grouped by timestamp
    val sorted = expenseTxns.sortedBy { it.timestamp }
    val chunkCount = 7
    val minTime = sorted.first().timestamp
    val maxTime = sorted.last().timestamp
    val timeSpan = (maxTime - minTime).coerceAtLeast(1L)
    val bucketAmounts = DoubleArray(chunkCount) { 0.0 }

    for (tx in sorted) {
        val fraction = ((tx.timestamp - minTime).toDouble() / timeSpan).coerceIn(0.0, 0.999)
        val bucket = (fraction * chunkCount).toInt().coerceIn(0, chunkCount - 1)
        bucketAmounts[bucket] += tx.amount
    }

    val maxVal = bucketAmounts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val points = bucketAmounts.map { (it / maxVal).toFloat().coerceIn(0.05f, 1.0f) }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = width / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val x = i * step
            val y = height - (p * (height - 30.dp.toPx())) - 10.dp.toPx()
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

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(RoyalBlue.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = RoyalBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEachIndexed { i, p ->
            val x = i * step
            val y = height - (p * (height - 30.dp.toPx())) - 10.dp.toPx()
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
    val computedTotal = if (totalExpenses > 0) totalExpenses else breakdown.sumOf { it.totalAmount }

    if (computedTotal <= 0.0 || breakdown.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(DashboardViewModel.formatCurrency(0.0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("No data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No category breakdown for this selection",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 30.dp.toPx()
            var start = -90f
            breakdown.forEach { item ->
                val pct = if (computedTotal > 0) (item.totalAmount / computedTotal).toFloat() else 0f
                val sweep = pct * 360f
                if (sweep > 0f) {
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
            Text(DashboardViewModel.formatCurrency(computedTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
