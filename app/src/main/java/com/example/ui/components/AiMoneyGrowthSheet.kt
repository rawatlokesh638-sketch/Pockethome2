package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AiWealthGrowthAdvisor
import com.example.service.AssetAllocationItem
import com.example.service.GrowthMilestone
import com.example.service.MoneyLeakTip
import com.example.service.WealthAdviceReport
import com.example.ui.CategorySpend
import com.example.ui.DashboardViewModel
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMoneyGrowthSheet(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    categorySpends: List<CategorySpend>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val baseReport = remember(monthlyIncome, monthlyExpenses, categorySpends) {
        AiWealthGrowthAdvisor.generateDailyReport(monthlyIncome, monthlyExpenses, categorySpends)
    }

    var customMonthlyAmount by remember { mutableStateOf(baseReport.monthlySavingsCapacity) }
    var selectedTimelineYears by remember { mutableStateOf(5) }

    val dynamicTimeline = remember(customMonthlyAmount) {
        AiWealthGrowthAdvisor.calculateTimeline(customMonthlyAmount, annualReturnRate = 0.12)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_money_growth_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF2E7D32), RoyalBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Money Growth",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Money Growth Advisor",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MoneyGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = MoneyGreen,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Daily Wealth Strategy • Kaha Lagao, Kaha Bachao",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Daily AI Strategy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aaj Ki AI Strategy (${baseReport.todayDate})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = RoyalBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = baseReport.dailyHeadline,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = baseReport.dailyActionTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Interactive Calculator: "Aap Har Mahine Kitna Bachana / Lagana Chahte Hain?"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🎯 Monthly Investment & Wealth Simulator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue
                    )
                    Text(
                        text = "Amount chunein aur dekhein paisa kabb takk aur kitna grow karega:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly SIP / Savings:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = DashboardViewModel.formatCurrency(customMonthlyAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = customMonthlyAmount.toFloat(),
                        onValueChange = { customMonthlyAmount = (it / 500).roundToInt() * 500.0 },
                        valueRange = 500f..30000f,
                        steps = 58,
                        colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000.0, 2500.0, 5000.0, 10000.0).forEach { amt ->
                            SuggestionChip(
                                onClick = { customMonthlyAmount = amt },
                                label = { Text("₹${amt.toInt()}", fontSize = 11.sp, fontWeight = if (customMonthlyAmount == amt) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. SECTION: "📈 Kitna Grow Hoga & Kabb Takk?"
            SectionHeader(
                title = "📈 Kitna Grow Hoga & Kabb Takk?",
                subtitle = "12% Expected CAGR Compounding Timeline"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dynamicTimeline.forEach { milestone ->
                    MilestoneCard(milestone = milestone)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. SECTION: "🚀 Kaha Lagao? (Asset Allocation)"
            SectionHeader(
                title = "🚀 Kaha Lagao? (Smart Allocation)",
                subtitle = "Har ₹100 ko sahi jagah baatne ka scientific formula"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                baseReport.recommendedAllocations.forEach { alloc ->
                    val allocatedForCustom = (customMonthlyAmount * (alloc.percentage / 100.0)).roundToInt().toDouble()
                    AllocationCard(alloc = alloc, customAmount = allocatedForCustom)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. SECTION: "🛑 Kaha Se Bachao? (Identified Leaks)"
            SectionHeader(
                title = "🛑 Kaha Se Bachao? (Money Leaks)",
                subtitle = "In jagahon se roz aur mahine paise bacha kar invest karein"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                baseReport.moneyLeaks.forEach { leak ->
                    MoneyLeakCard(leak = leak)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MilestoneCard(milestone: GrowthMilestone) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "🗓️ ${milestone.timelineLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Target: ${milestone.targetDate}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Aapka Jama Paisa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DashboardViewModel.formatCurrency(milestone.totalInvested),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Projected Wealth 🚀",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MoneyGreen
                    )
                    Text(
                        text = DashboardViewModel.formatCurrency(milestone.projectedValue),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = MoneyGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MoneyGreen.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Munafa / Growth: +${DashboardViewModel.formatCurrency(milestone.totalProfit)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MoneyGreen
                )
                Text(
                    text = milestone.milestoneGoal,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllocationCard(alloc: AssetAllocationItem, customAmount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RoyalBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${alloc.percentage}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RoyalBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = alloc.assetName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = DashboardViewModel.formatCurrency(customAmount) + "/mo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alloc.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAGR: ${alloc.expectedReturnRate} • ${alloc.riskLevel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MoneyGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Kaha Lagayein: ${alloc.recommendedWhere}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MoneyLeakCard(leak: MoneyLeakTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ ${leak.category}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandOrange
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandOrange.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Bachao: ₹${leak.potentialMonthlySavings.toInt()}/mo",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = leak.issueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "👉 Action: ${leak.actionStep}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
