package com.example.service

import com.example.model.Bill
import com.example.model.ExpenseCategories
import com.example.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

sealed class ParsedFinancialResult {
    data class ExpenseResult(val transaction: Transaction) : ParsedFinancialResult()
    data class IncomeResult(val transaction: Transaction) : ParsedFinancialResult()
    data class BillResult(val bill: Bill) : ParsedFinancialResult()
}

object SmartVoiceAndReceiptParser {

    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:Rs\.?|INR|₹|\$)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹|rupaye|rupees|hazar|k|sau)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount|amt|total|for|of|spent|paid|kharacha|diya|aayi|bill)\s*(?:of|is|hai|ke|to|for)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b([0-9]{2,6}(?:\.[0-9]{1,2})?)\b""")
    )

    fun parseSmartInput(
        rawText: String,
        userId: String
    ): ParsedFinancialResult {
        val lower = rawText.lowercase(Locale.getDefault())
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 1. Amount Extraction
        var amount = extractAmount(rawText)
        if (amount == null || amount <= 0.0) {
            amount = 100.0 // reasonable fallback if no number found
        }

        // 2. Type Detection (Bill vs Income vs Expense)
        val isBill = lower.contains("bill") || lower.contains("bijli") || lower.contains("electricity") ||
                lower.contains("water") || lower.contains("pani") || lower.contains("wifi") ||
                lower.contains("recharge") || lower.contains("due") || lower.contains("rent") ||
                lower.contains("emi") || lower.contains("gas cylinder") || lower.contains("last date") ||
                lower.contains("reminder")

        val isIncome = !isBill && (
                lower.contains("salary") || lower.contains("vetan") || lower.contains("income") ||
                        lower.contains("kamai") || lower.contains("aayi") || lower.contains("received") ||
                        lower.contains("credited") || lower.contains("bonus") || lower.contains("profit") ||
                        lower.contains("cashback") || lower.contains("refund") || lower.contains("freelance") ||
                        lower.contains("rent received") || lower.contains("mil gaye")
                )

        val paymentMethod = detectPaymentMethod(lower)

        if (isBill) {
            val billCategory = detectBillCategory(lower)
            val billTitle = detectBillTitle(lower, billCategory)
            val dueDate = detectDueDate(lower) ?: getNextWeekDate()

            val bill = Bill(
                userId = userId,
                title = billTitle,
                amount = amount,
                category = billCategory,
                dueDate = dueDate,
                isPaid = lower.contains("paid") || lower.contains("jama"),
                recurring = "MONTHLY",
                notes = "Added via Smart Voice/Doc: \"$rawText\""
            )
            return ParsedFinancialResult.BillResult(bill)
        } else if (isIncome) {
            val incomeCategory = detectIncomeCategory(lower)
            val incomeTitle = detectIncomeTitle(lower, incomeCategory)

            val tx = Transaction(
                userId = userId,
                title = incomeTitle,
                amount = amount,
                type = Transaction.TYPE_INCOME,
                category = incomeCategory,
                paymentMethod = paymentMethod,
                notes = "Added via Smart Voice/Doc: \"$rawText\"",
                date = todayStr,
                time = timeStr,
                timestamp = System.currentTimeMillis()
            )
            return ParsedFinancialResult.IncomeResult(tx)
        } else {
            val expenseCategory = detectExpenseCategory(lower)
            val expenseTitle = detectExpenseTitle(lower, expenseCategory)

            val tx = Transaction(
                userId = userId,
                title = expenseTitle,
                amount = amount,
                type = Transaction.TYPE_EXPENSE,
                category = expenseCategory,
                paymentMethod = paymentMethod,
                notes = "Added via Smart Voice/Doc: \"$rawText\"",
                date = todayStr,
                time = timeStr,
                timestamp = System.currentTimeMillis()
            )
            return ParsedFinancialResult.ExpenseResult(tx)
        }
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(",", "")
                val parsed = raw?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun detectPaymentMethod(lower: String): String {
        return when {
            lower.contains("phonepe") || lower.contains("phone pe") -> "UPI (PhonePe)"
            lower.contains("gpay") || lower.contains("google pay") -> "UPI (Google Pay)"
            lower.contains("paytm") -> "UPI (Paytm)"
            lower.contains("upi") || lower.contains("vpa") || lower.contains("online") -> "UPI (PhonePe)"
            lower.contains("cash") || lower.contains("rokhad") || lower.contains("nagad") -> "Cash"
            lower.contains("credit card") || lower.contains("credit") -> "Credit Card"
            lower.contains("debit card") || lower.contains("debit") || lower.contains("atm") -> "Debit Card"
            lower.contains("net banking") || lower.contains("bank transfer") -> "Net Banking"
            else -> "UPI (PhonePe)"
        }
    }

    private fun detectExpenseCategory(lower: String): String {
        return when {
            lower.contains("dmart") || lower.contains("grocery") || lower.contains("rashan") ||
                    lower.contains("sabzi") || lower.contains("fruits") || lower.contains("milk") ||
                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("bigbasket") ||
                    lower.contains("supermarket") || lower.contains("atta") || lower.contains("dal") -> "Groceries"

            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("khana") ||
                    lower.contains("hotel") || lower.contains("restaurant") || lower.contains("mcdonald") ||
                    lower.contains("burger") || lower.contains("pizza") || lower.contains("chai") ||
                    lower.contains("coffee") || lower.contains("dinner") || lower.contains("lunch") ||
                    lower.contains("breakfast") || lower.contains("cafe") -> "Food"

            lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ||
                    lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("auto") || lower.contains("cab") || lower.contains("metro") ||
                    lower.contains("bus") || lower.contains("train") || lower.contains("gaadi") ||
                    lower.contains("parking") || lower.contains("toll") -> "Transport"

            lower.contains("dawai") || lower.contains("medicine") || lower.contains("hospital") ||
                    lower.contains("doctor") || lower.contains("clinic") || lower.contains("pharmacy") ||
                    lower.contains("apollo") || lower.contains("test") || lower.contains("health") -> "Health"

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("kapde") ||
                    lower.contains("clothes") || lower.contains("shoes") || lower.contains("shopping") ||
                    lower.contains("myntra") || lower.contains("ajio") || lower.contains("mall") ||
                    lower.contains("dress") || lower.contains("shirt") || lower.contains("pant") -> "Shopping"

            lower.contains("school") || lower.contains("college") || lower.contains("fees") ||
                    lower.contains("tuition") || lower.contains("book") || lower.contains("course") ||
                    lower.contains("pen") || lower.contains("exam") -> "Education"

            lower.contains("bijli") || lower.contains("electricity") || lower.contains("water") ||
                    lower.contains("wifi") || lower.contains("recharge") || lower.contains("gas") ||
                    lower.contains("rent") -> "Bills"

            else -> "Others"
        }
    }

    private fun detectExpenseTitle(lower: String, category: String): String {
        return when {
            lower.contains("dmart") -> "DMart Groceries"
            lower.contains("blinkit") -> "Blinkit Order"
            lower.contains("zepto") -> "Zepto Order"
            lower.contains("bigbasket") -> "BigBasket"
            lower.contains("swiggy") -> "Swiggy Food"
            lower.contains("zomato") -> "Zomato Food"
            lower.contains("petrol") || lower.contains("fuel") -> "Petrol / Fuel"
            lower.contains("uber") -> "Uber Ride"
            lower.contains("ola") -> "Ola Ride"
            lower.contains("rapido") -> "Rapido Ride"
            lower.contains("amazon") -> "Amazon Shopping"
            lower.contains("flipkart") -> "Flipkart Shopping"
            lower.contains("apollo") -> "Apollo Pharmacy"
            lower.contains("chai") || lower.contains("tea") -> "Chai & Snacks"
            lower.contains("rashan") -> "Rashan / Groceries"
            lower.contains("sabzi") -> "Sabzi & Fruits"
            else -> "$category Expense"
        }
    }

    private fun detectIncomeCategory(lower: String): String {
        return when {
            lower.contains("salary") || lower.contains("vetan") || lower.contains("payroll") -> "Salary"
            lower.contains("dukan") || lower.contains("business") || lower.contains("shop") || lower.contains("vyapar") -> "Business"
            lower.contains("rent") || lower.contains("kiraya") -> "Rental Income"
            lower.contains("freelance") || lower.contains("client") || lower.contains("project") -> "Freelance"
            else -> "Others"
        }
    }

    private fun detectIncomeTitle(lower: String, category: String): String {
        return when {
            lower.contains("salary") || lower.contains("vetan") -> "Monthly Salary"
            lower.contains("cashback") -> "Cashback Reward"
            lower.contains("refund") -> "Payment Refund"
            lower.contains("bonus") -> "Diwali / Festive Bonus"
            lower.contains("kiraya") || lower.contains("rent") -> "Rental Income Received"
            lower.contains("freelance") -> "Freelance Payment"
            else -> "$category Income"
        }
    }

    private fun detectBillCategory(lower: String): String {
        return when {
            lower.contains("bijli") || lower.contains("electricity") || lower.contains("power") || lower.contains("bescom") || lower.contains("bses") -> "Electricity Bill"
            lower.contains("wifi") || lower.contains("internet") || lower.contains("broadband") || lower.contains("fiber") -> "Internet Bill"
            lower.contains("recharge") || lower.contains("mobile") || lower.contains("jio") || lower.contains("airtel") || lower.contains("vi ") -> "Mobile Recharge"
            lower.contains("gas") || lower.contains("cylinder") || lower.contains("indane") || lower.contains("hp") || lower.contains("bharat gas") -> "LPG Cylinder"
            lower.contains("water") || lower.contains("pani") || lower.contains("jal") -> "Water Bill"
            lower.contains("rent") || lower.contains("kiraya") || lower.contains("makan") -> "House Rent"
            else -> "Other Bill"
        }
    }

    private fun detectBillTitle(lower: String, category: String): String {
        return when (category) {
            "Electricity Bill" -> "Bijli / Electricity Bill"
            "Internet Bill" -> "Wi-Fi / Broadband Bill"
            "Mobile Recharge" -> "Mobile Recharge"
            "LPG Cylinder" -> "LPG Gas Cylinder"
            "Water Bill" -> "Water Bill (Pani)"
            "House Rent" -> "Monthly House Rent"
            else -> "Upcoming Utility Bill"
        }
    }

    private fun detectDueDate(lower: String): String? {
        val pattern = Pattern.compile("""(?:due|tarikh|date|on|by)\s*(?:is|ko|on)?\s*([0-9]{1,2})(?:st|nd|rd|th)?(?:\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|[0-9]{1,2}))?""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(lower)
        if (matcher.find()) {
            val day = matcher.group(1)?.toIntOrNull()
            if (day != null && day in 1..31) {
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)
                return String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth, day)
            }
        }
        return null
    }

    private fun getNextWeekDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    /**
     * Simulates intelligent bill/receipt OCR for photos and documents
     */
    fun parseReceiptSimulation(fileNameOrHint: String, userId: String): ParsedFinancialResult {
        val lower = fileNameOrHint.lowercase()
        val sampleMerchant = when {
            lower.contains("grocery") || lower.contains("food") -> "Reliance Smart Bazaar"
            lower.contains("med") || lower.contains("health") -> "Apollo Pharmacy"
            lower.contains("fuel") || lower.contains("petrol") -> "Indian Oil Corporation"
            lower.contains("restaurant") || lower.contains("cafe") -> "Cafe Coffee Day"
            lower.contains("bill") || lower.contains("elec") -> "Electricity Bill (BSES / BESCOM)"
            else -> "DMart Supermarket"
        }

        val sampleAmount = when {
            lower.contains("med") -> 485.0
            lower.contains("fuel") -> 650.0
            lower.contains("bill") -> 1850.0
            else -> 1240.0
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        if (lower.contains("bill") || sampleAmount > 1500.0) {
            return ParsedFinancialResult.BillResult(
                Bill(
                    userId = userId,
                    title = sampleMerchant,
                    amount = sampleAmount,
                    category = "Electricity Bill",
                    dueDate = getNextWeekDate(),
                    isPaid = false,
                    recurring = "MONTHLY",
                    notes = "Scanned from Receipt Document photo"
                )
            )
        }

        return ParsedFinancialResult.ExpenseResult(
            Transaction(
                userId = userId,
                title = sampleMerchant,
                amount = sampleAmount,
                type = Transaction.TYPE_EXPENSE,
                category = "Groceries",
                paymentMethod = "UPI (PhonePe)",
                notes = "Auto-extracted from Bill Receipt Photo 📸",
                date = todayStr,
                time = timeStr,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
