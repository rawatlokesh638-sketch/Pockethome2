package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    THIS_MONTH, THIS_YEAR
}

enum class ReportsTimeframe {
    MONTHLY, YEARLY, CUSTOM
}

enum class AppTab {
    HOME, TRANSACTIONS, REPORTS, BILLS, BUDGET
}

enum class AuthMode {
    LOGIN, SIGNUP, DASHBOARD
}

data class DashboardUiState(
    val currentMonthExpenses: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val monthlyBudget: Double = 0.0,
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
    val scannedSmsCount: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ExpenseRepository(application.applicationContext)

    private val _isProUser = MutableStateFlow(false)
    private val _hasSmsPermission = MutableStateFlow(false)
    private val _hasLocationPermission = MutableStateFlow(false)
    private val _deviceLocation = MutableStateFlow("")
    private val _scannedSmsCount = MutableStateFlow(0)

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

    private val currentMonthStr: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    private val currentYearStr: String = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    private val currentMonthDisplay: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    private val currentDateDisplay: String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())

    private val dataFlow = combine(_transactions, _bills, _budget) { txs, bills, budget ->
        Triple(txs, bills, budget)
    }

    private val authFlow = combine(_authMode, _authLoading, _authError, _isUserLoggedIn) { mode, loading, error, loggedIn ->
        Quad(mode, loading, error, loggedIn)
    }

    private val navFlow = combine(_activeTab, _homeTimeframe, _reportsTimeframe, _searchQuery) { tab, h, r, q ->
        Quad(tab, h, r, q)
    }

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

    val uiState: StateFlow<DashboardUiState> = combine(dataFlow, filterFlow) { data, filters ->
        val (allTransactions, allBills, budget) = data
        computeState(allTransactions, allBills, budget, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            currentDateDisplay = currentDateDisplay,
            activeMonthDisplay = currentMonthDisplay
        )
    )

    init {
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
                refreshData(user.uid)
            } else {
                _authMode.value = AuthMode.LOGIN
                _activeUserId.value = repository.getActiveUserId() // Local ID fallback
                _transactions.value = emptyList()
                _bills.value = emptyList()
                _budget.value = Budget()
            }
        }
    }

    private fun refreshData(uid: String) {
        repository.attachRtdbListeners(uid)

        viewModelScope.launch {
            repository.observeTransactions(uid).collect { list ->
                _transactions.value = list
            }
        }

        viewModelScope.launch {
            repository.observeBills(uid).collect { list ->
                _bills.value = list
            }
        }

        viewModelScope.launch {
            repository.observeBudget(uid, currentMonthStr).collect { b ->
                _budget.value = b
            }
        }
    }

    fun login(email: String, pass: String) {
        _authLoading.value = true
        _authError.value = null
        Firebase.auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _authLoading.value = false
            }
            .addOnFailureListener {
                _authLoading.value = false
                _authError.value = it.localizedMessage ?: "Login failed"
            }
    }

    fun signup(name: String, email: String, pass: String) {
        _authLoading.value = true
        _authError.value = null
        Firebase.auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val user = result.user
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name
                }
                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    _authLoading.value = false
                    // Auth state listener will handle the navigation/refresh
                }
            }
            .addOnFailureListener {
                _authLoading.value = false
                _authError.value = it.localizedMessage ?: "Signup failed"
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
        val b = Budget(
            userId = _activeUserId.value,
            month = currentMonthStr,
            monthlyBudget = newBudget
        )
        repository.saveBudget(b)
    }

    fun setProUser(isPro: Boolean) {
        _isProUser.value = isPro
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
                    val sum = all.filter { it.date == dStr }.sumOf { it.amount }
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
                    val sum = all.filter { it.date in sDate..eDate }.sumOf { it.amount }
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
                    val sum = all.filter { it.date.startsWith(mKey) }.sumOf { it.amount }
                    months.add(ChartBarData(label = mLbl, amount = sum, isHighlighted = (mKey == currentMonthKey)))
                }
                months
            }
        }
    }

    fun updatePermissions(hasSms: Boolean, hasLocation: Boolean, location: String = "") {
        _hasSmsPermission.value = hasSms
        _hasLocationPermission.value = hasLocation
        if (location.isNotEmpty()) {
            _deviceLocation.value = location
        }
    }

    fun scanInboxSmsTransactions(context: android.content.Context, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val loc = if (_deviceLocation.value.isNotEmpty()) _deviceLocation.value else com.example.service.SmsParser.getDeviceLocationName(context)
            _deviceLocation.value = loc
            val parsedList = com.example.service.SmsParser.scanExistingInboxSms(context, _activeUserId.value, loc)
            var count = 0
            for (tx in parsedList) {
                repository.saveTransaction(tx)
                count++
            }
            _scannedSmsCount.value = count
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    fun clearAllData() {
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
        filters: FilterState
    ): DashboardUiState {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(lastMonthCal.time)

        var expensesCurrent = 0.0
        var incomeCurrent = 0.0
        var expensesLast = 0.0
        var incomeLast = 0.0
        var todaySpend = 0.0
        val categoryMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()

        for (tx in allTransactions) {
            val isCurrentPeriod = if (filters.homeTimeframe == HomeTimeframe.THIS_MONTH) {
                tx.date.startsWith(currentMonthStr)
            } else {
                tx.date.startsWith(currentYearStr)
            }

            val isLastMonth = tx.date.startsWith(lastMonthStr)

            if (tx.isExpense()) {
                if (isCurrentPeriod) {
                    expensesCurrent += tx.amount
                    categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.amount
                    categoryCountMap[tx.category] = (categoryCountMap[tx.category] ?: 0) + 1

                    if (tx.date == todayStr) {
                        todaySpend += tx.amount
                    }
                } else if (isLastMonth) {
                    expensesLast += tx.amount
                }
            } else if (tx.isIncome()) {
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

        // Filter and sort transactions for Expense Management
        val filtered = allTransactions.filter { tx ->
            val matchesCategory = filters.categoryFilter == "ALL" || tx.category.equals(filters.categoryFilter, ignoreCase = true)
            val matchesPayment = filters.paymentFilter == "ALL" || tx.paymentMethod.contains(filters.paymentFilter, ignoreCase = true)
            val matchesDate = when (filters.dateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> tx.date == todayStr
                DateFilterOption.THIS_MONTH -> tx.date.startsWith(currentMonthStr)
                DateFilterOption.THIS_YEAR -> tx.date.startsWith(currentYearStr)
            }
            val matchesQuery = if (filters.searchQuery.isBlank()) true else {
                tx.title.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.notes.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.category.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.paymentMethod.contains(filters.searchQuery, ignoreCase = true) ||
                        tx.amount.toString().contains(filters.searchQuery)
            }
            matchesCategory && matchesPayment && matchesDate && matchesQuery
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

        return DashboardUiState(
            currentMonthExpenses = expensesCurrent,
            currentMonthIncome = incomeCurrent,
            remainingBalance = remainingBalance,
            monthlyBudget = monthlyBudgetAmount,
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
            userName = Firebase.auth.currentUser?.displayName ?: "User",
            currentDateDisplay = currentDateDisplay,
            activeMonthDisplay = currentMonthDisplay,
            isProUser = _isProUser.value,
            hasSmsPermission = _hasSmsPermission.value,
            hasLocationPermission = _hasLocationPermission.value,
            deviceLocation = _deviceLocation.value,
            scannedSmsCount = _scannedSmsCount.value
        )
    }

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

    companion object {
        fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 0
            return try {
                format.format(amount)
            } catch (e: Exception) {
                "₹${amount.toInt()}"
            }
        }
    }
}
