package com.example.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Transaction
import com.example.ui.components.AddBillSheet
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.ChartTimeframe
import com.example.ui.components.EditBudgetDialog
import com.example.ui.components.PocketHomeBottomBar
import com.example.ui.components.ProPlanSheet
import com.example.ui.components.SmartAddBottomSheet
import com.example.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddExpenseScreen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    var showSmartAddSheet by remember { mutableStateOf(false) }
    var showAddIncomeSheet by remember { mutableStateOf(false) }
    var showAddBillSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showProPlanSheet by remember { mutableStateOf(false) }
    var currentChartTimeframe by remember { mutableStateOf(ChartTimeframe.DAILY) }

    // Runtime Permission Launcher for SMS
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val smsGranted = permissionsMap[Manifest.permission.READ_SMS] == true ||
                permissionsMap[Manifest.permission.RECEIVE_SMS] == true

        viewModel.updatePermissions(hasSms = smsGranted)

        if (smsGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Scanning SMS inbox... ⏳")
            }
            viewModel.scanInboxSmsTransactions(context) { count ->
                scope.launch {
                    val msg = if (count > 0) {
                        "Synced $count transactions from SMS messages! 💳"
                    } else {
                        "SMS scanned! No transaction messages found. Future SMS will auto-sync."
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("SMS permission not granted. You can still add expenses manually.")
            }
        }
    }

    // Handle system back navigation
    BackHandler(enabled = showAddExpenseScreen || uiState.authMode != AuthMode.DASHBOARD || uiState.activeTab != AppTab.HOME) {
        if (showAddExpenseScreen) {
            showAddExpenseScreen = false
            editingTransaction = null
        } else if (uiState.authMode == AuthMode.SIGNUP) {
            viewModel.setAuthMode(AuthMode.LOGIN)
        } else if (uiState.activeTab != AppTab.HOME) {
            viewModel.selectTab(AppTab.HOME)
        }
    }

    Crossfade(targetState = uiState.authMode, label = "auth_fade") { mode ->
        when (mode) {
            AuthMode.LOGIN -> {
                LoginScreen(
                    onLogin = { e, p -> viewModel.login(email = e, pass = p) },
                    onNavigateToSignup = { viewModel.setAuthMode(AuthMode.SIGNUP) },
                    onGoogleSignIn = { token -> viewModel.signInWithGoogle(token) },
                    onQuickGoogleSignIn = { viewModel.quickGoogleSignIn() },
                    isLoading = uiState.authLoading,
                    errorMessage = uiState.authError
                )
            }
            AuthMode.SIGNUP -> {
                SignupScreen(
                    onSignup = { n, e, p -> viewModel.signup(name = n, email = e, pass = p) },
                    onNavigateToLogin = { viewModel.setAuthMode(AuthMode.LOGIN) },
                    onGoogleSignIn = { token -> viewModel.signInWithGoogle(token) },
                    onQuickGoogleSignIn = { viewModel.quickGoogleSignIn() },
                    isLoading = uiState.authLoading,
                    errorMessage = uiState.authError
                )
            }
            AuthMode.DASHBOARD -> {
                if (showAddExpenseScreen) {
                    AddExpenseScreen(
                        editingTransaction = editingTransaction,
                        isPro = uiState.isProUser,
                        onBack = {
                            showAddExpenseScreen = false
                            editingTransaction = null
                        },
                        onSave = { tx ->
                            viewModel.addOrUpdateTransaction(tx)
                            scope.launch {
                                val msg = if (editingTransaction != null) "Expense updated successfully! ✍️" else "Expense added successfully! 💸"
                                snackbarHostState.showSnackbar(msg)
                            }
                            showAddExpenseScreen = false
                            editingTransaction = null
                        },
                        onUpgradeClicked = { showProPlanSheet = true }
                    )
                } else {
                    Scaffold(
                        modifier = modifier
                            .fillMaxSize()
                            .testTag("dashboard_root"),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            PocketHomeBottomBar(
                                currentTab = uiState.activeTab,
                                onTabSelected = { viewModel.selectTab(it) },
                                onAddClicked = { showSmartAddSheet = true }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(targetState = uiState.activeTab, label = "tab_fade") { tab ->
                                when (tab) {
                                    AppTab.HOME -> {
                                        HomeScreen(
                                            uiState = uiState,
                                            proStatus = uiState.proStatus,
                                            chartData = viewModel.getChartData(currentChartTimeframe),
                                            currentChartTimeframe = currentChartTimeframe,
                                            onChartTimeframeChanged = { currentChartTimeframe = it },
                                            onTimeframeChanged = { viewModel.setHomeTimeframe(it) },
                                            onAddExpenseClicked = {
                                                editingTransaction = null
                                                showAddExpenseScreen = true
                                            },
                                            onAddIncomeClicked = { showAddIncomeSheet = true },
                                            onAddBillClicked = { showAddBillSheet = true },
                                            onSetBudgetClicked = { showEditBudgetDialog = true },
                                            onViewAllTransactions = { viewModel.selectTab(AppTab.TRANSACTIONS) },
                                            onClearAllData = {
                                                viewModel.clearAllData()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Sabhi data 0 kar diya gaya hai! 🧹")
                                                }
                                            },
                                            onOpenProSheet = { showProPlanSheet = true },
                                            onRequestSmsPermissions = {
                                                permissionsLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.READ_SMS,
                                                        Manifest.permission.RECEIVE_SMS
                                                    )
                                                )
                                            },
                                            onTransactionClicked = { tx ->
                                                editingTransaction = tx
                                                showAddExpenseScreen = true
                                            },
                                            onToggleBillPaid = { billId, status ->
                                                viewModel.toggleBillPaid(billId, status)
                                                scope.launch {
                                                    val msg = if (!status) "Bill marked as paid! ✅" else "Bill marked as pending."
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            },
                                            onDeleteBill = { billId ->
                                                viewModel.deleteBill(billId)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Bill deleted.")
                                                }
                                            },
                                            onLogout = { viewModel.logout() }
                                        )
                                    }

                                    AppTab.TRANSACTIONS -> {
                                        TransactionsScreen(
                                            uiState = uiState,
                                            onSearchChanged = { viewModel.setSearchQuery(it) },
                                            onCategoryFilterChanged = { viewModel.setCategoryFilter(it) },
                                            onPaymentFilterChanged = { viewModel.setPaymentFilter(it) },
                                            onDateFilterChanged = { viewModel.setDateFilter(it) },
                                            onSortChanged = { viewModel.setSortOption(it) },
                                            onAddExpenseClicked = {
                                                editingTransaction = null
                                                showAddExpenseScreen = true
                                            },
                                            onEditTransaction = { tx ->
                                                editingTransaction = tx
                                                showAddExpenseScreen = true
                                            },
                                            onDuplicateTransaction = { tx ->
                                                viewModel.duplicateTransaction(tx)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Transaction duplicated! 📋")
                                                }
                                            },
                                            onDeleteTransaction = { txId ->
                                                viewModel.deleteTransaction(txId)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Transaction deleted. 🗑️")
                                                }
                                            }
                                        )
                                    }

                                    AppTab.REPORTS -> {
                                        ReportsScreen(
                                            uiState = uiState,
                                            isPro = uiState.isProUser,
                                            onBack = { viewModel.selectTab(AppTab.HOME) },
                                            onTimeframeSelected = { viewModel.setReportsTimeframe(it) },
                                            onUpgradeClicked = { showProPlanSheet = true }
                                        )
                                    }

                                    AppTab.BILLS -> {
                                        BillsScreen(
                                            uiState = uiState,
                                            isPro = uiState.isProUser,
                                            onBack = { viewModel.selectTab(AppTab.HOME) },
                                            onAddBillClicked = { showAddBillSheet = true },
                                            onTogglePaid = { billId, status ->
                                                viewModel.toggleBillPaid(billId, status)
                                                scope.launch {
                                                    val msg = if (!status) "Bill marked as paid! ✅" else "Bill marked as pending."
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            },
                                            onDeleteBill = { billId ->
                                                viewModel.deleteBill(billId)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Bill deleted.")
                                                }
                                            },
                                            onUpgradeClicked = { showProPlanSheet = true }
                                        )
                                    }

                                    AppTab.BUDGET -> {
                                        BudgetScreen(
                                            uiState = uiState,
                                            onBack = { viewModel.selectTab(AppTab.HOME) },
                                            onEditBudgetClicked = { showEditBudgetDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets for Income, Bills, Budget, and Pro Plan
    if (showAddIncomeSheet) {
        AddTransactionSheet(
            initialType = Transaction.TYPE_INCOME,
            onDismiss = { showAddIncomeSheet = false },
            onSaveTransaction = { title, amount, type, category, method, notes, date ->
                viewModel.addOrUpdateTransaction(
                    Transaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        paymentMethod = method,
                        notes = notes,
                        date = date,
                        timestamp = System.currentTimeMillis()
                    )
                )
                scope.launch {
                    snackbarHostState.showSnackbar("Income added successfully! 💰")
                }
                showAddIncomeSheet = false
            }
        )
    }

    if (showAddBillSheet) {
        AddBillSheet(
            onDismiss = { showAddBillSheet = false },
            onSaveBill = { title, amount, category, dueDate, recurring, notes ->
                viewModel.addOrUpdateBill(
                    com.example.model.Bill(
                        title = title,
                        amount = amount,
                        category = category,
                        dueDate = dueDate,
                        recurring = recurring,
                        notes = notes
                    )
                )
                scope.launch {
                    snackbarHostState.showSnackbar("Upcoming bill saved! 🧾")
                }
                showAddBillSheet = false
            }
        )
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = uiState.monthlyBudget,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = { newBudget ->
                viewModel.updateMonthlyBudget(newBudget)
                scope.launch {
                    snackbarHostState.showSnackbar("Monthly budget updated to ${DashboardViewModel.formatCurrency(newBudget)}")
                }
                showEditBudgetDialog = false
            }
        )
    }

    if (showSmartAddSheet) {
        SmartAddBottomSheet(
            userId = uiState.activeUserId,
            onDismiss = { showSmartAddSheet = false },
            onSaveTransaction = { tx ->
                viewModel.addOrUpdateTransaction(tx)
                scope.launch {
                    val label = if (tx.isIncome()) "Income added: ${tx.title} (+${DashboardViewModel.formatCurrency(tx.amount)}) 💰"
                    else "Expense added: ${tx.title} (${DashboardViewModel.formatCurrency(tx.amount)}) 💸"
                    snackbarHostState.showSnackbar(label)
                }
                showSmartAddSheet = false
            },
            onSaveBill = { bill ->
                viewModel.addOrUpdateBill(bill)
                scope.launch {
                    snackbarHostState.showSnackbar("Bill added: ${bill.title} (${DashboardViewModel.formatCurrency(bill.amount)}) Due on ${bill.dueDate} 🧾")
                }
                showSmartAddSheet = false
            },
            onOpenManualExpense = {
                editingTransaction = null
                showAddExpenseScreen = true
            },
            onOpenManualIncome = {
                showAddIncomeSheet = true
            },
            onOpenManualBill = {
                showAddBillSheet = true
            }
        )
    }

    if (showProPlanSheet) {
        ProPlanSheet(
            proStatus = uiState.proStatus,
            isLoading = uiState.authLoading,
            onDismiss = { showProPlanSheet = false },
            onSubmitRequest = { txn, utr ->
                viewModel.submitProMembershipRequest(
                    txnId = txn,
                    utr = utr,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Payment request submitted! Admin jald hi approve kar denge. ⏳")
                        }
                        showProPlanSheet = false
                    },
                    onFailure = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Error: $error")
                        }
                    }
                )
            }
        )
    }
}
