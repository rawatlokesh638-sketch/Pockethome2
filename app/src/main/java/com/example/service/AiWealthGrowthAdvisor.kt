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
    val totalProfit: Double,
    val targetDate: String,
    val milestoneGoal: String
)

data class WealthAdviceReport(
    val todayDate: String,
    val dailyHeadline: String,
    val dailyActionTip: String,
    val monthlySavingsCapacity: Double,
    val savingsRatePercent: Int,
    val moneyLeaks: List<MoneyLeakTip>,
    val totalPotentialExtraSavings: Double,
    val recommendedAllocations: List<AssetAllocationItem>,
    val growthTimeline: List<GrowthMilestone>
)

object AiWealthGrowthAdvisor {

    fun generateDailyReport(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        categorySpends: List<CategorySpend>
    ): WealthAdviceReport {
        val todayStr = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())

        // Calculate base savings capacity
        val actualSavings = (monthlyIncome - monthlyExpenses).coerceAtLeast(0.0)
        val defaultSavingsBaseline = if (actualSavings > 0) actualSavings else (monthlyIncome * 0.20).coerceAtLeast(1500.0)
        val savingsRate = if (monthlyIncome > 0) ((actualSavings / monthlyIncome) * 100).roundToInt().coerceIn(0, 100) else 0

        // 1. Detect Money Leaks (Kaha Se Bachao)
        val leaks = mutableListOf<MoneyLeakTip>()
        var extraSavingsPotential = 0.0

        val foodSpend = categorySpends.firstOrNull { it.categoryName.equals("Food", ignoreCase = true) }?.totalAmount ?: 0.0
        if (foodSpend > 1000) {
            val cut = (foodSpend * 0.30).coerceAtLeast(500.0)
            leaks.add(
                MoneyLeakTip(
                    category = "Food & Dining",
                    issueText = "Bahar khana / Swiggy-Zomato pe ₹${foodSpend.toInt()} kharch ho raha hai.",
                    potentialMonthlySavings = cut,
                    actionStep = "Mahine me 2-3 baar bahar khana kam karke ghar ka swadisht khana khayein."
                )
            )
            extraSavingsPotential += cut
        } else {
            leaks.add(
                MoneyLeakTip(
                    category = "Food & Groceries",
                    issueText = "Supermarket bulk buying aur discount offers se grocery bill reduce karein.",
                    potentialMonthlySavings = 600.0,
                    actionStep = "Dmart / Local Mandi se weekly planning ke saath rashan lein."
                )
            )
            extraSavingsPotential += 600.0
        }

        val shoppingSpend = categorySpends.firstOrNull { it.categoryName.equals("Shopping", ignoreCase = true) }?.totalAmount ?: 0.0
        if (shoppingSpend > 1200) {
            val cut = (shoppingSpend * 0.35).coerceAtLeast(600.0)
            leaks.add(
                MoneyLeakTip(
                    category = "Shopping & Impulses",
                    issueText = "Online shopping / non-essential items pe ₹${shoppingSpend.toInt()} kharch hua.",
                    potentialMonthlySavings = cut,
                    actionStep = "48-Hour Rule lagayein: Kisi cheez ko khareedne se pehle 2 din wait karein."
                )
            )
            extraSavingsPotential += cut
        } else {
            leaks.add(
                MoneyLeakTip(
                    category = "Shopping & Non-essentials",
                    issueText = "Impulse flash sales aur unwanted orders ko avoid karein.",
                    potentialMonthlySavings = 800.0,
                    actionStep = "Cart me item daalkar 48 ghante baad check karein ki sach me zaroorat hai ya nahi."
                )
            )
            extraSavingsPotential += 800.0
        }

        val billSpend = categorySpends.firstOrNull { it.categoryName.equals("Bills", ignoreCase = true) }?.totalAmount ?: 0.0
        leaks.add(
            MoneyLeakTip(
                category = "Bills & Subscriptions",
                issueText = "Electricity wastage aur unused OTT / recharge subscriptions audit karein.",
                potentialMonthlySavings = 400.0,
                actionStep = "Unused OTT apps band karein aur bijli bacha kar seedhe ₹400 bachaayein."
            )
        )
        extraSavingsPotential += 400.0

        val totalInvestableMonthly = defaultSavingsBaseline + extraSavingsPotential

