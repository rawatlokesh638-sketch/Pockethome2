package com.example.service

import android.content.Context
import android.database.Cursor
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val MERCHANT_PATTERN = Pattern.compile(
        """(?:at|to|towards|vpa|info)\s+([A-Za-z0-9&'\-_\s]{2,25})(?:\s+(?:on|via|ref|bal|using|thru|avl|\.|$))""",
        Pattern.CASE_INSENSITIVE
    )

    fun parseSmsBody(
        body: String,
        sender: String,
        timestamp: Long,
        userId: String,
        locationName: String = ""
    ): Transaction? {
        val lowerBody = body.lowercase()

        // Must look like a banking or financial SMS
        val isExpense = lowerBody.contains("debited") || lowerBody.contains("spent") ||
                lowerBody.contains("paid") || lowerBody.contains("withdrawn") ||
                lowerBody.contains("purchase") || lowerBody.contains("sent rs") ||
                lowerBody.contains("deducted")

        val isIncome = lowerBody.contains("credited") || lowerBody.contains("received") ||
                lowerBody.contains("deposited") || lowerBody.contains("refund") ||
                lowerBody.contains("cashback") || lowerBody.contains("salary")

        if (!isExpense && !isIncome) {
            return null
        }

        // Extract Amount
        val matcher = AMOUNT_PATTERN.matcher(body)
        if (!matcher.find()) {
            return null
        }

        val rawAmount = matcher.group(1)?.replace(",", "") ?: return null
        val amount = rawAmount.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Extract Merchant / Beneficiary
        var merchant = ""
        val mMatcher = MERCHANT_PATTERN.matcher(body)
        if (mMatcher.find()) {
            merchant = mMatcher.group(1)?.trim() ?: ""
        }

        if (merchant.isBlank()) {
            merchant = when {
                sender.contains("HDFC", ignoreCase = true) -> "HDFC Bank Transaction"
                sender.contains("SBI", ignoreCase = true) -> "SBI Bank Transaction"
                sender.contains("ICICI", ignoreCase = true) -> "ICICI Bank Transaction"
                sender.contains("AXIS", ignoreCase = true) -> "Axis Bank Transaction"
                sender.contains("PAYTM", ignoreCase = true) -> "Paytm UPI"
                sender.contains("PHONEPE", ignoreCase = true) -> "PhonePe Transaction"
                else -> if (isIncome) "Income Received" else "Bank Expense"
            }
        }

        val category = detectCategory(merchant + " " + body, isIncome)
        val paymentMethod = detectPaymentMethod(body)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

        val noteText = if (locationName.isNotEmpty()) {
            "Auto-tracked via Bank SMS • $locationName"
        } else {
            "Auto-tracked via Bank SMS ($sender)"
        }

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

    private fun detectCategory(text: String, isIncome: Boolean): String {
        val lower = text.lowercase()
        if (isIncome) {
            return if (lower.contains("salary") || lower.contains("payroll") || lower.contains("wages")) {
                "Salary"
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
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") || lower.contains("phonepe") -> "UPI (PhonePe)"
            lower.contains("credit card") || lower.contains("card ending") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs") || lower.contains("netbanking") -> "Net Banking"
            lower.contains("cash") || lower.contains("atm") -> "Cash"
            else -> "UPI (PhonePe)"
        }
    }

    /**
     * Reads existing SMS inbox to find historical transactions ("aaj tak jo bhi messages ke dwara transactions hui")
     */
    fun scanExistingInboxSms(context: Context, userId: String, locationName: String): List<Transaction> {
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
                while (c.moveToNext() && count < 150) {
                    val address = if (addressIndex != -1) c.getString(addressIndex) ?: "" else ""
                    val body = if (bodyIndex != -1) c.getString(bodyIndex) ?: "" else ""
                    val timestamp = if (dateIndex != -1) c.getLong(dateIndex) else System.currentTimeMillis()

                    val tx = parseSmsBody(body, address, timestamp, userId, locationName)
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

    /**
     * Safely reads user's current city/area name using built-in Android LocationManager and Geocoder
     */
    fun getDeviceLocationName(context: Context): String {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return ""

            var bestLocation: Location? = null
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (e: SecurityException) {
                        null
                    }
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                    }
                }
            }

            if (bestLocation != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                    val subLocality = addr.subLocality ?: ""
                    if (subLocality.isNotEmpty() && locality.isNotEmpty()) {
                        "$subLocality, $locality"
                    } else if (locality.isNotEmpty()) {
                        locality
                    } else {
                        "India"
                    }
                } else {
                    "India"
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
