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

    private val chatSessionListAdapter = moshi.adapter<List<com.example.model.AiChatSession>>(
        Types.newParameterizedType(List::class.java, com.example.model.AiChatSession::class.java)
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

    private val _chatSessions = MutableStateFlow<List<com.example.model.AiChatSession>>(emptyList())
    val chatSessions = _chatSessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String>("")
    val activeSessionId = _activeSessionId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<com.example.model.AiChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _leaderboardEntries = MutableStateFlow<List<com.example.model.LeaderboardEntry>>(emptyList())

    private var txListener: ValueEventListener? = null
    private var billListener: ValueEventListener? = null
    private var budgetListener: ValueEventListener? = null
    private var proStatusListener: ValueEventListener? = null
    private var leaderboardListener: ValueEventListener? = null
    private var chatSessionsListener: ValueEventListener? = null

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
            chatSessionsListener?.let { userRef.child("chat_sessions").removeEventListener(it) }
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            budgetListener?.let { userRef.child("budgets").child(currentMonthStr).removeEventListener(it) }
            leaderboardListener?.let { database.reference.child("leaderboard_ranks").removeEventListener(it) }
        }
        txListener = null
        billListener = null
        budgetListener = null
        proStatusListener = null
        leaderboardListener = null
        chatSessionsListener = null
    }

    private fun loadLocalCacheForUser(userId: String) {
        try {
            val txJson = prefs.getString("cached_transactions_$userId", null)
            if (!txJson.isNullOrEmpty()) {
                val rawList = transactionListAdapter.fromJson(txJson) ?: emptyList()
                val list = sanitizeTransactions(rawList)
                _localTransactions.value = list
                if (list != rawList) {
                    persistLocalTransactions(userId, list)
                }
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

            val sessionsJson = prefs.getString("ai_chat_sessions_$userId", null)
            if (!sessionsJson.isNullOrEmpty()) {
                val sessions = chatSessionListAdapter.fromJson(sessionsJson) ?: emptyList()
                _chatSessions.value = sessions
                if (sessions.isNotEmpty()) {
                    val activeId = _activeSessionId.value.ifEmpty { sessions.first().id }
                    _activeSessionId.value = activeId
                    val activeSession = sessions.firstOrNull { it.id == activeId } ?: sessions.first()
                    _chatMessages.value = activeSession.messages
                } else {
                    createNewChatSession(userId)
                }
            } else {
                val chatJson = prefs.getString("ai_chat_history_$userId", null)
                val legacyMessages = if (!chatJson.isNullOrEmpty()) {
                    chatListAdapter.fromJson(chatJson) ?: emptyList()
                } else emptyList()

                val titleVal = if (legacyMessages.isNotEmpty()) {
                    legacyMessages.firstOrNull { it.sender == "USER" }?.text?.take(25) ?: "Financial Chat"
                } else "Financial Chat"

                val defaultSession = com.example.model.AiChatSession(
                    id = UUID.randomUUID().toString(),
                    title = titleVal,
                    messages = legacyMessages,
                    timestamp = System.currentTimeMillis()
                )
                _chatSessions.value = listOf(defaultSession)
                _activeSessionId.value = defaultSession.id
                _chatMessages.value = legacyMessages
                persistChatSessions(userId, listOf(defaultSession))
            }
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error reading local cache for user $userId", e)
            _localTransactions.value = emptyList()
            _localBills.value = emptyList()
            _localBudget.value = Budget()
            _chatMessages.value = emptyList()
        }
    }

    fun sanitizeTransactions(list: List<Transaction>): List<Transaction> {
        return list.map { tx ->
            val lowerNotes = tx.notes.lowercase()
            val lowerTitle = tx.title.lowercase()

            val isDebitOrMerchant = tx.sender.contains("PUREBR", ignoreCase = true) ||
                tx.notes.contains("PUREBR", ignoreCase = true) ||
                tx.title.contains("Pure Brands", ignoreCase = true) ||
                lowerNotes.contains("debited") || lowerNotes.contains("spent") ||
                lowerNotes.contains("paid") || lowerNotes.contains("purchase") ||
                lowerTitle.contains("purebr") || lowerTitle.contains("pure brands") ||
                lowerTitle.contains("amazon") || lowerTitle.contains("swiggy") ||
                lowerTitle.contains("zomato") || lowerTitle.contains("blinkit") ||
                lowerTitle.contains("zepto") ||
                (tx.title.equals("Money Received", ignoreCase = true) && tx.sender.contains("-"))

            if (tx.isIncome() && isDebitOrMerchant) {
                tx.copy(
                    type = Transaction.TYPE_EXPENSE,
                    title = if (tx.title.equals("Money Received", ignoreCase = true)) {
                        if (tx.sender.contains("PUREBR", ignoreCase = true)) "Pure Brands (PUREBR)" else "Card / UPI Expense"
                    } else tx.title,
                    category = if (tx.category == "Others") "Shopping" else tx.category
                )
            } else {
                tx
            }
        }
    }

    private fun persistLocalTransactions(userId: String, list: List<Transaction>) {
        val sanitized = sanitizeTransactions(list)
        _localTransactions.value = sanitized
        try {
            val key = if (userId.isNotEmpty()) "cached_transactions_$userId" else "cached_transactions"
            prefs.edit().putString(key, transactionListAdapter.toJson(sanitized)).apply()
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
        val uid = userId.ifEmpty { getActiveUserId() }
        _localProStatus.value = proStatus
        try {
            val key = if (uid.isNotEmpty()) "cached_pro_status_$uid" else "cached_pro_status"
            prefs.edit().putString(key, proStatusAdapter.toJson(proStatus)).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Error persisting pro status", e)
        }
        if (uid.isNotEmpty()) {
            try {
                rtdb?.reference?.child("users")?.child(uid)?.child("pro_status")?.setValue(proStatus)
                rtdb?.reference?.child("users")?.child(uid)?.child("proStatus")?.setValue(proStatus)
            } catch (e: Throwable) {
                Log.w("ExpenseRepo", "Failed to sync pro status to RTDB: ${e.message}")
            }
        }
    }

    fun getCustomApiKey(): String {
        return prefs.getString("custom_gemini_api_key", "") ?: ""
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
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
                                            aiCredits = if (ps.aiCredits < 1000) 20000 else ps.aiCredits
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

        // Chat Sessions Listener
        chatSessionsListener?.let { userRef.child("chat_sessions").removeEventListener(it) }
        chatSessionsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val sessions = mutableListOf<com.example.model.AiChatSession>()
                    snapshot.children.forEach { child ->
                        try {
                            child.getValue(com.example.model.AiChatSession::class.java)?.let {
                                sessions.add(it)
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepo", "Error parsing chat session from RTDB", e)
                        }
                    }
                    if (sessions.isNotEmpty()) {
                        _chatSessions.value = sessions
                        val activeId = _activeSessionId.value.ifEmpty { sessions.first().id }
                        _activeSessionId.value = activeId
                        val activeSession = sessions.firstOrNull { it.id == activeId } ?: sessions.first()
                        _chatMessages.value = activeSession.messages
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB chat sessions listen cancelled: ${error.message}")
            }
        }
        userRef.child("chat_sessions").addValueEventListener(chatSessionsListener!!)

        // Leaderboard Listener - 100% Pure Realtime Database Read & Write
        leaderboardListener?.let { database.reference.child("leaderboard_ranks").removeEventListener(it) }
        leaderboardListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.example.model.LeaderboardEntry>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        try {
                            // 1. Direct class parsing
                            val direct = try {
                                child.getValue(com.example.model.LeaderboardEntry::class.java)
                            } catch (_: Exception) { null }

                            val entry = direct ?: run {
                                // 2. Robust map field fallback if field names vary in RTDB
                                val key = child.key ?: ""
                                val name = child.child("handleName").getValue(String::class.java)
                                    ?: child.child("name").getValue(String::class.java)
                                    ?: child.child("userName").getValue(String::class.java)
                                    ?: child.child("displayName").getValue(String::class.java)
                                    ?: "Saver_${key.takeLast(4)}"

                                val xp = (child.child("xpPoints").value as? Long)?.toInt()
                                    ?: (child.child("xp").value as? Long)?.toInt()
                                    ?: (child.child("points").value as? Long)?.toInt()
                                    ?: (child.child("xpPoints").value as? String)?.toIntOrNull()
                                    ?: 0

                                val health = (child.child("financialHealthScore").value as? Long)?.toInt()
                                    ?: (child.child("healthScore").value as? Long)?.toInt()
                                    ?: 50

                                val tName = child.child("tierName").getValue(String::class.java)
                                    ?: com.example.model.UserLevelTier.getTierForXp(xp).displayName
                                val tEmoji = child.child("tierEmoji").getValue(String::class.java)
                                    ?: com.example.model.UserLevelTier.getTierForXp(xp).emoji

                                com.example.model.LeaderboardEntry(
                                    userId = key,
                                    handleName = name,
                                    avatarSeed = (key.hashCode() and 0x7FFFFFFF) % 10 + 1,
                                    xpPoints = xp,
                                    tierName = tName,
                                    tierEmoji = tEmoji,
                                    financialHealthScore = health
                                )
                            }

                            if (entry.userId.isNotBlank()) {
                                list.add(entry)
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepo", "Error parsing leaderboard entry from RTDB", e)
                        }
                    }
                }

                val activeUid = getActiveUserId()
                val userXp = prefs.getInt("cached_user_xp_$activeUid", 0)
                val userTier = com.example.model.UserLevelTier.getTierForXp(userXp)
                val userName = prefs.getString("cached_user_name_$activeUid", "You") ?: "You"

                // Ensure active user entry is present in list
                if (list.none { it.userId == activeUid }) {
                    val currentUserEntry = com.example.model.LeaderboardEntry(
                        userId = activeUid,
                        handleName = if (userName != "User" && userName.isNotBlank()) userName else "Saver_${activeUid.takeLast(4)}",
                        avatarSeed = (activeUid.hashCode() and 0x7FFFFFFF) % 10 + 1,
                        xpPoints = userXp,
                        tierName = userTier.displayName,
                        tierEmoji = userTier.emoji,
                        financialHealthScore = 75,
                        isCurrentUser = true
                    )
                    list.add(currentUserEntry)
                    try {
                        database.reference.child("leaderboard_ranks").child(activeUid).setValue(currentUserEntry)
                    } catch (e: Exception) {
                        Log.w("ExpenseRepo", "Could not write leaderboard rank: ${e.message}")
                    }
                }

                val sorted = list.sortedByDescending { it.xpPoints }.mapIndexed { index, item ->
                    item.copy(
                        rank = index + 1,
                        isCurrentUser = (item.userId == activeUid)
                    )
                }
                _leaderboardEntries.value = sorted
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ExpenseRepo", "RTDB leaderboard listen cancelled: ${error.message}")
                // Fallback for current user
                val activeUid = getActiveUserId()
                val userXp = prefs.getInt("cached_user_xp_$activeUid", 0)
                val userTier = com.example.model.UserLevelTier.getTierForXp(userXp)
                val userName = prefs.getString("cached_user_name_$activeUid", "You") ?: "You"
                val currentUserEntry = com.example.model.LeaderboardEntry(
                    userId = activeUid,
                    handleName = if (userName != "User" && userName.isNotBlank()) userName else "Saver_${activeUid.takeLast(4)}",
                    avatarSeed = (activeUid.hashCode() and 0x7FFFFFFF) % 10 + 1,
                    xpPoints = userXp,
                    tierName = userTier.displayName,
                    tierEmoji = userTier.emoji,
                    financialHealthScore = 75,
                    isCurrentUser = true,
                    rank = 1
                )
                if (_leaderboardEntries.value.isEmpty()) {
                    _leaderboardEntries.value = listOf(currentUserEntry)
                }
            }
        }
        try {
            database.reference.child("leaderboard_ranks").addValueEventListener(leaderboardListener!!)
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "Could not add listener to leaderboard_ranks: ${e.message}")
        }
    }

    fun syncUserProfile(userId: String, name: String, email: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val profileData = mapOf(
            "uid" to uid,
            "displayName" to name,
            "email" to email,
            "lastActive" to System.currentTimeMillis()
        )
        try {
            rtdb?.reference?.child("users")?.child(uid)?.child("profile")?.updateChildren(profileData)
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "Failed to sync user profile: ${e.message}")
        }
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

    fun observeLeaderboard(): Flow<List<com.example.model.LeaderboardEntry>> {
        return _leaderboardEntries.asStateFlow()
    }

    fun syncUserXpAndHealthScore(userId: String, xp: Int, healthScore: Int, userName: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val tier = com.example.model.UserLevelTier.getTierForXp(xp)
        val handle = if (userName.isNotBlank() && userName != "User") userName else "Saver_${uid.takeLast(4)}"
        val avatarSeed = (uid.hashCode() and 0x7FFFFFFF) % 10 + 1

        val entry = com.example.model.LeaderboardEntry(
            userId = uid,
            handleName = handle,
            avatarSeed = avatarSeed,
            xpPoints = xp,
            tierName = tier.displayName,
            tierEmoji = tier.emoji,
            financialHealthScore = healthScore,
            isCurrentUser = true
        )

        prefs.edit().putInt("cached_user_xp_$uid", xp).apply()
        prefs.edit().putString("cached_user_name_$uid", handle).apply()

        // Pure RTDB write
        try {
            rtdb?.reference?.child("leaderboard_ranks")?.child(uid)?.setValue(entry)
            rtdb?.reference?.child("users")?.child(uid)?.child("leaderboard_entry")?.setValue(entry)
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "Failed to write leaderboard entry to RTDB: ${e.message}")
        }
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

    fun toggleTransactionType(userId: String, transactionId: String, onSuccess: (Transaction) -> Unit = {}) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val currentList = _localTransactions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transactionId }
        if (index >= 0) {
            val old = currentList[index]
            val newType = if (old.isExpense()) Transaction.TYPE_INCOME else Transaction.TYPE_EXPENSE
            val newTitle = if (newType == Transaction.TYPE_EXPENSE && old.title.equals("Money Received", ignoreCase = true)) {
                if (old.sender.isNotEmpty()) {
                    val clean = old.sender.substringAfter("-").substringBefore("-").ifEmpty { "Expense" }
                    "$clean Purchase"
                } else "Expense"
            } else if (newType == Transaction.TYPE_INCOME && old.title.contains("Purchase", ignoreCase = true)) {
                "Income / Refund"
            } else old.title
            val updated = old.copy(type = newType, title = newTitle)
            currentList[index] = updated
            persistLocalTransactions(uid, currentList)
            rtdb?.reference?.child("users")?.child(uid)?.child("transactions")?.child(transactionId)?.setValue(updated)
            onSuccess(updated)
        }
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
        return deductAiTokens(userId, 50)
    }

    fun deductAiTokens(userId: String = "", tokensConsumed: Int = 50): Int {
        val uid = userId.ifEmpty { getActiveUserId() }
        val current = _localProStatus.value
        val newCredits = (current.aiCredits - tokensConsumed).coerceAtLeast(0)
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
            try {
                db.reference.child("pro_requests").child(userId).setValue(reqData)
                db.reference.child("proRequests").child(userId).setValue(reqData)
                db.reference.child("pro_requests_history").push().setValue(reqData)
                db.reference.child("users").child(userId).child("pro_status").setValue(pendingStatus)
                db.reference.child("users").child(userId).child("proStatus").setValue(pendingStatus)
            } catch (e: Exception) {
                Log.w("ExpenseRepo", "Failed to submit pro request to RTDB: ${e.message}")
            }
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

    private fun persistChatSessions(userId: String, sessions: List<com.example.model.AiChatSession>) {
        val uid = userId.ifEmpty { getActiveUserId() }
        _chatSessions.value = sessions
        try {
            val json = chatSessionListAdapter.toJson(sessions)
            prefs.edit().putString("ai_chat_sessions_$uid", json).apply()
        } catch (e: Throwable) {
            Log.e("ExpenseRepo", "Failed to persist chat sessions locally", e)
        }
        // Write to RTDB under both users/$uid/chat_sessions and users/$uid/chatSessions
        try {
            rtdb?.reference?.child("users")?.child(uid)?.child("chat_sessions")?.setValue(sessions)
            rtdb?.reference?.child("users")?.child(uid)?.child("chatSessions")?.setValue(sessions)
        } catch (e: Throwable) {
            Log.w("ExpenseRepo", "Failed to persist chat sessions to RTDB: ${e.message}")
        }
    }

    fun saveChatMessage(userId: String, message: com.example.model.AiChatMessage) {
        val uid = userId.ifEmpty { getActiveUserId() }
        var currentSessions = _chatSessions.value.toMutableList()
        var activeId = _activeSessionId.value

        if (activeId.isEmpty() || currentSessions.none { it.id == activeId }) {
            val newSession = com.example.model.AiChatSession(id = UUID.randomUUID().toString(), title = message.text.take(25).ifBlank { "Financial Chat" })
            currentSessions.add(0, newSession)
            activeId = newSession.id
            _activeSessionId.value = activeId
        }

        val sessionIndex = currentSessions.indexOfFirst { it.id == activeId }
        if (sessionIndex >= 0) {
            val session = currentSessions[sessionIndex]
            val updatedMessages = session.messages.toMutableList()
            updatedMessages.add(message)
            val updatedTitle = if (session.messages.isEmpty() && message.sender == "USER") message.text.take(28) else session.title
            val updatedSession = session.copy(title = updatedTitle, messages = updatedMessages, timestamp = System.currentTimeMillis())
            currentSessions[sessionIndex] = updatedSession
            _chatMessages.value = updatedMessages
            persistChatSessions(uid, currentSessions)
        }
    }

    fun updateChatMessageText(userId: String, messageId: String, newText: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val currentSessions = _chatSessions.value.toMutableList()
        val activeId = _activeSessionId.value
        val sessionIndex = currentSessions.indexOfFirst { it.id == activeId }
        if (sessionIndex >= 0) {
            val session = currentSessions[sessionIndex]
            val updatedMessages = session.messages.toMutableList()
            val msgIndex = updatedMessages.indexOfFirst { it.id == messageId }
            if (msgIndex >= 0) {
                updatedMessages[msgIndex] = updatedMessages[msgIndex].copy(text = newText)
                val updatedSession = session.copy(messages = updatedMessages)
                currentSessions[sessionIndex] = updatedSession
                _chatMessages.value = updatedMessages
                persistChatSessions(uid, currentSessions)
            }
        }
    }

    fun createNewChatSession(userId: String): String {
        val uid = userId.ifEmpty { getActiveUserId() }
        val newSession = com.example.model.AiChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Conversation",
            messages = emptyList(),
            timestamp = System.currentTimeMillis()
        )
        val currentSessions = _chatSessions.value.toMutableList()
        currentSessions.add(0, newSession)
        _activeSessionId.value = newSession.id
        _chatMessages.value = emptyList()
        persistChatSessions(uid, currentSessions)
        return newSession.id
    }

    fun selectChatSession(userId: String, sessionId: String) {
        val sessions = _chatSessions.value
        val target = sessions.firstOrNull { it.id == sessionId }
        if (target != null) {
            _activeSessionId.value = target.id
            _chatMessages.value = target.messages
        }
    }

    fun deleteChatSession(userId: String, sessionId: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val currentSessions = _chatSessions.value.filterNot { it.id == sessionId }.toMutableList()
        if (currentSessions.isEmpty()) {
            val newSession = com.example.model.AiChatSession(id = UUID.randomUUID().toString(), title = "Financial Chat", messages = emptyList())
            currentSessions.add(newSession)
        }
        val nextActiveId = if (_activeSessionId.value == sessionId) currentSessions.first().id else _activeSessionId.value
        _activeSessionId.value = nextActiveId
        _chatMessages.value = currentSessions.firstOrNull { it.id == nextActiveId }?.messages ?: emptyList()
        persistChatSessions(uid, currentSessions)
    }

    fun clearChatHistory(userId: String) {
        val uid = userId.ifEmpty { getActiveUserId() }
        val freshSession = com.example.model.AiChatSession(id = UUID.randomUUID().toString(), title = "Financial Chat", messages = emptyList())
        _activeSessionId.value = freshSession.id
        _chatMessages.value = emptyList()
        persistChatSessions(uid, listOf(freshSession))
        prefs.edit().remove("ai_chat_history_$uid").apply()
    }
}
