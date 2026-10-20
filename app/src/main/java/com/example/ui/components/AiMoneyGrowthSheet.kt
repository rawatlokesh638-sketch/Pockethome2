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
import java.util.Locale
import com.example.service.*
import com.example.ui.CategorySpend
import com.example.ui.DashboardViewModel
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMoneyGrowthSheet(
    isPro: Boolean = false,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    categorySpends: List<CategorySpend>,
    onDismiss: () -> Unit,
    onUpgradeClicked: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val report = remember(monthlyIncome, monthlyExpenses, categorySpends) {
        AiWealthGrowthAdvisor.generateDailyReport(monthlyIncome, monthlyExpenses, categorySpends)
    }

    var selectedRiskProfile by remember { mutableStateOf("MODERATE") } // CONSERVATIVE, MODERATE, AGGRESSIVE
    var customMonthlyAmount by remember { mutableStateOf(report.monthlySavingsCapacity) }
    var adjustForInflation by remember { mutableStateOf(false) }

    val currentAnnualRate = when (selectedRiskProfile) {
        "CONSERVATIVE" -> 0.085
        "AGGRESSIVE" -> 0.135
        else -> 0.115
    }

    val dynamicTimeline = remember(customMonthlyAmount, currentAnnualRate) {
        AiWealthGrowthAdvisor.calculateTimeline(customMonthlyAmount, annualReturnRate = currentAnnualRate)
    }

    val currentAllocations = when (selectedRiskProfile) {
        "CONSERVATIVE" -> report.conservativeAllocations
        "AGGRESSIVE" -> report.aggressiveAllocations
        else -> report.moderateAllocations
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF047857), RoyalBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Money Growth",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Institutional AI Advisor",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MoneyGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO WEALTH",
                                    color = MoneyGreen,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "Executive Financial Roadmap & Wealth Creation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isPro) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpgradeClicked() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1D4ED8).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO ⭐ (₹15/mo)",
                                    color = Color(0xFF1D4ED8),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview Mode: PRO buy karne ke baad custom controls unlock honge",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Upgrade ➔",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RoyalBlue),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. FINANCIAL HEALTH SCORE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(report.healthAudit.statusColorHex).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FINANCIAL HEALTH SCORE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${report.healthAudit.score}",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(report.healthAudit.statusColorHex)
                                )
                                Text(
                                    text = " / 100",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(report.healthAudit.statusColorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = report.healthAudit.grade,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(report.healthAudit.statusColorHex),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { report.healthAudit.score / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(report.healthAudit.statusColorHex),
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Savings Efficiency: ${report.savingsRatePercent}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Emergency Runway: ~${String.format(Locale.getDefault(), "%.1f", report.healthAudit.emergencyRunwayMonths)} mo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.executiveBrief,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 50 / 30 / 20 FRAMEWORK AUDIT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚖️ 50 / 30 / 20 Budget Rule Audit",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (report.ruleFiftyThirtyTwenty.isBalanced) MoneyGreen.copy(alpha = 0.15f) else BrandOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (report.ruleFiftyThirtyTwenty.isBalanced) "BALANCED ✅" else "OPTIMIZE ⚡",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = if (report.ruleFiftyThirtyTwenty.isBalanced) MoneyGreen else BrandOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetPillar(label = "Needs (50%)", current = "${report.ruleFiftyThirtyTwenty.needsPercent}%", color = RoyalBlue, modifier = Modifier.weight(1f))
                        BudgetPillar(label = "Wants (30%)", current = "${report.ruleFiftyThirtyTwenty.wantsPercent}%", color = BrandOrange, modifier = Modifier.weight(1f))
                        BudgetPillar(label = "Investments (20%)", current = "${report.ruleFiftyThirtyTwenty.investmentsPercent}%", color = MoneyGreen, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.ruleFiftyThirtyTwenty.suggestion,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. RISK PROFILE SELECTOR
            Text(
                text = "⚡ Select Investment Risk Profile",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskProfilePill(
                    title = "Conservative",
                    cagr = "8.5% p.a.",
                    selected = selectedRiskProfile == "CONSERVATIVE",
                    onClick = {
                        if (!isPro) onUpgradeClicked() else selectedRiskProfile = "CONSERVATIVE"
                    },
                    modifier = Modifier.weight(1f),
                    activeColor = RoyalBlue
                )
                RiskProfilePill(
                    title = "Moderate (Best)",
                    cagr = "11.5% p.a.",
                    selected = selectedRiskProfile == "MODERATE",
                    onClick = {
                        if (!isPro) onUpgradeClicked() else selectedRiskProfile = "MODERATE"
                    },
                    modifier = Modifier.weight(1f),
                    activeColor = MoneyGreen
                )
                RiskProfilePill(
                    title = "Aggressive",
                    cagr = "13.5% p.a.",
                    selected = selectedRiskProfile == "AGGRESSIVE",
                    onClick = {
                        if (!isPro) onUpgradeClicked() else selectedRiskProfile = "AGGRESSIVE"
                    },
                    modifier = Modifier.weight(1f),
                    activeColor = PurpleAccent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. INTERACTIVE WEALTH SIMULATOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🎯 Monthly SIP & Wealth Simulator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalBlue
                            )
                            if (!isPro) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1D4ED8).copy(alpha = 0.15f),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Text(
                                        text = "PRO ⭐ (₹15/mo)",
                                        color = Color(0xFF1D4ED8),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = "Inflation (6%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = if (isPro) adjustForInflation else false,
                                onCheckedChange = {
                                    if (!isPro) onUpgradeClicked() else adjustForInflation = it
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Text(
                        text = if (adjustForInflation) "Real Purchasing Power dikha raha hai (6% Inflation factored in):" else "Nominal Future Compounded Corpus at ${(currentAnnualRate * 100).roundToInt()}% CAGR:",
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
                            text = "Monthly Contribution:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = DashboardViewModel.formatCurrency(customMonthlyAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = customMonthlyAmount.toFloat(),
                        onValueChange = {
                            if (!isPro) {
                                onUpgradeClicked()
                            } else {
                                customMonthlyAmount = (it / 500).roundToInt() * 500.0
                            }
                        },
                        valueRange = 500f..40000f,
                        steps = 78,
                        colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1500.0, 3000.0, 5000.0, 10000.0).forEach { amt ->
                            SuggestionChip(
                                onClick = {
                                    if (!isPro) onUpgradeClicked() else customMonthlyAmount = amt
                                },
                                label = { Text("₹${amt.toInt()}", fontSize = 11.sp, fontWeight = if (customMonthlyAmount == amt) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. TIMELINE MILESTONES
            SectionHeader(
                title = "📈 Kitna Grow Hoga & Kabb Takk?",
                subtitle = if (adjustForInflation) "Real purchasing power after 6% inflation" else "${(currentAnnualRate * 100).roundToInt()}% Expected Blended CAGR Timeline"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dynamicTimeline.forEach { milestone ->
                    MilestoneCard(milestone = milestone, adjustForInflation = adjustForInflation)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. ASSET ALLOCATION
            SectionHeader(
                title = "🚀 Kaha Lagao? (Asset Allocation - $selectedRiskProfile)",
                subtitle = "Scientific diversification for optimal risk-adjusted returns"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentAllocations.forEach { alloc ->
                    val allocatedForCustom = (customMonthlyAmount * (alloc.percentage / 100.0)).roundToInt().toDouble()
                    AllocationCard(alloc = alloc, customAmount = allocatedForCustom)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. TAX OPTIMIZATION STRATEGIES
            SectionHeader(
                title = "🛡️ Tax Saving & Optimization Strategies",
                subtitle = "Government approved wealth shields to legally save taxes"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                report.taxStrategies.forEach { tax ->
                    TaxCard(tax = tax)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. MONEY LEAKS (KAHA BACHAO)
            SectionHeader(
                title = "🛑 Kaha Se Bachao? (Identified Leaks)",
                subtitle = "Prune non-essential spends and redirect into wealth creation"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                report.moneyLeaks.forEach { leak ->
                    MoneyLeakCard(leak = leak)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pro Membership Activation Card / Status
            if (!isPro) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RoyalBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "POCKETHOME PRO (₹15/MONTH)",
                                color = RoyalBlue,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock Interactive Wealth Planner & Custom Portfolio 🚀",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PRO plan activate karne ke baad sabhi custom SIP sliders, inflation adjusters aur dynamic leak alerts unlock honge.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onUpgradeClicked,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Upgrade to PRO (₹15/mo) Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PRO Active Member 🎉",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            )
                            Text(
                                text = "Aapke paas full institutional wealth advisory aur interactive simulator ka access hai.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun BudgetPillar(label: String, current: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = current, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun RiskProfilePill(
    title: String,
    cagr: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = RoyalBlue
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = cagr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White.copy(alpha = 0.9f) else activeColor
                )
            )
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
private fun MilestoneCard(milestone: GrowthMilestone, adjustForInflation: Boolean) {
    val displayValue = if (adjustForInflation) milestone.realPurchasingPower else milestone.projectedValue

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
                        text = if (adjustForInflation) "Real Wealth (Inflation Adj.)" else "Total Projected Wealth 🚀",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MoneyGreen
                    )
                    Text(
                        text = DashboardViewModel.formatCurrency(displayValue),
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
                    text = "Gain: +${DashboardViewModel.formatCurrency(displayValue - milestone.totalInvested)}",
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
                            .size(34.dp)
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

            Text(
                text = "CAGR: ${alloc.expectedReturnRate} • ${alloc.riskLevel}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MoneyGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Recommended Instruments: ${alloc.recommendedWhere}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun TaxCard(tax: TaxStrategyItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tax.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RoyalBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Sec ${tax.section}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RoyalBlue),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tax.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Limit: ${tax.maxLimit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = tax.impact, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MoneyGreen)
            }
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
                        text = "Save: ₹${leak.potentialMonthlySavings.toInt()}/mo",
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
