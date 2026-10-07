package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.ExpenseRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val repository = ExpenseRepository(context.applicationContext)
            val userId = repository.getActiveUserId()

            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: "Unknown"
                val timestamp = sms.timestampMillis

                val tx = SmsParser.parseSmsBody(body, sender, timestamp, userId)
                if (tx != null) {
                    Log.d("SmsReceiver", "Auto-detected transaction: ${tx.title} of ₹${tx.amount}")
                    repository.saveTransaction(tx)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing incoming SMS", e)
        }
    }
}
