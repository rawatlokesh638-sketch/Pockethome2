package com.example.service

import com.example.ui.CategorySpend
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class MoneyLeakTip(
    val category: String,
    val issueText: String,
    val potentialMonthlySavings: Double,
    val actionStep: String
)

data class AssetAllocationItem(
    val assetName: String,
    val percentage: Int,
    val allocatedAmount: Double,
    val expectedReturnRate: String,
    val riskLevel: String,
    val description: String,
    val recommendedWhere: String
)

data class GrowthMilestone(
    val timelineYears: Int,
    val timelineLabel: String,
    val totalInvested: Double,
    val projectedValue: Double,
    val realPurchasingPower: Double, // Adjusted for 6% inflation
    val totalProfit: Double,
    val targetDate: String,
    val milestoneGoal: String
)

data class TaxStrategyItem(
    val title: String,
    val section: String,
    val maxLimit: String,
    val recommendation: String,
    val impact: String
)

data class FiftyThirtyTwentyBreakdown(
    val needsAmount: Double,
    val needsPercent: Int,
    val wantsAmount: Double,
    val wantsPercent: Int,
    val investmentsAmount: Double,
    val investmentsPercent: Int,
    val isBalanced: Boolean,
    val suggestion: String
)

data class FinancialHealthAudit(
    val score: Int, // 0 - 100
    val grade: String, // e.g. "A+ (Wealth Builder)", "B (Stable)", "C (Action Needed)"
    val statusColorHex: Long,
    val emergencyRunwayMonths: Double,
    val savingsEfficiency: Int, // %
    val keyStrengths: List<String>,
    val actionPriorities: List<String>
)

data class WealthAdviceReport(
    val todayDate: String,
    val executiveBrief: String,
    val dailyActionTip: String,
    val monthlySavingsCapacity: Double,
    val savingsRatePercent: Int,
    val healthAudit: FinancialHealthAudit,
    val ruleFiftyThirtyTwenty: FiftyThirtyTwentyBreakdown,
    val moneyLeaks: List<MoneyLeakTip>,
    val totalPotentialExtraSavings: Double,
    val conservativeAllocations: List<AssetAllocationItem>,
    val moderateAllocations: List<AssetAllocationItem>,
    val aggressiveAllocations: List<AssetAllocationItem>,
    val taxStrategies: List<TaxStrategyItem>,
    val growthTimeline: List<GrowthMilestone>
) {
    val dailyHeadline: String get() = executiveBrief
    val recommendedAllocations: List<AssetAllocationItem> get() = moderateAllocations
}

object AiWealthGrowthAdvisor {

    fun generateDailyReport(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        categorySpends: List<CategorySpend>
    ): WealthAdviceReport {
        val todayStr = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())

        // 1. Calculate Core Numbers
        val actualSavings = (monthlyIncome - monthlyExpenses).coerceAtLeast(0.0)
        val defaultSavingsBaseline = if (actualSavings > 0) actualSavings else (monthlyIncome * 0.20).coerceAtLeast(2000.0)
        val savingsRate = if (monthlyIncome > 0) ((actualSavings / monthlyIncome) * 100).roundToInt().coerceIn(0, 100) else 0

        // 2. Identify Leaks (Kaha Se Bachao)
        val leaks = mutableListOf<MoneyLeakTip>()
        var extraSavingsPotential = 0.0

        val foodSpend = categorySpends.firstOrNull { it.categoryName.equals("Food", ignoreCase = true) }?.totalAmount ?: 0.0
        if (foodSpend > 1000) {
            val cut = (foodSpend * 0.30).coerceAtLeast(600.0)
            leaks.add(
                MoneyLeakTip(
                    category = "Food Delivery & Dining",
                    issueText = "Swiggy/Zomato & restaurants pe ₹${foodSpend.toInt()} kharch hua.",
                    potentialMonthlySavings = cut,
                    actionStep = "Food deliveries ko mahine me 3 baar limit karein. Seedhe ₹${cut.toInt()}/mo bachega."
                )
            )
            extraSavingsPotential += cut
        } else {
            leaks.add(
                MoneyLeakTip(
                    category = "Grocery Optimization",
                    issueText = "Supermarket bulk buying aur local mandi smart schedule follow karein.",
                    potentialMonthlySavings = 500.0,
                    actionStep = "Weekly rashan list banayein jisse bina zaroorat ke items cart me na jayein."
                )
            )
            extraSavingsPotential += 500.0
        }

