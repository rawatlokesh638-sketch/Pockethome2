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

    var showAddIncomeSheet by remember { mutableStateOf(false) }
    var showAddBillSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showProPlanSheet by remember { mutableStateOf(false) }
    var currentChartTimeframe by remember { mutableStateOf(ChartTimeframe.DAILY) }

    // Runtime Permission Launcher for SMS and Location
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val smsGranted = permissionsMap[Manifest.permission.READ_SMS] == true
        val locationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        viewModel.updatePermissions(hasSms = smsGranted, hasLocation = locationGranted)

        if (smsGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Scanning Bank SMS inbox... ⏳")
            }
            viewModel.scanInboxSmsTransactions(context) { count ->
                scope.launch {
                    val msg = if (count > 0) {
                        "Synced $count transactions from Bank SMS! 💳"
                    } else {
                        "SMS scanned! No new bank debits found. Future SMS will auto-sync."
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
                    isLoading = uiState.authLoading,
                    errorMessage = uiState.authError
                )
            }
            AuthMode.SIGNUP -> {
                SignupScreen(
                    onSignup = { n, e, p -> viewModel.signup(name = n, email = e, pass = p) },
                    onNavigateToLogin = { viewModel.setAuthMode(AuthMode.LOGIN) },
                    isLoading = uiState.authLoading,
                    errorMessage = uiState.authError
                )
            }
            AuthMode.DASHBOARD -> {
                if (showAddExpenseScreen) {
                    AddExpenseScreen(
                        editingTransaction = editingTransaction,
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
                        }
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
                                onTabSelected = { viewModel.selectTab(it) }
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
                                                        Manifest.permission.RECEIVE_SMS,
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
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
                                            onBack = { viewModel.selectTab(AppTab.HOME) },
                                            onTimeframeSelected = { viewModel.setReportsTimeframe(it) }
                                        )
                                    }

                                    AppTab.BILLS -> {
                                        BillsScreen(
                                            uiState = uiState,
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
                                            }
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

    if (showProPlanSheet) {
        ProPlanSheet(
            isCurrentlyPro = uiState.isProUser,
            onDismiss = { showProPlanSheet = false },
            onUpgradeSuccess = {
                viewModel.setProUser(true)
                showProPlanSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("Welcome to PocketHome PRO! ⭐ All features unlocked.")
                }
            }
        )
    }
}
