package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.Bill
import com.example.model.Budget
import com.example.model.Transaction
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ExpenseRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val transactionListAdapter = moshi.adapter<List<Transaction>>(
        Types.newParameterizedType(List::class.java, Transaction::class.java)
    )
    private val billListAdapter = moshi.adapter<List<Bill>>(
        Types.newParameterizedType(List::class.java, Bill::class.java)
    )
    private val budgetAdapter = moshi.adapter(Budget::class.java)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pocket_home_prefs", Context.MODE_PRIVATE)

    // Using Realtime Database (RTDB) as requested
    private val rtdb: FirebaseDatabase? by lazy {
        try {
            ensureFirebaseInitialized()
            FirebaseDatabase.getInstance()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Failed to get RTDB instance", e)
            null
        }
    }

    private val _localTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _localBills = MutableStateFlow<List<Bill>>(emptyList())
    private val _localBudget = MutableStateFlow(Budget())

    private var txListener: ValueEventListener? = null
    private var billListener: ValueEventListener? = null
    private var budgetListener: ValueEventListener? = null

    init {
        ensureFirebaseInitialized()
        loadLocalCache()
        val uid = getActiveUserId()
        ensureDataZeroIfFirstReset(uid)
        attachRtdbListeners(uid)
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:248460314025:android:feae4bcd4fb5f7050fb0b8")
                    .setApiKey("AIzaSyAoPn8qfSI98DvPPmJZyHNVJQTkXgbRzrE")
                    .setProjectId("pocket-home-3d327")
                    .setStorageBucket("pocket-home-3d327.firebasestorage.app")
                    .setGcmSenderId("248460314025")
                    // If your RTDB is in a different region, you might need setDatabaseUrl()
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d("ExpenseRepo", "FirebaseApp initialized explicitly")
            }
        } catch (e: Throwable) {
            Log.w("ExpenseRepo", "FirebaseApp explicit init fallback: ${e.message}")
        }
    }

    fun getActiveUserId(): String {
        val authUser = try {
            Firebase.auth.currentUser
        } catch (e: Throwable) {
            null
        }
        if (authUser != null && authUser.uid.isNotEmpty()) {
            return authUser.uid
        }
        var localId = prefs.getString("local_household_id", null)
        if (localId == null) {
            localId = "household_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("local_household_id", localId).apply()
        }
        return localId
    }

    private fun loadLocalCache() {
        try {
            val txJson = prefs.getString("cached_transactions", null)
            if (!txJson.isNullOrEmpty()) {
                val list = transactionListAdapter.fromJson(txJson) ?: emptyList()
                _localTransactions.value = list
            }

            val billJson = prefs.getString("cached_bills", null)
            if (!billJson.isNullOrEmpty()) {
                val list = billListAdapter.fromJson(billJson) ?: emptyList()
                _localBills.value = list
            }

            val budgetJson = prefs.getString("cached_budget", null)
            if (!budgetJson.isNullOrEmpty()) {
                val b = budgetAdapter.fromJson(budgetJson) ?: Budget()
                _localBudget.value = b
            }
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error reading local cache", e)
        }
    }

    private fun persistLocalTransactions(list: List<Transaction>) {
        _localTransactions.value = list
        try {
            prefs.edit().putString("cached_transactions", transactionListAdapter.toJson(list)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting transactions", e)
        }
    }

    private fun persistLocalBills(list: List<Bill>) {
        _localBills.value = list
        try {
            prefs.edit().putString("cached_bills", billListAdapter.toJson(list)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting bills", e)
        }
    }

    private fun persistLocalBudget(budget: Budget) {
        _localBudget.value = budget
        try {
            prefs.edit().putString("cached_budget", budgetAdapter.toJson(budget)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting budget", e)
        }
    }

    fun attachRtdbListeners(userId: String) {
        val database = rtdb ?: return
        val userRef = database.reference.child("users").child(userId)

        // Transactions Listener
        txListener?.let { userRef.child("transactions").removeEventListener(it) }
        txListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Transaction>()
                snapshot.children.forEach { child ->
                    try {
                        child.getValue(Transaction::class.java)?.let {
                            items.add(it.copy(id = child.key ?: ""))
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRepo", "Error parsing transaction from RTDB", e)
                    }
                }
                persistLocalTransactions(items.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB transaction listen cancelled: ${error.message}")
            }
        }
        userRef.child("transactions").addValueEventListener(txListener!!)

        // Bills Listener
        billListener?.let { userRef.child("bills").removeEventListener(it) }
        billListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Bill>()
                snapshot.children.forEach { child ->
                    try {
                        child.getValue(Bill::class.java)?.let {
                            items.add(it.copy(id = child.key ?: ""))
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRepo", "Error parsing bill from RTDB", e)
                    }
                }
                persistLocalBills(items.sortedBy { it.dueDate })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB bill listen cancelled: ${error.message}")
            }
        }
        userRef.child("bills").addValueEventListener(billListener!!)

        // Budget Listener
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        budgetListener?.let { userRef.child("budgets").child(currentMonthStr).removeEventListener(it) }
        budgetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        snapshot.getValue(Budget::class.java)?.let {
                            persistLocalBudget(it.copy(id = snapshot.key ?: ""))
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRepo", "Error parsing budget from RTDB", e)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB budget listen cancelled: ${error.message}")
            }
        }
        userRef.child("budgets").child(currentMonthStr).addValueEventListener(budgetListener!!)
    }

    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        return _localTransactions.asStateFlow()
    }

    fun observeBills(userId: String): Flow<List<Bill>> {
        return _localBills.asStateFlow()
    }

    fun observeBudget(userId: String, month: String): Flow<Budget> {
        return _localBudget.asStateFlow()
    }

    fun saveTransaction(transaction: Transaction, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val userId = transaction.userId.ifEmpty { getActiveUserId() }
        val db = rtdb ?: return
        val txId = if (transaction.id.isNotEmpty()) transaction.id else UUID.randomUUID().toString()
        val data = transaction.copy(id = txId, userId = userId)

        // Instant local update
        val currentList = _localTransactions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == txId }
        if (index >= 0) currentList[index] = data else currentList.add(0, data)
        currentList.sortByDescending { it.timestamp }
        persistLocalTransactions(currentList)
        onSuccess()

        // Sync to RTDB
        db.reference.child("users").child(userId).child("transactions").child(txId)
            .setValue(data)
            .addOnFailureListener { onFailure(it) }
    }

    fun duplicateTransaction(transaction: Transaction, onSuccess: () -> Unit = {}) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val duplicated = transaction.copy(
            id = "",
            title = "${transaction.title} (Copy)",
            date = todayStr,
            time = timeStr,
            timestamp = System.currentTimeMillis()
        )
        saveTransaction(duplicated, onSuccess = onSuccess)
    }

    fun deleteTransaction(userId: String, transactionId: String, onSuccess: () -> Unit = {}) {
        val currentList = _localTransactions.value.filterNot { it.id == transactionId }
        persistLocalTransactions(currentList)
        onSuccess()

        rtdb?.reference?.child("users")?.child(userId)?.child("transactions")?.child(transactionId)
            ?.removeValue()
    }

    fun saveBill(bill: Bill, onSuccess: () -> Unit = {}) {
        val userId = bill.userId.ifEmpty { getActiveUserId() }
        val db = rtdb ?: return
        val billId = if (bill.id.isNotEmpty()) bill.id else UUID.randomUUID().toString()
        val data = bill.copy(id = billId, userId = userId)

        val currentList = _localBills.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == billId }
        if (index >= 0) currentList[index] = data else currentList.add(data)
        currentList.sortBy { it.dueDate }
        persistLocalBills(currentList)
        onSuccess()

        db.reference.child("users").child(userId).child("bills").child(billId).setValue(data)
    }

    fun toggleBillPaid(userId: String, billId: String, isPaid: Boolean, onSuccess: () -> Unit = {}) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        rtdb?.reference?.child("users")?.child(userId)?.child("bills")?.child(billId)?.updateChildren(
            mapOf("isPaid" to isPaid, "paidDate" to if (isPaid) todayStr else "")
        )?.addOnSuccessListener { onSuccess() }
    }

    fun deleteBill(userId: String, billId: String, onSuccess: () -> Unit = {}) {
        val currentList = _localBills.value.filterNot { it.id == billId }
        persistLocalBills(currentList)
        onSuccess()
        rtdb?.reference?.child("users")?.child(userId)?.child("bills")?.child(billId)?.removeValue()
    }

    fun saveBudget(budget: Budget, onSuccess: () -> Unit = {}) {
        val userId = budget.userId.ifEmpty { getActiveUserId() }
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val data = budget.copy(id = currentMonthStr, userId = userId, month = currentMonthStr)

        persistLocalBudget(data)
        onSuccess()
        rtdb?.reference?.child("users")?.child(userId)?.child("budgets")?.child(currentMonthStr)?.setValue(data)
    }

    fun clearAllData(userId: String, onComplete: () -> Unit = {}) {
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        persistLocalTransactions(emptyList())
        persistLocalBills(emptyList())
        persistLocalBudget(Budget(id = currentMonthStr, userId = userId, month = currentMonthStr, monthlyBudget = 0.0))
        prefs.edit().putBoolean("data_cleared_to_zero_v5", true).apply()
        onComplete()

        rtdb?.reference?.child("users")?.child(userId)?.removeValue()
    }

    fun ensureDataZeroIfFirstReset(userId: String) {
        val alreadyCleared = prefs.getBoolean("data_cleared_to_zero_v5", false)
        if (!alreadyCleared) clearAllData(userId)
    }
}
