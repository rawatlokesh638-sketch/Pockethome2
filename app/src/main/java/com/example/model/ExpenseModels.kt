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
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
    }

    fun isExpense(): Boolean = type == TYPE_EXPENSE
    fun isIncome(): Boolean = type == TYPE_INCOME
}

data class Bill(
    val id: String = "",
    val userId: String = "",
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
    val month: String = "", // YYYY-MM
    val monthlyBudget: Double = 0.0, // Default 0
    val categoryBudgets: Map<String, Double> = emptyMap()
)

data class ProStatus(
    val isPro: Boolean = false,
    val status: String = "NONE", // NONE, PENDING, ACTIVE
    val expiryDate: String = "",
    val requestedAt: Long = 0
)

data class ProRequest(
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val txnId: String = "",
    val utrNumber: String = "",
    val amount: String = "25",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
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
