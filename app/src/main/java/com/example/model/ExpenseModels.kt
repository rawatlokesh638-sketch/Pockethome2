package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val familyId: String? = null, // ADDED
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TYPE_EXPENSE, // TYPE_EXPENSE or TYPE_INCOME
    val category: String = "Groceries",
    val paymentMethod: String = "UPI (PhonePe)",
    val notes: String = "",
    val date: String = "", // YYYY-MM-DD
    val time: String = "", // HH:mm
    val isRecurring: Boolean = false,
    val recurringPeriod: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val receiptUri: String = "",
    val sender: String = "", // ADDED
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
    }

    fun isExpense(): Boolean = type.equals(TYPE_EXPENSE, ignoreCase = true) || type.equals("DEBIT", ignoreCase = true) || type.isBlank()
    fun isIncome(): Boolean = type.equals(TYPE_INCOME, ignoreCase = true) || type.equals("CREDIT", ignoreCase = true)
}

data class Bill(
    val id: String = "",
    val userId: String = "",
    val familyId: String? = null, // ADDED
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "Electricity",
    val dueDate: String = "", // YYYY-MM-DD
    val isPaid: Boolean = false,
    val paidDate: String = "",
    val recurring: String = "MONTHLY",
    val notes: String = ""
)

data class Budget(
    val id: String = "",
    val userId: String = "",
    val familyId: String? = null, // ADDED
    val month: String = "", // YYYY-MM
    val monthlyBudget: Double = 0.0, // Default 0
    val categoryBudgets: Map<String, Double> = emptyMap()
)

data class Family(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val members: List<String> = emptyList() // List of user IDs
)

data class ProStatus(
    val isPro: Boolean = false,
    val status: String = "NONE", // NONE, PENDING, ACTIVE, EXPIRED
    val planPeriod: String = "MONTHLY",
    val expiryDate: String = "",
    val expiryTimestamp: Long = 0L,
    val requestedAt: Long = 0L,
    val approvedAt: Long = 0L,
    val durationDays: Int = 30,
    val aiCredits: Int = 20000
)