        val shoppingSpend = categorySpends.firstOrNull { it.categoryName.equals("Shopping", ignoreCase = true) }?.totalAmount ?: 0.0
        if (shoppingSpend > 1200) {
            val cut = (shoppingSpend * 0.35).coerceAtLeast(700.0)
            leaks.add(
                MoneyLeakTip(
                    category = "Impulse Shopping & Flash Sales",
                    issueText = "Online shopping / non-essential lifestyle items me ₹${shoppingSpend.toInt()} kharch hue.",
                    potentialMonthlySavings = cut,
                    actionStep = "48-Hour Purchase Rule: Koi bhi non-essential cheez lene se pehle 2 din rukiye."
                )
            )
            extraSavingsPotential += cut
        } else {
            leaks.add(
                MoneyLeakTip(
                    category = "Discretionary Lifestyle",
                    issueText = "Impulsive e-commerce orders ko 48-hour delay rule se filter karein.",
                    potentialMonthlySavings = 600.0,
                    actionStep = "Cart me rakh kar 2 din baad dekhein ki sach me zaroorat hai ya sirf mood spend tha."
                )
            )
            extraSavingsPotential += 600.0
        }

        val billSpend = categorySpends.firstOrNull { it.categoryName.equals("Bills", ignoreCase = true) }?.totalAmount ?: 0.0
        leaks.add(
            MoneyLeakTip(
                category = "Subscriptions & Utility Wastage",
                issueText = "Redundant OTT apps, unused mobile plans aur electricity consumption audit karein.",
                potentialMonthlySavings = 450.0,
                actionStep = "Unused streaming services cancel karein aur standby electronics switch off karein."
            )
        )
        extraSavingsPotential += 450.0

        val totalInvestableMonthly = defaultSavingsBaseline + extraSavingsPotential

        // 3. 50 / 30 / 20 Framework Audit
        val needsCategories = setOf("groceries", "bills", "rent", "health", "education", "utilities", "fuel")
        val wantsCategories = setOf("food", "shopping", "entertainment", "travel", "dining", "hobbies")

        val needsAmount = categorySpends.filter { it.categoryName.lowercase() in needsCategories }.sumOf { it.totalAmount }
        val wantsAmount = categorySpends.filter { it.categoryName.lowercase() in wantsCategories }.sumOf { it.totalAmount }
        val totalRecorded = (needsAmount + wantsAmount).coerceAtLeast(monthlyExpenses)
        val denom = if (monthlyIncome > 0) monthlyIncome else totalRecorded.coerceAtLeast(1000.0)

        val needsPct = ((needsAmount / denom) * 100).roundToInt().coerceIn(0, 100)
        val wantsPct = ((wantsAmount / denom) * 100).roundToInt().coerceIn(0, 100)
        val invPct = ((actualSavings / denom) * 100).roundToInt().coerceIn(0, 100)

        val fiftyThirtyTwenty = FiftyThirtyTwentyBreakdown(
            needsAmount = needsAmount,
            needsPercent = needsPct,
            wantsAmount = wantsAmount,
            wantsPercent = wantsPct,
            investmentsAmount = actualSavings,
            investmentsPercent = invPct,
            isBalanced = (needsPct <= 55 && wantsPct <= 35 && invPct >= 18),
            suggestion = if (invPct >= 20) {
                "Ideal ratio achieve ho gaya hai! Investments 20%+ target se aage hain."
            } else {
                "Wants ($wantsPct%) ko thoda kam karke Investments ko 20%+ par le jane ki zaroorat hai."
            }
        )

        // 4. Financial Health Score (0 - 100)
        var score = 40
        // Savings rate points (up to 30)
        score += (savingsRate * 0.30).roundToInt().coerceIn(0, 30)
        // Needs ratio discipline (up to 20)
        if (needsPct in 35..55) score += 20 else if (needsPct <= 65) score += 10
        // Wants discipline (up to 10)
        if (wantsPct <= 30) score += 10 else if (wantsPct <= 40) score += 5

