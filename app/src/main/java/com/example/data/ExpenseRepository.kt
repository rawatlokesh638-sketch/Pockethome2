package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.Bill
import com.example.model.Budget
import com.example.model.ProRequest
import com.example.model.ProStatus
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
    private val proStatusAdapter = moshi.adapter(ProStatus::class.java)
    private val chatListAdapter = moshi.adapter<List<com.example.model.AiChatMessage>>(
        Types.newParameterizedType(List::class.java, com.example.model.AiChatMessage::class.java)
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pocket_home_prefs", Context.MODE_PRIVATE)

    // Using Realtime Database (RTDB)
    private val rtdb: FirebaseDatabase? by lazy {
        try {
            ensureFirebaseInitialized()
            FirebaseDatabase.getInstance()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Failed to get RTDB instance", e)
            null
        }
    }

    private var currentUserId: String = ""

    private val _localTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _localBills = MutableStateFlow<List<Bill>>(emptyList())
    private val _localBudget = MutableStateFlow(Budget())
    private val _localProStatus = MutableStateFlow(ProStatus())
    private val _chatMessages = MutableStateFlow<List<com.example.model.AiChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private var txListener: ValueEventListener? = null
    private var billListener: ValueEventListener? = null
    private var budgetListener: ValueEventListener? = null
    private var proStatusListener: ValueEventListener? = null

    init {
        ensureFirebaseInitialized()
        val authUser = try { Firebase.auth.currentUser } catch (e: Throwable) { null }
        val activeUid = if (authUser != null && authUser.uid.isNotEmpty()) authUser.uid else getActiveUserId()
        onUserChanged(activeUid)
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
                    .setDatabaseUrl("https://pocket-home-3d327-default-rtdb.firebaseio.com")
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
        if (currentUserId.isNotEmpty()) {
            return currentUserId
        }
        var localId = prefs.getString("local_household_id", null)
        if (localId == null) {
            localId = "household_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("local_household_id", localId).apply()
        }
        return localId
    }

    fun onUserChanged(userId: String) {
        currentUserId = userId
        loadLocalCacheForUser(userId)
        attachRtdbListeners(userId)
    }

    fun onUserLoggedOut() {
        detachRtdbListeners()
        currentUserId = ""
        _localTransactions.value = emptyList()
        _localBills.value = emptyList()
        _localBudget.value = Budget()
        _localProStatus.value = ProStatus()
    }

    private fun detachRtdbListeners() {
        val database = rtdb ?: return
        if (currentUserId.isNotEmpty()) {
            val userRef = database.reference.child("users").child(currentUserId)
            txListener?.let { userRef.child("transactions").removeEventListener(it) }
            billListener?.let { userRef.child("bills").removeEventListener(it) }
            proStatusListener?.let { userRef.child("pro_status").removeEventListener(it) }
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            budgetListener?.let { userRef.child("budgets").child(currentMonthStr).removeEventListener(it) }
        }
        txListener = null
        billListener = null
        budgetListener = null
        proStatusListener = null
    }

    private fun loadLocalCacheForUser(userId: String) {
        try {
            val txJson = prefs.getString("cached_transactions_$userId", null)
            if (!txJson.isNullOrEmpty()) {
                val list = transactionListAdapter.fromJson(txJson) ?: emptyList()
                _localTransactions.value = list
            } else {
                _localTransactions.value = emptyList()
            }

            val billJson = prefs.getString("cached_bills_$userId", null)
            if (!billJson.isNullOrEmpty()) {
                val list = billListAdapter.fromJson(billJson) ?: emptyList()
                _localBills.value = list
            } else {
                _localBills.value = emptyList()
            }

            val proStatusJson = prefs.getString("cached_pro_status_$userId", null)
            if (!proStatusJson.isNullOrEmpty()) {
                val ps = proStatusAdapter.fromJson(proStatusJson) ?: ProStatus()
                _localProStatus.value = ps
            } else {
                _localProStatus.value = ProStatus()
            }

            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val budgetJson = prefs.getString("cached_budget_$userId", null)
            if (!budgetJson.isNullOrEmpty()) {
                val b = budgetAdapter.fromJson(budgetJson) ?: Budget(id = currentMonthStr, userId = userId, month = currentMonthStr, monthlyBudget = 0.0)
                _localBudget.value = b
            } else {
                _localBudget.value = Budget(id = currentMonthStr, userId = userId, month = currentMonthStr, monthlyBudget = 0.0)
            }

            val chatJson = prefs.getString("ai_chat_history_$userId", null)
            if (!chatJson.isNullOrEmpty()) {
                val list = chatListAdapter.fromJson(chatJson) ?: emptyList()
                _chatMessages.value = list
            } else {
                _chatMessages.value = emptyList()
            }
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error reading local cache for user $userId", e)
            _localTransactions.value = emptyList()
            _localBills.value = emptyList()
            _localBudget.value = Budget()
            _chatMessages.value = emptyList()
        }
    }

    private fun persistLocalTransactions(userId: String, list: List<Transaction>) {
        _localTransactions.value = list
        try {
            val key = if (userId.isNotEmpty()) "cached_transactions_$userId" else "cached_transactions"
            prefs.edit().putString(key, transactionListAdapter.toJson(list)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting transactions", e)
        }
    }

    private fun persistLocalBills(userId: String, list: List<Bill>) {
        _localBills.value = list
        try {
            val key = if (userId.isNotEmpty()) "cached_bills_$userId" else "cached_bills"
            prefs.edit().putString(key, billListAdapter.toJson(list)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting bills", e)
        }
    }

    private fun persistLocalBudget(userId: String, budget: Budget) {
        _localBudget.value = budget
        try {
            val key = if (userId.isNotEmpty()) "cached_budget_$userId" else "cached_budget"
            prefs.edit().putString(key, budgetAdapter.toJson(budget)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting budget", e)
        }
    }

    private fun persistLocalProStatus(userId: String, proStatus: ProStatus) {
        _localProStatus.value = proStatus
        try {
            val key = if (userId.isNotEmpty()) "cached_pro_status_$userId" else "cached_pro_status"
            prefs.edit().putString(key, proStatusAdapter.toJson(proStatus)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting pro status", e)
        }
    }

    fun isProActive(userId: String = ""): Boolean {
        val targetId = userId.ifEmpty { getActiveUserId() }
        val ps = _localProStatus.value
        if (ps.isPro || ps.status == "ACTIVE") return true
        val proStatusJson = prefs.getString("cached_pro_status_$targetId", null)
        if (!proStatusJson.isNullOrEmpty()) {
            val cachedPs = proStatusAdapter.fromJson(proStatusJson)
            return cachedPs?.isPro == true || cachedPs?.status == "ACTIVE"
        }
        return false
    }

    fun attachRtdbListeners(userId: String) {
        val database = rtdb ?: return
        val userRef = database.reference.child("users").child(userId)

        // Transactions Listener
        txListener?.let { userRef.child("transactions").removeEventListener(it) }
        txListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Transaction>()
                if (snapshot.exists()) {
                    snapshot.children.forEach { child ->
                        try {
                            child.getValue(Transaction::class.java)?.let {
                                items.add(it.copy(id = child.key ?: ""))
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepo", "Error parsing transaction from RTDB", e)
                        }
                    }
                }
                // On signup with 0 records, snapshot is empty and items is emptyList()
                persistLocalTransactions(userId, items.sortedByDescending { it.timestamp })
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
                if (snapshot.exists()) {
                    snapshot.children.forEach { child ->
                        try {
                            child.getValue(Bill::class.java)?.let {
                                items.add(it.copy(id = child.key ?: ""))
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepo", "Error parsing bill from RTDB", e)
                        }
                    }
                }
                persistLocalBills(userId, items.sortedBy { it.dueDate })
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
                            persistLocalBudget(userId, it.copy(id = snapshot.key ?: ""))
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRepo", "Error parsing budget from RTDB", e)
                    }
                } else {
                    persistLocalBudget(userId, Budget(id = currentMonthStr, userId = userId, month = currentMonthStr, monthlyBudget = 0.0))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB budget listen cancelled: ${error.message}")
            }
        }
        userRef.child("budgets").child(currentMonthStr).addValueEventListener(budgetListener!!)

        // Pro Status Listener (Only active when approved in Firebase, valid for durationDays)
        proStatusListener?.let { userRef.child("pro_status").removeEventListener(it) }
        proStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        snapshot.getValue(ProStatus::class.java)?.let { ps ->
                            val currentTime = System.currentTimeMillis()
                            val durationMillis = ps.durationDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L

                            val finalStatus = when {
                                ps.status == "ACTIVE" || ps.isPro -> {
                                    val expiryTime = when {
                                        ps.expiryTimestamp > 0L -> ps.expiryTimestamp
                                        ps.approvedAt > 0L -> ps.approvedAt + durationMillis
                                        ps.requestedAt > 0L -> ps.requestedAt + durationMillis
                                        else -> currentTime + durationMillis
                                    }

                                    if (currentTime > expiryTime) {
                                        // Plan duration has completed!
                                        ps.copy(
                                            isPro = false,
                                            status = "EXPIRED",
                                            expiryTimestamp = expiryTime,
                                            aiCredits = 0
                                        )
                                    } else {
                                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        val expDateStr = ps.expiryDate.ifEmpty { sdf.format(Date(expiryTime)) }
                                        ps.copy(
                                            isPro = true,
                                            status = "ACTIVE",
                                            expiryDate = expDateStr,
                                            expiryTimestamp = expiryTime,
                                            aiCredits = ps.aiCredits.coerceAtLeast(1)
                                        )
                                    }
                                }
                                ps.status == "PENDING" -> {
                                    ps.copy(isPro = false, status = "PENDING")
                                }
                                else -> {
                                    ps.copy(isPro = false, status = "NONE")
                                }
                            }
                            persistLocalProStatus(userId, finalStatus)
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRepo", "Error parsing pro status", e)
                    }
                } else {
                    persistLocalProStatus(userId, ProStatus())
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB pro status listen cancelled: ${error.message}")
            }
        }
        userRef.child("pro_status").addValueEventListener(proStatusListener!!)
    }

    fun observeTransactions(userId: String = ""): Flow<List<Transaction>> {
        return _localTransactions.asStateFlow()
    }

    fun observeBills(userId: String = ""): Flow<List<Bill>> {
        return _localBills.asStateFlow()
    }

    fun observeBudget(userId: String = "", month: String = ""): Flow<Budget> {
        return _localBudget.asStateFlow()
    }

    fun observeProStatus(userId: String = ""): Flow<ProStatus> {
        return _localProStatus.asStateFlow()
    }

    fun saveTransaction(transaction: Transaction, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val userId = transaction.userId.ifEmpty { getActiveUserId() }
        val txId = if (transaction.id.isNotEmpty()) transaction.id else UUID.randomUUID().toString()

        // Instant local update with smart deduplication
        val currentList = _localTransactions.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.id == txId || (
                it.sender.isNotEmpty() &&
                transaction.sender.isNotEmpty() &&
                it.sender.equals(transaction.sender, ignoreCase = true) &&
                it.amount == transaction.amount &&
                kotlin.math.abs(it.timestamp - transaction.timestamp) < 60000L
            )
        }
        val finalId = if (existingIndex >= 0) currentList[existingIndex].id else txId
        val data = transaction.copy(id = finalId, userId = userId)

        if (existingIndex >= 0) {
            currentList[existingIndex] = data
        } else {
            currentList.add(0, data)
        }
        currentList.sortByDescending { it.timestamp }
        persistLocalTransactions(userId, currentList)
        onSuccess()

        // Sync to RTDB if available
        rtdb?.reference?.child("users")?.child(userId)?.child("transactions")?.child(finalId)
            ?.setValue(data)
            ?.addOnFailureListener { onFailure(it) }
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
        val uid = userId.ifEmpty { getActiveUserId() }
        val currentList = _localTransactions.value.filterNot { it.id == transactionId }
        persistLocalTransactions(uid, currentList)
        onSuccess()

        rtdb?.reference?.child("users")?.child(uid)?.child("transactions")?.child(transactionId)
            ?.removeValue()
    }

    fun saveBill(bill: Bill, onSuccess: () -> Unit = {}) {
        val userId = bill.userId.ifEmpty { getActiveUserId() }
        val billId = if (bill.id.isNotEmpty()) bill.id else UUID.randomUUID().toString()
        val data = bill.copy(id = billId, userId = userId)

        val currentList = _localBills.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == billId }
        if (index >= 0) currentList[index] = data else currentList.add(data)
        currentList.sortBy { it.dueDate }
        persistLocalBills(userId, currentList)
        onSuccess()

        rtdb?.reference?.child("users")?.child(userId)?.child("bills")?.child(billId)?.setValue(data)
    }

    fun toggleBillPaid(userId: String, billId: String, isPaid: Boolean, onSuccess: () -> Unit = {}) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentList = _localBills.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == billId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isPaid = isPaid, paidDate = if (isPaid) todayStr else "")
            persistLocalBills(uid, currentList)
        }
        onSuccess()
        rtdb?.reference?.child("users")?.child(uid)?.child("bills")?.child(billId)?.updateChildren(
            mapOf("isPaid" to isPaid, "paidDate" to if (isPaid) todayStr else "")
        )
    }

    fun deleteBill(userId: String, billId: String, onSuccess: () -> Unit = {}) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val currentList = _localBills.value.filterNot { it.id == billId }
        persistLocalBills(uid, currentList)
        onSuccess()
        rtdb?.reference?.child("users")?.child(uid)?.child("bills")?.child(billId)?.removeValue()
    }

    fun saveBudget(budget: Budget, onSuccess: () -> Unit = {}) {
        val userId = budget.userId.ifEmpty { getActiveUserId() }
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val data = budget.copy(id = currentMonthStr, userId = userId, month = currentMonthStr)

        persistLocalBudget(userId, data)
        onSuccess()
        rtdb?.reference?.child("users")?.child(userId)?.child("budgets")?.child(currentMonthStr)?.setValue(data)
    }

    fun getAiCredits(userId: String = ""): Int {
        val status = _localProStatus.value
        return if (status.isPro || status.status == "ACTIVE") {
            status.aiCredits
        } else {
            0
        }
    }

    fun deductAiCredit(userId: String = ""): Int {
        val uid = userId.ifEmpty { getActiveUserId() }
        val current = _localProStatus.value
        val newCredits = (current.aiCredits - 1).coerceAtLeast(0)
        val updated = current.copy(aiCredits = newCredits)
        persistLocalProStatus(uid, updated)
        rtdb?.reference?.child("users")?.child(uid)?.child("pro_status")?.child("aiCredits")?.setValue(newCredits)
        return newCredits
    }

    fun submitProRequest(request: ProRequest, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val userId = request.userId.ifEmpty { getActiveUserId() }
        val days = if (request.amount == "120" || request.planPeriod == "YEARLY") 365 else 30
        val pendingStatus = ProStatus(
            isPro = false,
            status = "PENDING",
            planPeriod = if (days == 365) "YEARLY" else "MONTHLY",
            durationDays = days,
            requestedAt = System.currentTimeMillis()
        )
        persistLocalProStatus(userId, pendingStatus)
        onSuccess()

        val db = rtdb
        if (db != null) {
            val reqData = request.copy(
                userId = userId,
                status = "PENDING",
                planPeriod = if (days == 365) "YEARLY" else "MONTHLY",
                durationDays = days,
                requestedAt = System.currentTimeMillis()
            )
            db.reference.child("pro_requests").child(userId).setValue(reqData)
            db.reference.child("pro_requests_history").push().setValue(reqData)
            db.reference.child("users").child(userId).child("pro_status").setValue(pendingStatus)
        }
    }

    fun clearAllData(userId: String, onComplete: () -> Unit = {}) {
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        persistLocalTransactions(userId, emptyList())
        persistLocalBills(userId, emptyList())
        persistLocalBudget(userId, Budget(id = currentMonthStr, userId = userId, month = currentMonthStr, monthlyBudget = 0.0))
        onComplete()

        rtdb?.reference?.child("users")?.child(userId)?.removeValue()
    }

    fun saveChatMessage(userId: String, message: com.example.model.AiChatMessage) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val current = _chatMessages.value.toMutableList()
        current.add(message)
        _chatMessages.value = current
        try {
            val json = chatListAdapter.toJson(current)
            prefs.edit().putString("ai_chat_history_$uid", json).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Failed to persist chat message", e)
        }
    }

    fun clearChatHistory(userId: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        _chatMessages.value = emptyList()
        prefs.edit().remove("ai_chat_history_$uid").apply()
    }
}