data class ProRequest(
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val txnId: String = "",
    val utrNumber: String = "",
    val amount: String = "15",
    val planPeriod: String = "MONTHLY",
    val durationDays: Int = 30,
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, ACTIVE, REJECTED
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "USER", // "USER" or "AI"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AiChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Financial Chat",
    val messages: List<AiChatMessage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class UserLevelTier(
    val levelNumber: Int,
    val displayName: String,
    val emoji: String,
    val minXp: Int,
    val maxXp: Int,
    val title: String,
    val hexColor: Long
) {
    IRON(1, "Iron", "🪙", 0, 150, "Financial Beginner", 0xFF8E8E93),
    BRONZE(2, "Bronze", "🥉", 150, 350, "Budget Starter", 0xFFCD7F32),
    COPPER(3, "Copper", "🟤", 350, 600, "Money Saver", 0xFFB87333),
    SILVER(4, "Silver", "🥈", 600, 1000, "Smart Planner", 0xFFC0C0C0),
    GOLD(5, "Gold", "🥇", 1000, 1500, "Wealth Builder", 0xFFFFD700),
    PLATINUM(6, "Platinum", "💠", 1500, 2200, "Financial Strategist", 0xFF00E5FF),
    DIAMOND(7, "Diamond", "💎", 2200, 3000, "Saving Master", 0xFF00B0FF),
    RUBY(8, "Ruby", "🔴", 3000, 4000, "Invest Expert", 0xFFFF1744),
    GRANDMASTER(9, "Grandmaster", "👑", 4000, 5500, "Wealth Champion", 0xFFAA00FF),
    GOD(10, "God", "⚡", 5500, Int.MAX_VALUE, "Financial Deity", 0xFFFFAB00);

    companion object {
        fun getTierForXp(xp: Int): UserLevelTier {
            return values().lastOrNull { xp >= it.minXp } ?: IRON
        }

        fun getTierForScore(score: Int): UserLevelTier {
            return when {
                score >= 99 -> GOD
                score >= 94 -> GRANDMASTER
                score >= 88 -> RUBY
                score >= 80 -> DIAMOND
                score >= 70 -> PLATINUM
                score >= 60 -> GOLD
                score >= 50 -> SILVER
                score >= 40 -> COPPER
                score >= 30 -> BRONZE
                else -> IRON
            }
        }
    }
}

data class LeaderboardEntry(
    val userId: String = "",
    val handleName: String = "SmartSaver",
    val avatarSeed: Int = 1,
    val xpPoints: Int = 0,
    val tierName: String = "Iron",
    val tierEmoji: String = "🪙",
    val financialHealthScore: Int = 50,
    val isCurrentUser: Boolean = false,
    val rank: Int = 0
)

data class ExpenseCategoryInfo(
    val id: String,
    val name: String,
    val hindiName: String,
    val icon: ImageVector,
    val color: Color
)

object ExpenseCategories {
    // Exact 8 categories from Mockup Screen 2 (Add Expense)
    val EXPENSE_CATEGORIES = listOf(
        ExpenseCategoryInfo("Groceries", "Groceries", "Rashan", Icons.Default.ShoppingBag, CatGroceries),
        ExpenseCategoryInfo("Bills", "Bills", "Bijli / Recharge", Icons.Default.Description, CatBills),
        ExpenseCategoryInfo("Food", "Food", "Khana / Zomato", Icons.Default.Restaurant, CatFood),
        ExpenseCategoryInfo("Transport", "Transport", "Petrol / Gaadi", Icons.Default.DirectionsCar, CatTransport),
        ExpenseCategoryInfo("Health", "Health", "Dawai / Hospital", Icons.Default.Favorite, CatHealth),
        ExpenseCategoryInfo("Shopping", "Shopping", "Kapde / Mall", Icons.Default.ShoppingCart, CatShopping),
        ExpenseCategoryInfo("Education", "Education", "School / Fees", Icons.Default.School, CatEducation),
        ExpenseCategoryInfo("Others", "Others", "Anya Kharcha", Icons.Default.MoreHoriz, CatOthers)
    )

    val INCOME_CATEGORIES = listOf(
        ExpenseCategoryInfo("Salary", "Salary", "Vetan / Salary", Icons.Default.Payments, CatGroceries),
        ExpenseCategoryInfo("Business", "Business", "Vyapar / Dukan", Icons.Default.AccountBalance, CatBills),
        ExpenseCategoryInfo("Rental Income", "Rental Income", "Kiraya Prapti", Icons.Default.Home, CatFood),
        ExpenseCategoryInfo("Freelance", "Freelance", "Extra Kamai", Icons.Default.Payments, CatTransport),
        ExpenseCategoryInfo("Others", "Others", "Anya Aay", Icons.Default.MoreHoriz, CatOthers)
    )

    val BILL_CATEGORIES = listOf(
        ExpenseCategoryInfo("Electricity Bill", "Electricity Bill", "Bijli", Icons.Default.Bolt, CatFood),
        ExpenseCategoryInfo("Internet Bill", "Internet Bill", "Wi-Fi", Icons.Default.Wifi, CatBills),
        ExpenseCategoryInfo("Mobile Recharge", "Mobile Recharge", "Mobile", Icons.Default.PhoneAndroid, CatTransport),
        ExpenseCategoryInfo("LPG Cylinder", "LPG Cylinder", "Gas", Icons.Default.LocalGasStation, CatHealth),
        ExpenseCategoryInfo("Water Bill", "Water Bill", "Pani", Icons.Default.WaterDrop, CatBills),
        ExpenseCategoryInfo("House Rent", "House Rent", "Kiraya", Icons.Default.Home, CatShopping),
        ExpenseCategoryInfo("Other Bill", "Other Bill", "Anya", Icons.Default.MoreHoriz, CatOthers)
    )

    val PAYMENT_METHODS = listOf(
        "UPI (PhonePe)",
        "UPI (Google Pay)",
        "UPI (Paytm)",
        "Cash",
        "Credit Card",
        "Debit Card",
        "Net Banking"
    )

    fun getCategoryInfo(name: String): ExpenseCategoryInfo {
        return EXPENSE_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: INCOME_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: BILL_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: ExpenseCategoryInfo(name, name, name, Icons.Default.Receipt, CatOthers)
    }
}