        // 2. Kaha Lagao (Smart Asset Allocation Breakdown)
        val allocations = listOf(
            AssetAllocationItem(
                assetName = "Nifty 50 Index / Equity Mutual Fund",
                percentage = 50,
                allocatedAmount = (totalInvestableMonthly * 0.50).roundToInt().toDouble(),
                expectedReturnRate = "12% - 14% p.a.",
                riskLevel = "Moderate (Long Term Wealth)",
                description = "Bharat ki top 50 companiyo me automatic hissa. Long term me FD se double growth deta hai.",
                recommendedWhere = "Zerodha Coin, Groww, AngelOne (Nifty 50 Direct Index SIP)"
            ),
            AssetAllocationItem(
                assetName = "Emergency Safety Reserve",
                percentage = 20,
                allocatedAmount = (totalInvestableMonthly * 0.20).roundToInt().toDouble(),
                expectedReturnRate = "6.5% - 7.5% p.a.",
                riskLevel = "Very Low (100% Safe & Instant Liquid)",
                description = "Medical ya achanak aane wale kharche ke liye 3 mahine ka kharcha safe rakhna zaroori hai.",
                recommendedWhere = "High-Yield Bank Savings Account ya Liquid Mutual Fund"
            ),
            AssetAllocationItem(
                assetName = "Sovereign Gold Bonds / Digital Gold",
                percentage = 15,
                allocatedAmount = (totalInvestableMonthly * 0.15).roundToInt().toDouble(),
                expectedReturnRate = "10% - 11% p.a. + 2.5% Interest",
                riskLevel = "Low-Moderate (Inflation Shield)",
                description = "Sona mehengai (inflation) se bachata hai. Har mahine chhota hissa gold me lagayein.",
                recommendedWhere = "RBI Sovereign Gold Bond (SGB) ya SGB secondary market"
            ),
            AssetAllocationItem(
                assetName = "Fixed Deposit (FD) / PPF / Sukanya",
                percentage = 15,
                allocatedAmount = (totalInvestableMonthly * 0.15).roundToInt().toDouble(),
                expectedReturnRate = "7.1% - 8.2% p.a. (Tax-Free)",
                riskLevel = "Zero Risk (Government Backed)",
                description = "Guaranteed tax-free income aur future security ke liye fixed return.",
                recommendedWhere = "Post Office PPF Account ya Bank Recurring Deposit (RD)"
            )
        )

        // 3. Kitna Grow Hoga & Kabb Takk (Timeline Projections at 12% blended CAGR)
        val growthTimeline = calculateTimeline(totalInvestableMonthly, annualReturnRate = 0.12)

        // 4. Daily AI Headline & Action
        val headline = if (savingsRate >= 30) {
            "Shaandaar! Aapka Savings Rate $savingsRate% hai. Aggressive SIP se financial freedom jald milegi!"
        } else if (actualSavings > 0) {
            "Aap is mahine ₹${actualSavings.toInt()} bacha rahe hain. Chhote cuts se isko ₹${totalInvestableMonthly.toInt()} kar sakte hain!"
        } else {
            "Hisaab audit: ₹${extraSavingsPotential.toInt()} ki leaking pakdi gayi hai. Inhe rok kar naya investment shuru karein!"
        }

        val dailyTip = "💡 Aaj ka AI Step: Roz ₹${(totalInvestableMonthly / 30).roundToInt()} alag karke ek Nifty 50 Index SIP shuru karein. 5 saal me ₹${growthTimeline.firstOrNull { it.timelineYears == 5 }?.projectedValue?.toInt() ?: 0} ka corpus ban sakta hai!"

        return WealthAdviceReport(
            todayDate = todayStr,
            dailyHeadline = headline,
            dailyActionTip = dailyTip,
            monthlySavingsCapacity = totalInvestableMonthly,
            savingsRatePercent = savingsRate,
            moneyLeaks = leaks,
            totalPotentialExtraSavings = extraSavingsPotential,
            recommendedAllocations = allocations,
            growthTimeline = growthTimeline
        )
    }

    fun calculateTimeline(monthlyInvestment: Double, annualReturnRate: Double = 0.12): List<GrowthMilestone> {
        val r = annualReturnRate / 12.0
        val yearsList = listOf(1, 3, 5, 10)
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())

        return yearsList.map { years ->
            val n = years * 12
            // SIP Compound Interest Formula: P * [((1 + r)^n - 1) / r] * (1 + r)
            val futureValue = if (r > 0) {
                monthlyInvestment * (((1 + r).pow(n) - 1) / r) * (1 + r)
            } else {
                monthlyInvestment * n
            }
            val totalInvested = monthlyInvestment * n
            val profit = futureValue - totalInvested
            val targetYear = currentYear + years

            val goalText = when (years) {
                1 -> "Emergency Fund Ready + 1st Milestone"
                3 -> "Car Downpayment ya Home Upgrade Buffer"
                5 -> "₹${(futureValue / 100000.0).formatDec()} Lakhs ka Big Wealth Corpus"
                else -> "Financial Independence & Early Retirement Fund"
            }

            GrowthMilestone(
                timelineYears = years,
                timelineLabel = "$years Saal Baad ($targetYear)",
                totalInvested = totalInvested,
                projectedValue = futureValue,
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