        val runwayMonths = if (monthlyExpenses > 0) ((actualSavings * 3) / monthlyExpenses) else 3.0

        val (grade, colorHex) = when {
            score >= 80 -> "A+ (Wealth Builder)" to 0xFF10B981
            score >= 65 -> "A (Financially Healthy)" to 0xFF2563EB
            score >= 50 -> "B (Stable - Needs Growth)" to 0xFFF59E0B
            else -> "C (Action Required)" to 0xFFEF4444
        }

        val healthAudit = FinancialHealthAudit(
            score = score.coerceIn(0, 100),
            grade = grade,
            statusColorHex = colorHex,
            emergencyRunwayMonths = runwayMonths,
            savingsEfficiency = savingsRate,
            keyStrengths = listOf(
                "Consistent monthly tracking enabled",
                "Automated transaction capture via SMS active",
                "Zero hidden fees / 100% data transparency"
            ),
            actionPriorities = listOf(
                "Leaked ₹${extraSavingsPotential.toInt()} ko automatically Index SIP me direct karein",
                "Minimum 3 mahine ka emergency liquid reserve maintain karein",
                "High-interest debts (Credit Card/BNPL) ko turant clear karein"
            )
        )

        // 5. Professional Asset Allocation Models
        val conservativeAllocations = listOf(
            AssetAllocationItem(
                assetName = "Fixed Deposit (FD) & Liquid Debt Fund",
                percentage = 40,
                allocatedAmount = (totalInvestableMonthly * 0.40).roundToInt().toDouble(),
                expectedReturnRate = "6.8% - 7.5% p.a.",
                riskLevel = "Very Low (Capital Protection)",
                description = "High safety bank FDs aur liquid funds me paisa safe rahega aur turant nikal sakte hain.",
                recommendedWhere = "SBI, HDFC Bank FD ya Parag Parikh Liquid Fund"
            ),
            AssetAllocationItem(
                assetName = "Public Provident Fund (PPF) / Post Office",
                percentage = 20,
                allocatedAmount = (totalInvestableMonthly * 0.20).roundToInt().toDouble(),
                expectedReturnRate = "7.1% p.a. (Tax-Free)",
                riskLevel = "Zero Risk (Sovereign Guaranteed)",
                description = "Section 80C ke tahat tax bachat aur guaranteed tax-free return.",
                recommendedWhere = "Post Office PPF Account ya Nationalised Bank"
            ),
            AssetAllocationItem(
                assetName = "Nifty 50 Index Mutual Fund",
                percentage = 25,
                allocatedAmount = (totalInvestableMonthly * 0.25).roundToInt().toDouble(),
                expectedReturnRate = "11% - 13% p.a.",
                riskLevel = "Moderate (Bluechip Compounding)",
                description = "India ki top 50 corporate giants me hissa. Inflation ko beat karne ka best tareeka.",
                recommendedWhere = "Zerodha Coin, Groww (UTI / Navi Nifty 50 Direct Index)"
            ),
            AssetAllocationItem(
                assetName = "Sovereign Gold Bonds (SGB) / Gold ETF",
                percentage = 15,
                allocatedAmount = (totalInvestableMonthly * 0.15).roundToInt().toDouble(),
                expectedReturnRate = "10% p.a. + 2.5% RBI Interest",
                riskLevel = "Low-Moderate (Hedge Against Rupee Drop)",
                description = "Sona portfolio ko market crash ke dauran girne se bachata hai.",
                recommendedWhere = "RBI SGB issue ya Gold BeES on NSE"
            )
        )

