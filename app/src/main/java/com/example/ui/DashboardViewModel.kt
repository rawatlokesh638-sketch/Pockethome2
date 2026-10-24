    fun computeState(
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
            val effectiveDate = if (tx.date.isNotBlank()) tx.date else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.timestamp))
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
