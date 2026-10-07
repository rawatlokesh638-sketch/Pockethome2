package com.example.service

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.example.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:Rs\.?|INR|₹|\$|USD)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""amt(?:ount)?\s*(?:of)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""for\s*(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    private val MERCHANT_REGEX = Pattern.compile(
        """(?:at|to|towards|vpa|info|for|via)\s+([A-Za-z0-9&'\-_\s]{2,25})(?:\s+(?:on|via|ref|bal|using|thru|avl|\.|$))""",
        Pattern.CASE_INSENSITIVE
    )

    fun parseSmsBody(
        body: String,
        sender: String,
        timestamp: Long,
        userId: String
    ): Transaction? {
        val lowerBody = body.lowercase()

        // Comprehensive financial / transaction keywords (Bank, Wallet, Amazon Pay, Credit Card, UPI, OTP alerts)
        val isExpense = lowerBody.contains("debited") || lowerBody.contains("spent") ||
                lowerBody.contains("paid") || lowerBody.contains("withdrawn") ||
                lowerBody.contains("purchase") || lowerBody.contains("sent") ||
                lowerBody.contains("deducted") || lowerBody.contains("charged") ||
                lowerBody.contains("used") || lowerBody.contains("payment of") ||
                lowerBody.contains("order placed") || lowerBody.contains("bought") ||
                lowerBody.contains("transferred") || lowerBody.contains("txn of") ||
                lowerBody.contains("transaction of") || lowerBody.contains("bill paid") ||
                lowerBody.contains("auto-debited") || lowerBody.contains("auto debit") ||
                lowerBody.contains("renewed") || lowerBody.contains("subscription")

        val isIncome = lowerBody.contains("credited") || lowerBody.contains("received") ||
                lowerBody.contains("deposited") || lowerBody.contains("refund") ||
                lowerBody.contains("cashback") || lowerBody.contains("salary") ||
                lowerBody.contains("added to") || lowerBody.contains("added in") ||
                lowerBody.contains("reversed") || lowerBody.contains("topup")

        if (!isExpense && !isIncome) {
            return null
        }

        // Extract Amount using multiple fallback patterns
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(",", "")
                val parsed = raw?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    amount = parsed
                    break
                }
            }
        }

        if (amount == null || amount <= 0.0) {
            return null
        }

        // Extract Merchant / Merchant Name
        var merchant = detectKnownMerchant(lowerBody)

        if (merchant.isBlank()) {
            val mMatcher = MERCHANT_REGEX.matcher(body)
            if (mMatcher.find()) {
                merchant = mMatcher.group(1)?.trim() ?: ""
            }
        }

        if (merchant.isBlank()) {
            merchant = when {
                lowerBody.contains("amazon") -> "Amazon"
                lowerBody.contains("paytm") -> "Paytm Wallet"
                lowerBody.contains("phonepe") -> "PhonePe"
                sender.contains("HDFC", ignoreCase = true) -> "HDFC Bank"
                sender.contains("SBI", ignoreCase = true) -> "SBI Bank"
                sender.contains("ICICI", ignoreCase = true) -> "ICICI Bank"
                sender.contains("AXIS", ignoreCase = true) -> "Axis Bank"
                sender.contains("KOTAK", ignoreCase = true) -> "Kotak Bank"
                sender.contains("BOB", ignoreCase = true) -> "Bank of Baroda"
                sender.contains("PNB", ignoreCase = true) -> "PNB Bank"
                sender.contains("PAYTM", ignoreCase = true) -> "Paytm"
                sender.contains("PHONEPE", ignoreCase = true) -> "PhonePe"
                else -> if (isIncome) "Money Received" else "Card / Bank Expense"
            }
        }

        val category = detectCategory(merchant + " " + body, isIncome)
        val paymentMethod = detectPaymentMethod(body)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

        val noteText = "Auto-tracked via SMS ($sender)"

        return Transaction(
            userId = userId,
            title = merchant.take(30),
            amount = amount,
            type = if (isIncome) Transaction.TYPE_INCOME else Transaction.TYPE_EXPENSE,
            category = category,
            paymentMethod = paymentMethod,
            notes = noteText,
            date = dateStr,
            time = timeStr,
            timestamp = timestamp
        )
    }

    private fun detectKnownMerchant(lowerText: String): String {
        return when {
            lowerText.contains("amazon pay") -> "Amazon Pay"
            lowerText.contains("amazon") -> "Amazon"
            lowerText.contains("flipkart") -> "Flipkart"
            lowerText.contains("swiggy instamart") -> "Swiggy Instamart"
            lowerText.contains("swiggy") -> "Swiggy"
            lowerText.contains("zomato") -> "Zomato"
            lowerText.contains("blinkit") -> "Blinkit"
            lowerText.contains("zepto") -> "Zepto"
            lowerText.contains("bigbasket") -> "BigBasket"
            lowerText.contains("dmart") -> "DMart"
            lowerText.contains("myntra") -> "Myntra"
            lowerText.contains("ajio") -> "Ajio"
            lowerText.contains("meesho") -> "Meesho"
            lowerText.contains("nykaa") -> "Nykaa"
            lowerText.contains("uber") -> "Uber"
            lowerText.contains("ola") -> "Ola"
            lowerText.contains("rapido") -> "Rapido"
            lowerText.contains("paytm wallet") -> "Paytm Wallet"
            lowerText.contains("paytm") -> "Paytm"
            lowerText.contains("phonepe") -> "PhonePe"
            lowerText.contains("gpay") || lowerText.contains("google pay") -> "Google Pay"
            lowerText.contains("jio") -> "Jio Recharge"
            lowerText.contains("airtel") -> "Airtel Recharge"
            lowerText.contains("bookmyshow") -> "BookMyShow"
            lowerText.contains("cred") -> "CRED Bill Pay"
            else -> ""
        }
    }

    private fun detectCategory(text: String, isIncome: Boolean): String {
        val lower = text.lowercase()
        if (isIncome) {
            return if (lower.contains("salary") || lower.contains("payroll") || lower.contains("wages")) {
                "Salary"
            } else if (lower.contains("refund") || lower.contains("cashback")) {
                "Others"
            } else {
                "Others"
            }
        }

        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("mcdonald") ||
                    lower.contains("domino") || lower.contains("kfc") || lower.contains("starbucks") ||
                    lower.contains("burger") || lower.contains("restaurant") || lower.contains("cafe") ||
                    lower.contains("food") -> "Food"

            lower.contains("dmart") || lower.contains("blinkit") || lower.contains("zepto") ||
                    lower.contains("bigbasket") || lower.contains("instamart") || lower.contains("grofer") ||
                    lower.contains("grocery") || lower.contains("rashan") || lower.contains("supermarket") ||
                    lower.contains("milk") || lower.contains("sabzi") -> "Groceries"

            lower.contains("electricity") || lower.contains("bescom") || lower.contains("bses") ||
                    lower.contains("mseb") || lower.contains("water") || lower.contains("gas") ||
                    lower.contains("cylinder") || lower.contains("indane") || lower.contains("airtel") ||
                    lower.contains("jio") || lower.contains("vi ") || lower.contains("broadband") ||
                    lower.contains("wifi") || lower.contains("dth") || lower.contains("recharge") ||
                    lower.contains("billdesk") -> "Bills"

            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("petrol") || lower.contains("fuel") || lower.contains("indian oil") ||
                    lower.contains("bharat petroleum") || lower.contains("hpcl") || lower.contains("bpcl") ||
                    lower.contains("shell") || lower.contains("metro") || lower.contains("fastag") ||
                    lower.contains("toll") -> "Transport"

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("ajio") || lower.contains("meesho") || lower.contains("nykaa") ||
                    lower.contains("zara") || lower.contains("h&m") || lower.contains("shopping") ||
                    lower.contains("mall") -> "Shopping"

            lower.contains("apollo") || lower.contains("pharmeasy") || lower.contains("1mg") ||
                    lower.contains("netmeds") || lower.contains("hospital") || lower.contains("clinic") ||
                    lower.contains("pharmacy") || lower.contains("doctor") || lower.contains("medplus") -> "Health"

            lower.contains("school") || lower.contains("college") || lower.contains("tuition") ||
                    lower.contains("udemy") || lower.contains("coursera") || lower.contains("fees") ||
                    lower.contains("allen") || lower.contains("byju") -> "Education"

            else -> "Others"
        }
    }

    private fun detectPaymentMethod(body: String): String {
        val lower = body.lowercase()
        return when {
            lower.contains("amazon pay") -> "Amazon Pay Wallet"
            lower.contains("paytm wallet") -> "Paytm Wallet"
            lower.contains("phonepe wallet") -> "PhonePe Wallet"
            lower.contains("wallet") -> "Wallet"
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") || lower.contains("phonepe") -> "UPI (PhonePe)"
            lower.contains("credit card") || lower.contains("card ending") || lower.contains("sbi card") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs") || lower.contains("netbanking") -> "Net Banking"
            lower.contains("cash") || lower.contains("atm") -> "Cash"
            else -> "UPI / Digital"
        }
    }

    /**
     * Reads existing SMS inbox to find all historical transaction messages
     */
    fun scanExistingInboxSms(context: Context, userId: String): List<Transaction> {
        val results = mutableListOf<Transaction>()
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC"
            )

            cursor?.use { c ->
                val addressIndex = c.getColumnIndex("address")
                val bodyIndex = c.getColumnIndex("body")
                val dateIndex = c.getColumnIndex("date")

                var count = 0
                while (c.moveToNext() && count < 200) {
                    val address = if (addressIndex != -1) c.getString(addressIndex) ?: "" else ""
                    val body = if (bodyIndex != -1) c.getString(bodyIndex) ?: "" else ""
                    val timestamp = if (dateIndex != -1) c.getLong(dateIndex) else System.currentTimeMillis()

                    val tx = parseSmsBody(body, address, timestamp, userId)
                    if (tx != null) {
                        results.add(tx)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsParser", "Error scanning SMS inbox", e)
        }
        return results
    }
}