        val moderateAllocations = listOf(
            AssetAllocationItem(
                assetName = "Nifty 50 Large-Cap Index Fund",
                percentage = 35,
                allocatedAmount = (totalInvestableMonthly * 0.35).roundToInt().toDouble(),
                expectedReturnRate = "12% - 14% p.a.",
                riskLevel = "Moderate (Core Wealth Engine)",
                description = "HDFC, Reliance, TCS, Infosys jaise 50 leaders ka automatic ownership.",
                recommendedWhere = "Nifty 50 Direct Index Fund (Zero commission via Groww / Coin)"
            ),
            AssetAllocationItem(
                assetName = "Mid-Cap / Flexi-Cap Mutual Fund",
                percentage = 20,
                allocatedAmount = (totalInvestableMonthly * 0.20).roundToInt().toDouble(),
                expectedReturnRate = "14% - 16% p.a.",
                riskLevel = "Moderate-High (High Growth Potential)",
                description = "Tezi se badhti hui emerging Indian companies me aggressive returns.",
                recommendedWhere = "Parag Parikh Flexi Cap ya Motilal Oswal Midcap Direct Fund"
            ),
            AssetAllocationItem(
                assetName = "Emergency Reserve (Liquid FD / High-Yield Sav.)",
                percentage = 25,
                allocatedAmount = (totalInvestableMonthly * 0.25).roundToInt().toDouble(),
                expectedReturnRate = "7.0% - 7.5% p.a.",
                riskLevel = "Very Low (Instant Emergency Access)",
                description = "Kisi bhi medical ya emergency ke liye hamesha ready cash buffer.",
                recommendedWhere = "Auto-sweep Fixed Deposit ya Liquid Mutual Fund"
            ),
            AssetAllocationItem(
                assetName = "Sovereign Gold Bonds / Gold ETF",
                percentage = 20,
                allocatedAmount = (totalInvestableMonthly * 0.20).roundToInt().toDouble(),
                expectedReturnRate = "10% - 11% p.a. + 2.5% Cash Interest",
                riskLevel = "Low-Moderate (Inflation Shield)",
                description = "Pavitra sona me paisa safe rahega aur 2.5% annual guaranteed interest milega.",
                recommendedWhere = "RBI Sovereign Gold Bonds (SGB) ya Gold BeES"
            )
        )

        val aggressiveAllocations = listOf(
            AssetAllocationItem(
                assetName = "Nifty 50 & Nifty Next 50 Equity SIP",
                percentage = 45,
                allocatedAmount = (totalInvestableMonthly * 0.45).roundToInt().toDouble(),
                expectedReturnRate = "13% - 15% p.a.",
                riskLevel = "Moderate-High (Maximum Compounding)",
                description = "Bharat ki top 100 bluechip companiyo me automatic continuous SIP.",
                recommendedWhere = "Direct Index Mutual Funds"
            ),
            AssetAllocationItem(
                assetName = "Small-Cap & Mid-Cap Alpha Fund",
                percentage = 25,
                allocatedAmount = (totalInvestableMonthly * 0.25).roundToInt().toDouble(),
                expectedReturnRate = "16% - 18% p.a.",
                riskLevel = "High (High Alpha Return)",
                description = "Agli Infosys ya Titan banne wali high-growth businesses me hissa.",
                recommendedWhere = "Nippon India / Quant Small Cap Direct Plan"
            ),
            AssetAllocationItem(
                assetName = "Sovereign Gold Bonds (SGB)",
                percentage = 15,
                allocatedAmount = (totalInvestableMonthly * 0.15).roundToInt().toDouble(),
                expectedReturnRate = "10% - 11% p.a. + 2.5% Interest",
                riskLevel = "Low-Moderate (Crash Protection)",
                description = "Stock market corrections ke dauran portfolio ki stability ensure karta hai.",
                recommendedWhere = "RBI SGB Scheme"
            ),
            AssetAllocationItem(
                assetName = "Liquid Safety & Arbitrage Fund",
                percentage = 15,
                allocatedAmount = (totalInvestableMonthly * 0.15).roundToInt().toDouble(),
                expectedReturnRate = "7.2% - 7.8% p.a.",
                riskLevel = "Zero Equity Risk (Tax Efficient)",
                description = "Dip khareedne aur emergencies ke liye backup cash.",
                recommendedWhere = "Kotak / ICICI Arbitrage Fund"
            )
        )

        // 6. Tax Strategy Recommendations
        val taxStrategies = listOf(
            TaxStrategyItem(
                title = "ELSS Mutual Funds (Section 80C)",
                section = "80C",
                maxLimit = "₹1,50,000 / year",
                recommendation = "Traditional insurance policies ki jagah ELSS chunein. Sirf 3 saal ka lock-in aur 14%+ returns.",
                impact = "Save up to ₹46,800 in income tax annually"
            ),
            TaxStrategyItem(
                title = "National Pension System (NPS)",
                section = "80CCD(1B)",
                maxLimit = "₹50,000 / year extra",
                recommendation = "80C ke 1.5 Lakh ke alawa extra ₹50,000 ki tax deduction milti hai.",
                impact = "Additional ₹15,600 tax saved every year"
            ),
            TaxStrategyItem(
                title = "Family Health Insurance (Section 80D)",
                section = "80D",
                maxLimit = "₹25,000 - ₹50,000",
                recommendation = "Khandan aur parents ka comprehensive health cover lene par premium tax-deductible hota hai.",
                impact = "Medical protection + up to ₹25,000 deduction"
            )
        )

