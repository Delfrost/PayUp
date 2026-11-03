package com.payup.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.Transaction
import com.payup.app.data.model.TransactionType
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.viewmodel.BudgetUiModel
import com.payup.app.ui.viewmodel.CategoryBudgetInfo
import com.payup.app.ui.viewmodel.DashboardViewModel
import com.payup.app.util.CurrencyUtils
import com.payup.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTransactionClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllTransactions: () -> Unit,
    onAddTransaction: () -> Unit,
    onScanScreenshots: (List<Uri>) -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onScanScreenshots(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PayUp", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Offline-First Finance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            PayUpIcons.PhotoCamera,
                            contentDescription = "Scan Screenshot",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onViewAllTransactions) {
                        Icon(
                            PayUpIcons.Search,
                            contentDescription = "Search Transactions",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(PayUpIcons.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Cards with Total Balance, Income, Salary, Expenses, Savings
                item {
                    SummarySection(
                        income = state.totalIncome,
                        configuredIncome = state.configuredIncome,
                        totalAllocated = state.totalAllocatedBudgets,
                        unallocated = state.unallocatedIncome,
                        salary = state.salaryIncome,
                        expenses = state.totalExpenses,
                        savings = state.savings,
                        totalBalance = state.totalBalance
                    )
                }

                // FEAT-02: Over-allocation warning banner
                if (state.isOverAllocated) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    PayUpIcons.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Budgets Over-allocated",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Total allocated (${CurrencyUtils.formatPaiseCompact(state.totalAllocatedBudgets)}) exceeds configured income (${CurrencyUtils.formatPaiseCompact(state.configuredIncome)}) by ${CurrencyUtils.formatPaiseCompact(state.totalAllocatedBudgets - state.configuredIncome)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Enable Bank SMS Tracking Card (if not granted)
                if (!hasSmsPermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    PayUpIcons.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Track Bank SMS Alerts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Auto-record transactions from SBI, HDFC, ICICI, Axis alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Account Balances
                if (state.accounts.isNotEmpty()) {
                    item {
                        Text(
                            "Account Balances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.accounts, key = { it.id }) { account ->
                                val monthlySpend = state.paymentMethodSpends[account.id] ?: 0L
                                ElevatedCard(
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            account.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            CurrencyUtils.formatPaiseCompact(account.balancePaise),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (monthlySpend > 0) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Spent: ${CurrencyUtils.formatPaiseCompact(monthlySpend)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FEAT-02 & FEAT-03: Named Budgets & Income Allocations
                if (state.budgets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Named Budgets & Allocations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.unallocatedIncome > 0) {
                                    Text(
                                        "${CurrencyUtils.formatPaiseCompact(state.unallocatedIncome)} unallocated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Text(
                                "Tap to expand payments",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(state.budgets, key = { it.budget.id }) { budgetModel ->
                        ExpandableBudgetCard(
                            model = budgetModel,
                            getTransactions = { viewModel.getBudgetTransactions(it) },
                            onTransactionClick = onTransactionClick
                        )
                    }
                }

                // Category Budgets (Expandable with individual payments)
                if (state.categorySpends.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap to expand payments",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(state.categorySpends, key = { it.category.id }) { info ->
                        ExpandableCategoryCard(
                            info = info,
                            getTransactions = { viewModel.getCategoryTransactions(it) },
                            onTransactionClick = onTransactionClick,
                            onViewFullHistory = { onCategoryClick(info.category.id) }
                        )
                    }
                }

                // Recent Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onViewAllTransactions) {
                            Text("View All History")
                        }
                    }
                }

                if (state.recentTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    PayUpIcons.Transactions,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No transactions yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Tap '+' to record your first ₹1, ₹10 or any payment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.recentTransactions, key = { it.id }) { txn ->
                        TransactionRow(txn, onClick = { onTransactionClick(txn.id) })
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun SummarySection(
    income: Long,
    configuredIncome: Long,
    totalAllocated: Long,
    unallocated: Long,
    salary: Long,
    expenses: Long,
    savings: Long,
    totalBalance: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Total Account Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(2.dp))
            Text(
                CurrencyUtils.formatPaiseCompact(totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    icon = PayUpIcons.Income,
                    label = if (configuredIncome > 0) "Income Target" else "Total Income",
                    amount = if (configuredIncome > 0) configuredIncome else income,
                    color = Color(0xFF2E7D32)
                )
                if (totalAllocated > 0) {
                    SummaryItem(
                        icon = PayUpIcons.Payments,
                        label = "Allocated",
                        amount = totalAllocated,
                        color = Color(0xFF00796B)
                    )
                }
                SummaryItem(
                    icon = PayUpIcons.Expense,
                    label = "Expenses",
                    amount = expenses,
                    color = Color(0xFFD32F2F)
                )
                SummaryItem(
                    icon = PayUpIcons.Savings,
                    label = "Savings",
                    amount = savings,
                    color = if (savings >= 0) Color(0xFF1565C0) else Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: Long,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            CurrencyUtils.formatPaiseCompact(amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ExpandableBudgetCard(
    model: BudgetUiModel,
    getTransactions: (String) -> kotlinx.coroutines.flow.Flow<List<Transaction>>,
    onTransactionClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (model.allocatedPaise > 0) {
        (model.spentPaise.toFloat() / model.allocatedPaise.toFloat()).coerceIn(0f, 1f)
    } else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Budget info and expand trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(model.budget.color.toInt() or 0xFF000000.toInt()).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        PayUpIcons.forCategory(model.budget.icon),
                        contentDescription = null,
                        tint = Color(model.budget.color.toInt() or 0xFF000000.toInt()),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.budget.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Spent: ${CurrencyUtils.formatPaiseCompact(model.spentPaise)} / ${CurrencyUtils.formatPaiseCompact(model.allocatedPaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (model.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        CurrencyUtils.formatPaiseCompact(kotlin.math.abs(model.remainingPaise)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (model.isOverspent) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (model.isOverspent) "over budget" else "remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) PayUpIcons.ExpandLess else PayUpIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Budget Progress Bar
            if (model.allocatedPaise > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (model.isOverspent) MaterialTheme.colorScheme.error else Color(model.budget.color.toInt() or 0xFF000000.toInt()),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Categories assigned chips
            if (model.categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    model.categories.take(4).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (model.categories.size > 4) {
                        Text("+${model.categories.size - 4} more", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Expanded content: Displays individual payments in this budget
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val transactionsFlow = remember(model.budget.id) { getTransactions(model.budget.id) }
                val transactions by transactionsFlow.collectAsState(initial = emptyList())
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Budget Payments (${transactions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            "No payments recorded for this budget yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        transactions.forEach { txn ->
                            CategoryPaymentItem(txn = txn, onClick = { onTransactionClick(txn.id) })
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableCategoryCard(
    info: CategoryBudgetInfo,
    getTransactions: (String) -> kotlinx.coroutines.flow.Flow<List<Transaction>>,
    onTransactionClick: (String) -> Unit,
    onViewFullHistory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (info.budgetPaise > 0) {
        (info.spentPaise.toFloat() / info.budgetPaise.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverBudget = info.budgetPaise > 0 && info.spentPaise > info.budgetPaise
    val remaining = info.budgetPaise - info.spentPaise

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category info and expand trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(info.category.color.toInt() or 0xFF000000.toInt()).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        PayUpIcons.forCategory(info.category.icon),
                        contentDescription = null,
                        tint = Color(info.category.color.toInt() or 0xFF000000.toInt()),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Spent: " + CurrencyUtils.formatPaiseCompact(info.spentPaise) +
                                if (info.budgetPaise > 0) " / Budget: ${CurrencyUtils.formatPaiseCompact(info.budgetPaise)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (info.budgetPaise > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            CurrencyUtils.formatPaiseCompact(kotlin.math.abs(remaining)),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isOverBudget) "over budget" else "remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) PayUpIcons.ExpandLess else PayUpIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Budget Progress Bar
            if (info.budgetPaise > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Expanded content: Displays individual payments with amount, merchant, date/time and payment source
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val transactionsFlow = remember(info.category.id) { getTransactions(info.category.id) }
                val transactions by transactionsFlow.collectAsState(initial = emptyList())
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Individual Payments (${transactions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            "No payments recorded for this category yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        transactions.forEach { txn ->
                            CategoryPaymentItem(txn = txn, onClick = { onTransactionClick(txn.id) })
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onViewFullHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(PayUpIcons.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search & full paginated history for ${info.category.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPaymentItem(
    txn: Transaction,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    txn.merchant.ifBlank { "Unknown Merchant" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateUtils.formatDateTime(txn.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val source = txn.sourceApp ?: (if (txn.accountId != null) "Account" else "Manual")
                    Text(
                        " • $source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                (if (txn.type == TransactionType.INCOME) "+" else "-") +
                        CurrencyUtils.formatPaiseCompact(txn.amountPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (txn.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, tint) = when (transaction.type) {
                TransactionType.INCOME -> PayUpIcons.Income to Color(0xFF2E7D32)
                TransactionType.EXPENSE -> PayUpIcons.Expense to Color(0xFFD32F2F)
                TransactionType.TRANSFER -> PayUpIcons.Transfer to Color(0xFF1565C0)
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        DateUtils.relativeDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val source = transaction.sourceApp ?: transaction.detectionMethod.name.replace("_", " ").lowercase()
                    Text(
                        " · $source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                (if (transaction.type == TransactionType.INCOME) "+" else "-") +
                        CurrencyUtils.formatPaiseCompact(transaction.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    TransactionType.INCOME -> Color(0xFF2E7D32)
                    TransactionType.EXPENSE -> Color(0xFFD32F2F)
                    TransactionType.TRANSFER -> Color(0xFF1565C0)
                }
            )
        }
    }
}
