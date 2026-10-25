package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ExpenseRepository
import com.example.model.Bill
import com.example.model.Budget
import com.example.model.ExpenseCategories
import com.example.model.Transaction
import com.example.ui.components.ChartBarData
import com.example.ui.components.ChartTimeframe
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpend(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

enum class SortOption {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
}

enum class DateFilterOption {
    ALL, TODAY, THIS_MONTH, THIS_YEAR
}

enum class HomeTimeframe {
    THIS_MONTH, THIS_YEAR, ALL_TIME
}

enum class ReportsTimeframe {
    MONTHLY, YEARLY, ALL_TIME, CUSTOM
}

enum class AppTab {
    HOME, TRANSACTIONS, REPORTS, BILLS, BUDGET, CHAT, LEADERBOARD
}

enum class AuthMode {
    LOGIN, SIGNUP, DASHBOARD
}

data class CategoryBudgetStatus(
    val category: String,
    val spent: Double,
    val allTimeSpent: Double = 0.0,
    val limit: Double,
    val percentage: Float
)

data class DashboardUiState(
    val currentMonthExpenses: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val totalAllTimeExpenses: Double = 0.0,
    val totalAllTimeIncome: Double = 0.0,
    val periodExpenses: Double = 0.0,
    val periodIncome: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val categoryBudgets: Map<String, Double> = emptyMap(),
    val categoryBudgetStatuses: List<CategoryBudgetStatus> = emptyList(),
    val budgetUsedPercent: Float = 0f,
    val todaySpending: Double = 0.0,
    val lastMonthExpenses: Double = 0.0,
    val monthOverMonthDiff: Double = 0.0,
    val monthOverMonthPercent: Float = 0f,
    val isSpendHigherThanLastMonth: Boolean = false,
    val incomePercentVsLastMonth: Float = 0f,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val upcomingBills: List<Bill> = emptyList(),
    val activeTab: AppTab = AppTab.HOME,
    val authMode: AuthMode = AuthMode.LOGIN,
    val authLoading: Boolean = false,
    val authError: String? = null,
    val isUserLoggedIn: Boolean = false,
    val homeTimeframe: HomeTimeframe = HomeTimeframe.THIS_MONTH,
    val reportsTimeframe: ReportsTimeframe = ReportsTimeframe.MONTHLY,
    val selectedCategoryFilter: String = "ALL",
    val selectedPaymentFilter: String = "ALL",
    val selectedDateFilter: DateFilterOption = DateFilterOption.ALL,
    val selectedSortOption: SortOption = SortOption.DATE_DESC,
    val searchQuery: String = "",
    val activeUserId: String = "",
    val userName: String = "User",
    val currentDateDisplay: String = "",
    val activeMonthDisplay: String = "",
    val isProUser: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val deviceLocation: String = "",
    val scannedSmsCount: Int = 0,
    val proStatus: com.example.model.ProStatus = com.example.model.ProStatus(),
    val isAiGenerating: Boolean = false,
    val userXp: Int = 0,
    val financialHealthScoreInt: Int = 50,
    val userTier: com.example.model.UserLevelTier = com.example.model.UserLevelTier.IRON,
    val leaderboardEntries: List<com.example.model.LeaderboardEntry> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ExpenseRepository(application.applicationContext)

    private val _localUserName = MutableStateFlow("User")
    private val _isProUser = MutableStateFlow(false)
    private val _hasSmsPermission = MutableStateFlow(false)
    private val _hasLocationPermission = MutableStateFlow(false)
    private val _deviceLocation = MutableStateFlow("")
    private val _scannedSmsCount = MutableStateFlow(0)
    private val _isAiGenerating = MutableStateFlow(false)

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    private val _authMode = MutableStateFlow(if (Firebase.auth.currentUser != null) AuthMode.DASHBOARD else AuthMode.LOGIN)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _isUserLoggedIn = MutableStateFlow(Firebase.auth.currentUser != null)

    private val _homeTimeframe = MutableStateFlow(HomeTimeframe.THIS_MONTH)
    private val _reportsTimeframe = MutableStateFlow(ReportsTimeframe.MONTHLY)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    private val _selectedPaymentFilter = MutableStateFlow("ALL")
    private val _selectedDateFilter = MutableStateFlow(DateFilterOption.ALL)
    private val _selectedSortOption = MutableStateFlow(SortOption.DATE_DESC)

    private val _activeUserId = MutableStateFlow(repository.getActiveUserId())
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    private val _budget = MutableStateFlow(Budget())
    private val _proStatus = MutableStateFlow(com.example.model.ProStatus())

    private fun getCurrentMonthStr(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    private fun getCurrentYearStr(): String = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    private val currentMonthDisplay: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    private val currentDateDisplay: String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())

    private val navFlow = combine(_activeTab, _homeTimeframe, _reportsTimeframe, _searchQuery) { tab, h, r, q ->
        Quad(tab, h, r, q)
    }

    private val authFlow = combine(_authMode, _authLoading, _authError, _isUserLoggedIn) { mode, loading, error, loggedIn ->
        Quad(mode, loading, error, loggedIn)
    }

    private val combinedDataFlow = combine(_transactions, _bills, _budget, _proStatus) { txs, bills, budget, pro ->
        DataQuad(txs, bills, budget, pro)
    }

    val chatMessages = repository.chatMessages

    private val filterFlow = combine(
        navFlow,
        authFlow,
        combine(_selectedCategoryFilter, _selectedPaymentFilter, _selectedDateFilter, _selectedSortOption) { c, p, d, s ->
            Quad(c, p, d, s)
        }
    ) { nav, auth, f ->
        FilterState(
            nav.first, nav.second, nav.third, nav.fourth,
            auth.first, auth.second, auth.third, auth.fourth,
            f.first, f.second, f.third, f.fourth
        )
    }

    private val _leaderboard = MutableStateFlow<List<com.example.model.LeaderboardEntry>>(emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(combinedDataFlow, filterFlow, _isAiGenerating, _leaderboard) { data, filters, isGen, leaderboard ->
        val (allTransactions, allBills, budget, pro) = data
        computeState(allTransactions, allBills, budget, pro, filters).copy(
            isAiGenerating = isGen,
            leaderboardEntries = leaderboard
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            currentDateDisplay = currentDateDisplay,
            activeMonthDisplay = currentMonthDisplay
        )
    )

    init {
        viewModelScope.launch {
            repository.observeTransactions().collect { list ->
                _transactions.value = list
            }
        }
        viewModelScope.launch {
            repository.observeBills().collect { list ->
                _bills.value = list
            }
        }
        viewModelScope.launch {
            repository.observeBudget().collect { b ->
                _budget.value = b
            }
        }
        viewModelScope.launch {
            repository.observeProStatus().collect { ps ->
                _proStatus.value = ps
                _isProUser.value = ps.isPro || ps.status == "ACTIVE"
            }
        }
        viewModelScope.launch {
            repository.observeLeaderboard().collect { list ->
                _leaderboard.value = list
            }
        }
        setupAuthListener()
    }

    private fun setupAuthListener() {
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            val loggedIn = user != null
            _isUserLoggedIn.value = loggedIn
            if (loggedIn) {
                _authMode.value = AuthMode.DASHBOARD
                _activeUserId.value = user!!.uid
                _localUserName.value = user.displayName ?: user.email?.substringBefore("@") ?: "User"
                repository.onUserChanged(user.uid)
            } else {
                _authMode.value = AuthMode.LOGIN
                _activeUserId.value = ""
                repository.onUserLoggedOut()
            }
        }
    }

    fun refreshData(uid: String) {
        repository.onUserChanged(uid)
    }

    private fun formatAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: "Authentication failed"
        if (msg.contains("not allowed", ignoreCase = true) || msg.contains("disabled", ignoreCase = true)) {
            return "⚠️ Firebase Console me Email/Password provider Disabled hai! Pehle Firebase Console > Authentication > Sign-in method me jaakar 'Email/Password' ko Enable (ON) karein."
        }
        return msg
    }

    fun setAuthError(error: String?) {
        _authError.value = error
    }

    fun login(email: String, pass: String) {
        _authLoading.value = true
        _authError.value = null
        Firebase.auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener {
                _authLoading.value = false
            }
            .addOnFailureListener { e ->
                _authLoading.value = false
                _authError.value = formatAuthError(e)
            }
    }

    fun signup(name: String, email: String, pass: String) {
        _authLoading.value = true
        _authError.value = null
        Firebase.auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name.trim()
                }
                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    _authLoading.value = false
                }
            }
            .addOnFailureListener { e ->
                _authLoading.value = false
                _authError.value = formatAuthError(e)
            }
    }

    fun logout() {
        Firebase.auth.signOut()
    }

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _authError.value = null
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setHomeTimeframe(timeframe: HomeTimeframe) {
        _homeTimeframe.value = timeframe
    }

    fun setReportsTimeframe(timeframe: ReportsTimeframe) {
        _reportsTimeframe.value = timeframe
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setPaymentFilter(payment: String) {
        _selectedPaymentFilter.value = payment
    }

    fun setDateFilter(dateFilter: DateFilterOption) {
        _selectedDateFilter.value = dateFilter
    }

    fun setSortOption(sortOption: SortOption) {
        _selectedSortOption.value = sortOption
    }

    fun addOrUpdateTransaction(transaction: Transaction) {
        repository.saveTransaction(transaction.copy(userId = _activeUserId.value))
    }

    fun duplicateTransaction(transaction: Transaction) {
        repository.duplicateTransaction(transaction)
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(_activeUserId.value, transactionId)
    }

    fun addOrUpdateBill(bill: Bill) {
        repository.saveBill(bill.copy(userId = _activeUserId.value))
    }

    fun toggleBillPaid(billId: String, currentStatus: Boolean) {
        repository.toggleBillPaid(_activeUserId.value, billId, !currentStatus)
    }

    fun deleteBill(billId: String) {
        repository.deleteBill(_activeUserId.value, billId)
    }

    fun updateMonthlyBudget(newBudget: Double) {
        val current = _budget.value
        val b = current.copy(
            userId = _activeUserId.value,
            month = getCurrentMonthStr(),
            monthlyBudget = newBudget
        )
        repository.saveBudget(b)
    }

    fun updateCategoryBudget(category: String, limit: Double) {
        val current = _budget.value
        val updatedMap = current.categoryBudgets.toMutableMap()
        if (limit > 0) {
            updatedMap[category] = limit
        } else {
            updatedMap.remove(category)
        }
        val b = current.copy(
            userId = _activeUserId.value,
            month = getCurrentMonthStr(),
            categoryBudgets = updatedMap
        )
        repository.saveBudget(b)
    }

    fun removeCategoryBudget(category: String) {
        val current = _budget.value
        val updatedMap = current.categoryBudgets.toMutableMap()
        updatedMap.remove(category)
        val b = current.copy(
            userId = _activeUserId.value,
            month = getCurrentMonthStr(),
            categoryBudgets = updatedMap
        )
        repository.saveBudget(b)
    }

    fun setProUser(isPro: Boolean) {
        _isProUser.value = isPro
    }

    fun submitProMembershipRequest(txnId: String, utr: String, amount: String = "15", onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val user = Firebase.auth.currentUser
        val userId = user?.uid ?: repository.getActiveUserId()
        val userEmail = user?.email ?: "member@pockethome.app"
        val userName = user?.displayName ?: _localUserName.value.ifEmpty { "Pockethome Member" }

        val request = com.example.model.ProRequest(
            userId = userId,
            userEmail = userEmail,
            userName = userName,
            txnId = txnId.ifEmpty { "TXN_${System.currentTimeMillis().toString().takeLast(6)}" },
            utrNumber = utr.ifEmpty { "UTR_${System.currentTimeMillis().toString().takeLast(8)}" },
            amount = amount,
            planPeriod = if (amount == "120") "YEARLY" else "MONTHLY",
            durationDays = if (amount == "120") 365 else 30,
            requestedAt = System.currentTimeMillis(),
            status = "PENDING"
        )
        _authLoading.value = true
        repository.submitProRequest(request, 
            onSuccess = {
                _authLoading.value = false
                onSuccess()
            },
            onFailure = {
                _authLoading.value = false
                onFailure(it.localizedMessage ?: "Submission failed")
            }
        )
    }

    private fun getIsoDateStr(tx: Transaction): String {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        if (tx.date.isNotBlank()) {
            if (tx.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) {
                return tx.date.take(10)
            }
            try {
                val parsed = listOf(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault()),
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                ).firstNotNullOfOrNull {
                    try { it.parse(tx.date) } catch (e: Exception) { null }
                }
                if (parsed != null) {
                    return sdfDate.format(parsed)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return sdfDate.format(Date(tx.timestamp))
    }

    fun getChartData(timeframe: ChartTimeframe): List<ChartBarData> {
        val all = _transactions.value.filter { it.isExpense() }
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return when (timeframe) {
            ChartTimeframe.DAILY -> {
                val days = mutableListOf<ChartBarData>()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val todayStr = sdfDate.format(Date())

                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val dStr = sdfDate.format(c.time)
                    val lbl = dayFormat.format(c.time)
                    val sum = all.filter { getIsoDateStr(it) == dStr }.sumOf { it.amount }
                    days.add(ChartBarData(label = lbl, amount = sum, isHighlighted = (dStr == todayStr)))
                }
                days
            }
            ChartTimeframe.WEEKLY -> {
                val weeks = mutableListOf<ChartBarData>()
                for (w in 4 downTo 1) {
                    val startDayOffset = w * 7
                    val endDayOffset = (w - 1) * 7
                    val cStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -startDayOffset) }
                    val cEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -endDayOffset) }
                    val sDate = sdfDate.format(cStart.time)
                    val eDate = sdfDate.format(cEnd.time)
                    val sum = all.filter {
                        getIsoDateStr(it) in sDate..eDate
                    }.sumOf { it.amount }
                    weeks.add(ChartBarData(label = "Wk $w", amount = sum, isHighlighted = (w == 1)))
                }
                weeks
            }
            ChartTimeframe.MONTHLY -> {
                val months = mutableListOf<ChartBarData>()
                val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                for (m in 5 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.MONTH, -m) }
                    val mKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(c.time)
                    val mLbl = monthLabelFormat.format(c.time)
                    val sum = all.filter {
                        getIsoDateStr(it).take(7) == mKey
                    }.sumOf { it.amount }
                    months.add(ChartBarData(label = mLbl, amount = sum, isHighlighted = (mKey == currentMonthKey)))
                }
                months
            }
        }
    }

    fun updatePermissions(hasSms: Boolean) {
        _hasSmsPermission.value = hasSms
    }

    fun scanInboxSmsTransactions(context: android.content.Context, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val parsedList = com.example.service.SmsParser.scanExistingInboxSms(context, _activeUserId.value)
            val existingTransactions = _transactions.value
            val seenFingerprints = mutableSetOf<String>()
            existingTransactions.forEach { existingTx ->
                seenFingerprints.add("${existingTx.sender}_${existingTx.amount}_${existingTx.timestamp}")
                seenFingerprints.add("${existingTx.title}_${existingTx.amount}_${existingTx.date}_${existingTx.time}")
            }

            var count = 0
            for (tx in parsedList) {
                val fp1 = "${tx.sender}_${tx.amount}_${tx.timestamp}"
                val fp2 = "${tx.title}_${tx.amount}_${tx.date}_${tx.time}"

                val isDuplicate = seenFingerprints.contains(fp1) || 
                    seenFingerprints.contains(fp2) || 
                    existingTransactions.any { existingTx ->
                        existingTx.amount == tx.amount && 
                        existingTx.sender.isNotEmpty() && 
                        existingTx.sender.equals(tx.sender, ignoreCase = true) && 
                        kotlin.math.abs(existingTx.timestamp - tx.timestamp) < 60000L
                    }

                if (!isDuplicate) {
                    seenFingerprints.add(fp1)
                    seenFingerprints.add(fp2)
                    repository.saveTransaction(tx)
                    count++
                }
            }
            _scannedSmsCount.value = count
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    fun clearAllData() {
        val currentMonthStr = getCurrentMonthStr()
        repository.clearAllData(_activeUserId.value) {
            _transactions.value = emptyList()
            _bills.value = emptyList()
            _budget.value = Budget(userId = _activeUserId.value, month = currentMonthStr, monthlyBudget = 0.0)
        }
    }

    private fun computeState(
        allTransactions: List<Transaction>,
        allBills: List<Bill>,
        budget: Budget,
        proStatus: com.example.model.ProStatus,
        filters: FilterState
    ): DashboardUiState {
        val currentMonthStr = getCurrentMonthStr()
        val currentYearStr = getCurrentYearStr()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(lastMonthCal.time)

        val activeHomeTimeframe = if (filters.tab == AppTab.REPORTS) {
            when (filters.reportsTimeframe) {
                ReportsTimeframe.MONTHLY -> HomeTimeframe.THIS_MONTH
                ReportsTimeframe.YEARLY -> HomeTimeframe.THIS_YEAR
                ReportsTimeframe.ALL_TIME, ReportsTimeframe.CUSTOM -> HomeTimeframe.ALL_TIME
            }
        } else {
            filters.homeTimeframe
        }

        var expensesCurrent = 0.0
        var incomeCurrent = 0.0
        var expensesLast = 0.0
        var incomeLast = 0.0
        var todaySpend = 0.0
        var allTimeExpenses = 0.0
        var allTimeIncome = 0.0
        val categoryMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()

        for (tx in allTransactions) {
            val txEffectiveDate = getIsoDateStr(tx)
            val isCurrentPeriod = when (activeHomeTimeframe) {
                HomeTimeframe.THIS_MONTH -> txEffectiveDate.startsWith(currentMonthStr)
                HomeTimeframe.THIS_YEAR -> txEffectiveDate.startsWith(currentYearStr)
                HomeTimeframe.ALL_TIME -> true
            }

            val isLastMonth = txEffectiveDate.startsWith(lastMonthStr)

            if (tx.isExpense()) {
                allTimeExpenses += tx.amount
                if (isCurrentPeriod) {
                    expensesCurrent += tx.amount
                    categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.amount
                    categoryCountMap[tx.category] = (categoryCountMap[tx.category] ?: 0) + 1

                    if (txEffectiveDate == todayStr) {
                        todaySpend += tx.amount
                    }
                } else if (isLastMonth) {
                    expensesLast += tx.amount
                }
            } else if (tx.isIncome()) {
                allTimeIncome += tx.amount
                if (isCurrentPeriod) {
                    incomeCurrent += tx.amount
                } else if (isLastMonth) {
                    incomeLast += tx.amount
                }
            }
        }

        val remainingBalance = incomeCurrent - expensesCurrent
        val monthlyBudgetAmount = budget.monthlyBudget
        val budgetUsedPercent = if (monthlyBudgetAmount > 0) {
            ((expensesCurrent / monthlyBudgetAmount) * 100).toFloat().coerceIn(0f, 150f)
        } else 0f

        val diff = expensesCurrent - expensesLast
        val diffPercent = if (expensesLast > 0) ((diff / expensesLast) * 100).toFloat() else 0f
        val incomeDiffPercent = if (incomeLast > 0) (((incomeCurrent - incomeLast) / incomeLast) * 100).toFloat() else 0f

        // Category breakdown
        val breakdown = categoryMap.entries.map { (cat, amount) ->
            val pct = if (expensesCurrent > 0) ((amount / expensesCurrent) * 100).toFloat() else 0f
            CategorySpend(
                categoryName = cat,
                totalAmount = amount,
                percentage = pct,
                count = categoryCountMap[cat] ?: 1
            )
        }.sortedByDescending { it.totalAmount }

        // Category budget statuses
        val allRelevantCategories = (budget.categoryBudgets.keys + categoryMap.keys).toSet()
        val budgetStatuses = allRelevantCategories.map { cat ->
            val spent = categoryMap[cat] ?: 0.0
            val limit = budget.categoryBudgets[cat] ?: 0.0
            val pct = if (limit > 0) ((spent / limit) * 100).toFloat().coerceIn(0f, 150f) else 0f
            CategoryBudgetStatus(
                category = cat,
                spent = spent,
                limit = limit,
                percentage = pct
            )
        }.sortedWith(compareByDescending<CategoryBudgetStatus> { it.limit > 0 }.thenByDescending { it.spent })

        // Filter and sort transactions for Expense Management
        val filtered = allTransactions.filter { tx ->
            val effectiveDate = getIsoDateStr(tx)
            val matchesCategory = filters.categoryFilter == "ALL" || tx.category.equals(filters.categoryFilter, ignoreCase = true)
            val matchesPayment = filters.paymentFilter == "ALL" || tx.paymentMethod.contains(filters.paymentFilter, ignoreCase = true)
            val matchesPeriod = when (activeHomeTimeframe) {
                HomeTimeframe.THIS_MONTH -> effectiveDate.startsWith(currentMonthStr)
                HomeTimeframe.THIS_YEAR -> effectiveDate.startsWith(currentYearStr)
                HomeTimeframe.ALL_TIME -> true
            }
            val matchesDate = when (filters.dateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> effectiveDate == todayStr
                DateFilterOption.THIS_MONTH -> effectiveDate.startsWith(currentMonthStr)
                DateFilterOption.THIS_YEAR -> effectiveDate.startsWith(currentYearStr)
            }
            val matchesQuery = if (filters.searchQuery.isBlank()) true else {
                tx.title.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.notes.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.category.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.paymentMethod.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.amount.toString().contains(filters.searchQuery)
            }
            matchesCategory && matchesPayment && matchesPeriod && matchesDate && matchesQuery
        }.let { list ->
            when (filters.sortOption) {
                SortOption.DATE_DESC -> list.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.timestamp })
                SortOption.DATE_ASC -> list.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.timestamp })
                SortOption.AMOUNT_DESC -> list.sortedByDescending { it.amount }
                SortOption.AMOUNT_ASC -> list.sortedBy { it.amount }
            }
        }

        val sortedBills = allBills.sortedWith(
            compareBy<Bill> { it.isPaid }.thenBy { it.dueDate }
        )

        val savingsRatio = if (incomeCurrent > 0) (remainingBalance / incomeCurrent).coerceIn(0.0, 1.0) else 0.0
        val budgetHealth = if (monthlyBudgetAmount > 0) (1.0 - (expensesCurrent / monthlyBudgetAmount).coerceIn(0.0, 1.0)) else 0.0
        val rawHealthScore = if (allTransactions.isEmpty()) 50.0 else ((savingsRatio * 60.0) + (budgetHealth * 40.0)).coerceIn(10.0, 100.0)
        val healthScoreInt = Math.round(rawHealthScore).toInt()

        val computedXp = if (allTransactions.isEmpty()) {
            0
        } else {
            allTransactions.size * 5
        }
        val computedTier = com.example.model.UserLevelTier.getTierForXp(computedXp)

        val userNameVal = Firebase.auth.currentUser?.displayName?.ifEmpty { _localUserName.value } ?: _localUserName.value.ifEmpty { "User" }
        repository.syncUserXpAndHealthScore(_activeUserId.value, computedXp, healthScoreInt, userNameVal)

        return DashboardUiState(
            currentMonthExpenses = expensesCurrent,
            currentMonthIncome = incomeCurrent,
            totalAllTimeExpenses = allTimeExpenses,
            totalAllTimeIncome = allTimeIncome,
            remainingBalance = remainingBalance,
            monthlyBudget = monthlyBudgetAmount,
            categoryBudgets = budget.categoryBudgets,
            categoryBudgetStatuses = budgetStatuses,
            budgetUsedPercent = budgetUsedPercent,
            todaySpending = todaySpend,
            lastMonthExpenses = expensesLast,
            monthOverMonthDiff = diff,
            monthOverMonthPercent = diffPercent,
            isSpendHigherThanLastMonth = diff > 0,
            incomePercentVsLastMonth = incomeDiffPercent,
            categoryBreakdown = breakdown,
            recentTransactions = allTransactions.take(10),
            filteredTransactions = filtered,
            upcomingBills = sortedBills,
            activeTab = filters.tab,
            authMode = filters.authMode,
            authLoading = filters.authLoading,
            authError = filters.authError,
            isUserLoggedIn = filters.isLoggedIn,
            homeTimeframe = filters.homeTimeframe,
            reportsTimeframe = filters.reportsTimeframe,
            selectedCategoryFilter = filters.categoryFilter,
            selectedPaymentFilter = filters.paymentFilter,
            selectedDateFilter = filters.dateFilter,
            selectedSortOption = filters.sortOption,
            searchQuery = filters.searchQuery,
            activeUserId = _activeUserId.value,
            userName = userNameVal,
            currentDateDisplay = currentDateDisplay,
            activeMonthDisplay = currentMonthDisplay,
            isProUser = proStatus.isPro || proStatus.status == "ACTIVE",
            proStatus = proStatus,
            hasSmsPermission = _hasSmsPermission.value,
            hasLocationPermission = _hasLocationPermission.value,
            deviceLocation = _deviceLocation.value,
            scannedSmsCount = _scannedSmsCount.value,
            userXp = computedXp,
            financialHealthScoreInt = healthScoreInt,
            userTier = computedTier
        )
    }

    private data class DataQuad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class FilterState(
        val tab: AppTab,
        val homeTimeframe: HomeTimeframe,
        val reportsTimeframe: ReportsTimeframe,
        val searchQuery: String,
        val authMode: AuthMode,
        val authLoading: Boolean,
        val authError: String?,
        val isLoggedIn: Boolean,
        val categoryFilter: String,
        val paymentFilter: String,
        val dateFilter: DateFilterOption,
        val sortOption: SortOption
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun sendMessageToAi(text: String) {
        if (text.isBlank() || _isAiGenerating.value) return
        val uid = _activeUserId.value

        // Check 20,000 monthly AI tokens limit
        val currentTokensLeft = repository.getAiCredits(uid)
        if (currentTokensLeft <= 0) {
            val userMessage = com.example.model.AiChatMessage(sender = "USER", text = text)
            repository.saveChatMessage(uid, userMessage)
            val limitMessage = com.example.model.AiChatMessage(
                sender = "AI",
                text = "🚫 Aapka 20,000 Monthly AI Tokens quota poora ho chuka hai (0/20,000 Left). Ab AI aur responses nahi de sakta. Agle billing cycle me 20,000 naye tokens milenge."
            )
            repository.saveChatMessage(uid, limitMessage)
            return
        }

        val userMessage = com.example.model.AiChatMessage(sender = "USER", text = text)
        repository.saveChatMessage(uid, userMessage)

        val aiMsgPlaceholder = com.example.model.AiChatMessage(sender = "AI", text = "...")
        repository.saveChatMessage(uid, aiMsgPlaceholder)

        _isAiGenerating.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val envKey = try { System.getenv("GEMINI_API_KEY") ?: "" } catch (e: Throwable) { "" }
                val buildEnvKey = try { BuildConfig.GEMINI_API_KEY_ENV } catch (e: Throwable) { "" }
                val buildConfigKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
                val rawKey = listOf(envKey, buildEnvKey, buildConfigKey).firstOrNull {
                    it.isNotBlank() && !it.contains("placeholder", ignoreCase = true)
                }?.trim()?.trim('"') ?: ""

                val apiKey = rawKey
                var replySuccess = false
                var finalResponseText = ""
                var realApiTokenCount: Int? = null

                if (apiKey.isNotBlank()) {
                    try {
                        val income = uiState.value.currentMonthIncome
                        val expense = uiState.value.currentMonthExpenses
                        val balance = (income - expense).coerceAtLeast(0.0)
                        val monthlyBudget = uiState.value.monthlyBudget
                        val formattedIncome = formatCurrency(income)
                        val formattedExpense = formatCurrency(expense)
                        val formattedBalance = formatCurrency(balance)
                        val formattedBudget = formatCurrency(monthlyBudget)

                        val systemInstruction = com.example.service.SystemInstruction(
                            parts = listOf(
                                com.example.service.Part(
                                    text = "You are Pockethome AI, a friendly, intelligent personal financial assistant for Indian users. " +
                                           "STRICT FORMATTING & BREVITY RULES:\n" +
                                           "1. Match user's message length: If user sends a simple short greeting like 'hi', 'hello', 'hey', 'namaste', or 'kaise ho', reply with ONLY a concise 1-2 sentence warm greeting with emojis.\n" +
                                           "2. Respond in clean ChatGPT conversational style using friendly emojis and line breaks.\n" +
                                           "3. DO NOT use raw markdown syntax like asterisks (*), double asterisks (**), or hash signs (#).\n" +
                                           "Live User Financial Context:\n" +
                                           "• Monthly Income: $formattedIncome\n" +
                                           "• Monthly Expense: $formattedExpense\n" +
                                           "• Monthly Balance: $formattedBalance\n" +
                                           "• Monthly Budget: $formattedBudget"
                                )
                            )
                        )

                        val historyList = chatMessages.value.filter { it.id != aiMsgPlaceholder.id }.takeLast(6).map { msg ->
                            com.example.service.Content(
                                parts = listOf(com.example.service.Part(text = msg.text)),
                                role = if (msg.sender == "USER") "user" else "model"
                            )
                        }
                        val currentPrompt = com.example.service.Content(
                            parts = listOf(com.example.service.Part(text = text)),
                            role = "user"
                        )
                        val fullContents = historyList + currentPrompt

                        val request = com.example.service.GenerateContentRequest(
                            contents = fullContents,
                            systemInstruction = systemInstruction
                        )

                        val modelsToTry = listOf(
                            "gemini-2.5-flash",
                            "gemini-3.1-flash-lite-preview",
                            "gemini-3.5-flash",
                            "gemini-3.6-flash",
                            "gemini-3.8-flash"
                        )

                        val moshi = com.squareup.moshi.Moshi.Builder()
                            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val adapter = moshi.adapter(com.example.service.GenerateContentResponse::class.java)

                        for (model in modelsToTry) {
                            try {
                                val streamResponseBody = com.example.service.GeminiService.api.generateContentStreamForModel(model, apiKey, request)
                                var accumulatedText = ""
                                streamResponseBody.byteStream().bufferedReader().use { reader ->
                                    var line: String?
                                    while (reader.readLine().also { line = it } != null) {
                                        val rawLine = line?.trim() ?: continue
                                        if (!rawLine.startsWith("data:")) continue
                                        val jsonPayload = rawLine.removePrefix("data:").trim()
                                        if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue
                                        try {
                                            val chunk = adapter.fromJson(jsonPayload)
                                            chunk?.usageMetadata?.totalTokenCount?.let { count ->
                                                if (count > 0) realApiTokenCount = count
                                            }
                                            val chunkText = chunk?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                            if (!chunkText.isNullOrEmpty()) {
                                                accumulatedText += chunkText
                                                repository.updateChatMessageText(uid, aiMsgPlaceholder.id, accumulatedText)
                                            }
                                        } catch (eChunk: Exception) {
                                            // ignore frame parse glitch
                                        }
                                    }
                                }

                                if (accumulatedText.isNotBlank()) {
                                    replySuccess = true
                                    finalResponseText = accumulatedText
                                    android.util.Log.d("DashboardViewModel", "Gemini stream success using model: $model, tokens: $realApiTokenCount")
                                    break
                                }
                            } catch (modelErr: Exception) {
                                android.util.Log.w("DashboardViewModel", "Stream model $model failed: ${modelErr.message}, trying next model...")
                                // Non-streamed fallback for this model
                                try {
                                    val response = com.example.service.GeminiService.api.generateContentForModel(model, apiKey, request)
                                    response.usageMetadata?.totalTokenCount?.let { count ->
                                        if (count > 0) realApiTokenCount = count
                                    }
                                    val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                    if (!textResult.isNullOrBlank()) {
                                        finalResponseText = textResult.trim()
                                        repository.updateChatMessageText(uid, aiMsgPlaceholder.id, finalResponseText)
                                        replySuccess = true
                                        break
                                    }
                                } catch (eNonStream: Exception) {
                                    // continue to next model
                                }
                            }
                        }
                    } catch (netErr: Exception) {
                        android.util.Log.e("DashboardViewModel", "Gemini API call failed: ${netErr.message}")
                    }
                }

                if (!replySuccess) {
                    finalResponseText = generateFinancialAdvisorReply(text)
                    repository.updateChatMessageText(uid, aiMsgPlaceholder.id, finalResponseText)
                }

                // Calculate & Deduct Monthly AI Tokens consumed
                val totalTokensConsumed = realApiTokenCount ?: run {
                    val promptWords = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    val promptTokens = (promptWords * 1.33).toInt().coerceAtLeast((text.length / 3.0).toInt()).coerceAtLeast(10)

                    val responseWords = finalResponseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    val responseTokens = (responseWords * 1.33).toInt().coerceAtLeast((finalResponseText.length / 3.0).toInt()).coerceAtLeast(15)

                    promptTokens + responseTokens
                }

                repository.deductAiTokens(uid, totalTokensConsumed)

            } catch (e: Exception) {
                val fallbackText = generateFinancialAdvisorReply(text)
                repository.updateChatMessageText(uid, aiMsgPlaceholder.id, fallbackText)
                repository.deductAiTokens(uid, 50)
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    val chatSessions = repository.chatSessions
    val activeSessionId = repository.activeSessionId

    fun createNewChatSession() {
        repository.createNewChatSession(_activeUserId.value)
    }

    fun selectChatSession(sessionId: String) {
        repository.selectChatSession(_activeUserId.value, sessionId)
    }

    fun deleteChatSession(sessionId: String) {
        repository.deleteChatSession(_activeUserId.value, sessionId)
    }

    fun clearChatHistory() {
        repository.clearChatHistory(_activeUserId.value)
    }

    private fun generateFinancialAdvisorReply(query: String): String {
        val q = query.lowercase().trim()
        val income = uiState.value.currentMonthIncome
        val expense = uiState.value.currentMonthExpenses
        val balance = (income - expense).coerceAtLeast(0.0)
        val formattedIncome = formatCurrency(income)
        val formattedExpense = formatCurrency(expense)
        val formattedBalance = formatCurrency(balance)

        return when {
            q in listOf("hi", "hello", "hey", "hii", "namaste", "pranam", "kaise ho", "help") || q.startsWith("hi ") || q.startsWith("hello ") || q.startsWith("hey ") -> {
                "Namaste! 🙏 Main Pockethome AI hoon. Aaj aapki finances me kya help karun? 😊"
            }
            q.contains("save") || q.contains("bachat") || q.contains("saving") || q.contains("bachao") -> {
                val targetSavings = if (income > 0) formatCurrency(income * 0.20) else "₹3,000"
                "💡 Smart Money Saving Strategy:\n\n" +
                "1. 50/30/20 Rule: Apni aay ka kam se kam 20% ($targetSavings/mahina) pehle save karein, baaki bache paise hi kharch karein.\n" +
                "2. 24-Hour Rule: Kisi bhi unplanned shopping ya online order se pehle 24 ghante intezaar karein taaki impulsive buying na ho.\n" +
                "3. Discretionary Limits: Dining out, cafes aur entertainment kharchon par 15% ki limit lagayein.\n\n" +
                "Aapke account mein is mahine $formattedBalance bache hain. Ise liquid emergency fund ya SIP mein daalna sabse behtar rahega."
            }
            q.contains("sip") || q.contains("invest") || q.contains("mutual fund") || q.contains("stock") || q.contains("share") || q.contains("grow") -> {
                val suggestedSip = if (balance > 2000) formatCurrency((balance * 0.6).coerceAtLeast(1000.0)) else "₹1,500"
                "📈 AI Money Growth & SIP Recommendation:\n\n" +
                "Aapke monthly balance ($formattedBalance) ke aadhar par suggested SIP: $suggestedSip/mahina.\n\n" +
                "Recommended Asset Allocation:\n" +
                "• 50% Nifty 50 Index Fund (Low cost, safe long-term compounding)\n" +
                "• 30% Parag Parikh Flexi Cap Fund (Balanced equity growth)\n" +
                "• 20% Short-term Liquid Debt / Gold ETF (Emergency backup cash)\n\n" +
                "⭐ Tip: Har saal apni SIP ko 10% Step-up karein. 12% compounding se agle 10 saalon mein aapka wealth corpus 3x se zyada multiply ho sakta hai!"
            }
            q.contains("emergency") || q.contains("runway") -> {
                val monthlyNeeds = if (expense > 0) expense else 15000.0
                val targetEmergency = formatCurrency(monthlyNeeds * 6)
                "🛡️ Emergency Runway Protection:\n\n" +
                "Aapke monthly kharche ke hisaab se minimum 6 mahine ka emergency fund hona chahiye:\n\n" +
                "• Target Emergency Fund: $targetEmergency\n" +
                "• Kahan rakhein: 50% High-interest Savings account + 50% Liquid Mutual Fund ya FD (Instant withdrawal facility ke sath).\n" +
                "• Emergency fund ko kabhi bhi risky assets ya locked-in funds mein mat lagayein."
            }
            q.contains("budget") || q.contains("50/30/20") || q.contains("rule") -> {
                val needs = if (income > 0) formatCurrency(income * 0.50) else "50%"
                val wants = if (income > 0) formatCurrency(income * 0.30) else "30%"
                val invest = if (income > 0) formatCurrency(income * 0.20) else "20%"
                "🎯 Golden 50/30/20 Budgeting Rule:\n\n" +
                "• 50% Needs ($needs): Ghar ka kiraya, rashan, bijli, gas, aur zaruri bills.\n" +
                "• 30% Wants ($wants): Dining out, movies, shopping, gadgets aur vacations.\n" +
                "• 20% Investments ($invest): SIP, Emergency Fund aur future wealth creation.\n\n" +
                "Pockethome Budget screen par aap category-wise limits set karke overspending alerts enable kar sakte hain!"
            }
            q.contains("debt") || q.contains("loan") || q.contains("emi") || q.contains("credit card") -> {
                "💳 Debt Freedom (Karza Mukti) Plan:\n\n" +
                "1. Avalanche Method: Sabse pehle un debts ko pay off karein jinka interest rate sabse high hai (e.g. Credit Card 36-42% interest).\n" +
                "2. Snowball Method: Chhote loans pehle close karke momentum aur peace of mind banayein.\n" +
                "3. Credit Card Rule: Kabhi bhi minimum due pay mat karein; hamesha Total Amount Due pay karein taaki penalty aur compounding interest na lage."
            }
            else -> {
                "Pockethome AI Financial Guidance:\n\n" +
                "Aapke sawal '$query' ke liye hamara AI recommendation:\n\n" +
                "• Hamesha apne monthly kharchon ko categorized rakhein taaki money leaks detect ho sakein.\n" +
                "• Current Month Overview: Aay $formattedIncome, Kharcha $formattedExpense, Bachat $formattedBalance.\n" +
                "• Har mahine aay aate hi kam se kam 20% pehle alag account ya SIP mein transfer karein.\n\n" +
                "Agar aapko specific SIP plan, budget analysis ya tax-saving tips chahiye, to batayein!"
            }
        }
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
            format.maximumFractionDigits = 0
            return try {
                format.format(amount)
            } catch (e: Exception) {
                "₹${amount.toInt()}"
            }
        }
    }
}