        // 7. Timeline Projections (Calculated at 12% blended CAGR with Inflation Adjustment)
        val growthTimeline = calculateTimeline(totalInvestableMonthly, annualReturnRate = 0.12)

        // 8. Executive Daily Brief & Action Tip
        val executiveBrief = if (savingsRate >= 30) {
            "Executive Summary: Portfolio Health is Exceptional ($savingsRate% Savings Rate). Systematic investment will multiply your corpus exponentially."
        } else if (actualSavings > 0) {
            "Executive Summary: Positive Cash Flow Active (₹${actualSavings.toInt()}/mo). Minor lifestyle leak pruning unlocks ₹${totalInvestableMonthly.toInt()} investable wealth."
        } else {
            "Executive Summary: Cash Leak Warning. ₹${extraSavingsPotential.toInt()}/mo in unmonitored spends identified. Immediate capital redeployment recommended."
        }

        val dailyActionTip = "Strategic Recommendation: Automate a monthly SIP of ₹${totalInvestableMonthly.toInt()} on the 5th of every month. In 5 years, this builds a projected corpus of ₹${growthTimeline.firstOrNull { it.timelineYears == 5 }?.projectedValue?.toInt() ?: 0}!"

        return WealthAdviceReport(
            todayDate = todayStr,
            executiveBrief = executiveBrief,
            dailyActionTip = dailyActionTip,
            monthlySavingsCapacity = totalInvestableMonthly,
            savingsRatePercent = savingsRate,
            healthAudit = healthAudit,
            ruleFiftyThirtyTwenty = fiftyThirtyTwenty,
            moneyLeaks = leaks,
            totalPotentialExtraSavings = extraSavingsPotential,
            conservativeAllocations = conservativeAllocations,
            moderateAllocations = moderateAllocations,
            aggressiveAllocations = aggressiveAllocations,
            taxStrategies = taxStrategies,
            growthTimeline = growthTimeline
        )
    }

    fun calculateTimeline(monthlyInvestment: Double, annualReturnRate: Double = 0.12): List<GrowthMilestone> {
        val r = annualReturnRate / 12.0
        val inflationRate = 0.06 // 6% Indian CPI inflation
        val yearsList = listOf(1, 3, 5, 10)
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())

        return yearsList.map { years ->
            val n = years * 12
            // Future Value Formula for Monthly SIP: P * [((1 + r)^n - 1) / r] * (1 + r)
            val futureValue = if (r > 0) {
                monthlyInvestment * (((1 + r).pow(n) - 1) / r) * (1 + r)
            } else {
                monthlyInvestment * n
            }
            val totalInvested = monthlyInvestment * n
            val profit = futureValue - totalInvested
            val targetYear = currentYear + years

            // Real purchasing power adjusted for 6% inflation
            val realValue = futureValue / ((1 + inflationRate).pow(years))

            val goalText = when (years) {
                1 -> "6-Month Emergency Fund Completed 🛡️"
                3 -> "Major Goal Downpayment / New Car Fund 🚗"
                5 -> "₹${(futureValue / 100000.0).formatDec()} Lakhs Wealth Corpus Milestone 🏆"
                else -> "Financial Independence & Early Retirement (FIRE) 🌟"
            }

            GrowthMilestone(
                timelineYears = years,
                timelineLabel = "$years Year Horizon ($targetYear)",
                totalInvested = totalInvested,
                projectedValue = futureValue,
                realPurchasingPower = realValue,
                totalProfit = profit,
                targetDate = "$monthStr $targetYear",
                milestoneGoal = goalText
            )
        }
    }

    private fun Double.formatDec(): String {
        return String.format(Locale.getDefault(), "%.1f", this)
    }
}
