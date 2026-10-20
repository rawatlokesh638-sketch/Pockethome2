package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "security_prefs")

class SecurityManager(private val context: Context) {
    private val PIN_KEY = stringPreferencesKey("app_pin")

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[PIN_KEY] = pin }
    }

    suspend fun getPin(): String? {
        val pin = context.dataStore.data.map { it[PIN_KEY] }.first()
        return if (pin.isNullOrEmpty()) null else pin
    }

    suspend fun clearPin() {
        context.dataStore.edit { it.remove(PIN_KEY) }
    }

    suspend fun hasPin(): Boolean {
        return getPin() != null
    }
}
