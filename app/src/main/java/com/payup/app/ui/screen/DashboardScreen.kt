package com.payup.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.Account
import com.payup.app.data.model.Transaction
import com.payup.app.data.model.TransactionType
import com.payup.app.ui.component.*
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PayUp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Offline-First Finance",
                            style = MaterialTheme.typography.labelSmall,
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                },
                actions = {
                    // Screenshot OCR Button
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            PayUpIcons.PhotoCamera,
                            contentDescription = "Scan Screenshot",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // Search Transactions Button
                    IconButton(onClick = onViewAllTransactions) {
                        Icon(
                            PayUpIcons.Search,
                            contentDescription = "Search Transactions",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                Icon(PayUpIcons.Add, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Apple-Style Hero Total Balance Card with Cash & UPI split
                item {
                    HeroBalanceCard(
                        totalBalance = state.totalBalance,
                        accounts = state.accounts,
                        income = state.totalIncome,
                        expenses = state.totalExpenses
                    )
                }

                // 2. Budget Over-Allocation Warning (if applicable)
                if (state.isOverAllocated) {
                    item {
                        PayUpCard(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    PayUpIcons.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Budgets Over-allocated",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Allocated (${CurrencyUtils.formatPaiseCompact(state.totalAllocatedBudgets)}) exceeds income (${CurrencyUtils.formatPaiseCompact(state.configuredIncome)}) by ${CurrencyUtils.formatPaiseCompact(state.totalAllocatedBudgets - state.configuredIncome)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PayUpThemeExtended.secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Bank SMS Tracking Banner (if not granted)
                if (!hasSmsPermission) {
                    item {
                        PayUpCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            PayUpIcons.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Track Bank SMS Alerts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Auto-record incoming SBI, HDFC, ICICI, Axis alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PayUpThemeExtended.secondaryTextColor
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                PayUpPrimaryButton(
                                    text = "Enable",
                                    onClick = { smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Accounts Carousel (Reconciled Cash, UPI, Bank, Cards)
                if (state.accounts.isNotEmpty()) {
                    item {
                        PayUpSectionHeader(title = "Account Balances")
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.accounts, key = { it.id }) { account ->
                                AccountCard(
                                    account = account,
                                    monthlySpend = state.paymentMethodSpends[account.id] ?: 0L
                                )
                            }
                        }
                    }
                }

                // 5. Named Budgets & Envelope Allocations
                if (state.budgets.isNotEmpty()) {
                    item {
                        PayUpSectionHeader(
                            title = "Budgets & Allocations",
                            actionText = if (state.unallocatedIncome > 0) "${CurrencyUtils.formatPaiseCompact(state.unallocatedIncome)} unallocated" else null
                        )
                    }
                    items(state.budgets, key = { it.budget.id }) { budgetModel ->
                        ExpandableBudgetCard(
                            model = budgetModel,
                            getTransactions = { viewModel.getBudgetTransactions(it) },
                            onTransactionClick = onTransactionClick
                        )
                    }
                }

                // 6. Category Breakdown
                if (state.categorySpends.isNotEmpty()) {
                    item {
                        PayUpSectionHeader(title = "Category Breakdown")
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

                // 7. Recent Transactions (Every payment visible with clean tabular numerals)
                item {
                    PayUpSectionHeader(
                        title = "Recent Transactions",
                        actionText = "View All",
                        onActionClick = onViewAllTransactions
                    )
                }

                if (state.recentTransactions.isEmpty()) {
                    item {
                        PayUpEmptyState(
                            icon = PayUpIcons.Transactions,
                            title = "No Transactions Recorded",
                            description = "Tap '+' below to record your first payment or scan a receipt.",
                            actionText = "Add First Transaction",
                            onActionClick = onAddTransaction
                        )
                    }
                } else {
                    items(state.recentTransactions, key = { it.id }) { txn ->
                        AppleTransactionRow(
                            transaction = txn,
                            onClick = { onTransactionClick(txn.id) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}

/**
 * Apple-inspired monochromatic Hero Balance Card.
 * Prominently presents total available balance and concise sub-breakdown.
 */
@Composable
private fun HeroBalanceCard(
    totalBalance: Long,
    accounts: List<Account>,
    income: Long,
    expenses: Long
) {
    val upiAccount = accounts.find { it.id == "acc_upi" || it.name.contains("UPI", ignoreCase = true) }
    val cashAccount = accounts.find { it.id == "acc_cash" || it.name.equals("Cash", ignoreCase = true) }

    PayUpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "TOTAL AVAILABLE BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = PayUpThemeExtended.secondaryTextColor,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                CurrencyUtils.formatPaise(totalBalance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            // Sub-breakdown: Cash & UPI balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (upiAccount != null) {
                    BalancePill(
                        label = "UPI / Wallet",
                        amount = upiAccount.balancePaise,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (cashAccount != null) {
                    BalancePill(
                        label = "Cash in Hand",
                        amount = cashAccount.balancePaise,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = PayUpThemeExtended.borderColor, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Monthly Flow Metrics: Income vs Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        PayUpIcons.Income,
                        contentDescription = "Income",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = PayUpThemeExtended.secondaryTextColor)
                        Text(CurrencyUtils.formatPaiseCompact(income), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        PayUpIcons.Expense,
                        contentDescription = "Expenses",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Expenses", style = MaterialTheme.typography.labelSmall, color = PayUpThemeExtended.secondaryTextColor)
                        Text(CurrencyUtils.formatPaiseCompact(expenses), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        PayUpIcons.Savings,
                        contentDescription = "Savings",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Net Savings", style = MaterialTheme.typography.labelSmall, color = PayUpThemeExtended.secondaryTextColor)
                        val savings = income - expenses
                        Text(CurrencyUtils.formatPaiseCompact(savings), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancePill(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, PayUpThemeExtended.borderColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = PayUpThemeExtended.secondaryTextColor
            )
            Text(
                CurrencyUtils.formatPaiseCompact(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    monthlySpend: Long
) {
    PayUpCard(
        modifier = Modifier.width(136.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                account.name,
                style = MaterialTheme.typography.labelSmall,
                color = PayUpThemeExtended.secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                CurrencyUtils.formatPaiseCompact(account.balancePaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (monthlySpend > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Spent: ${CurrencyUtils.formatPaiseCompact(monthlySpend)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PayUpThemeExtended.secondaryTextColor
                )
            }
        }
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

    PayUpCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            PayUpIcons.forCategory(model.budget.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        model.budget.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${CurrencyUtils.formatPaiseCompact(model.spentPaise)} / ${CurrencyUtils.formatPaiseCompact(model.allocatedPaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        CurrencyUtils.formatPaiseCompact(kotlin.math.abs(model.remainingPaise)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (model.isOverspent) "over budget" else "remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) PayUpIcons.ExpandLess else PayUpIcons.ExpandMore,
                    contentDescription = null,
                    tint = PayUpThemeExtended.secondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (model.allocatedPaise > 0) {
                Spacer(Modifier.height(10.dp))
                PayUpLinearProgress(
                    progress = progress,
                    isOverBudget = model.isOverspent
                )
            }

            // Categories assigned chips
            if (model.categories.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    model.categories.take(4).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, PayUpThemeExtended.borderColor)
                        ) {
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = PayUpThemeExtended.secondaryTextColor
                            )
                        }
                    }
                    if (model.categories.size > 4) {
                        Text(
                            "+${model.categories.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = PayUpThemeExtended.secondaryTextColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            // Expanded Payments
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val transactionsFlow = remember(model.budget.id) { getTransactions(model.budget.id) }
                val transactions by transactionsFlow.collectAsState(initial = emptyList())
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = PayUpThemeExtended.borderColor, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
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
                            color = PayUpThemeExtended.secondaryTextColor,
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
    val remaining = info.budgetPaise - info.spentPaise

    PayUpCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            PayUpIcons.forCategory(info.category.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Spent: " + CurrencyUtils.formatPaiseCompact(info.spentPaise) +
                                if (info.budgetPaise > 0) " / ${CurrencyUtils.formatPaiseCompact(info.budgetPaise)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
                if (info.budgetPaise > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            CurrencyUtils.formatPaiseCompact(kotlin.math.abs(remaining)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (info.spentPaise > info.budgetPaise) "over budget" else "remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) PayUpIcons.ExpandLess else PayUpIcons.ExpandMore,
                    contentDescription = null,
                    tint = PayUpThemeExtended.secondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (info.budgetPaise > 0) {
                Spacer(Modifier.height(10.dp))
                PayUpLinearProgress(
                    progress = progress,
                    isOverBudget = info.spentPaise > info.budgetPaise
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val transactionsFlow = remember(info.category.id) { getTransactions(info.category.id) }
                val transactions by transactionsFlow.collectAsState(initial = emptyList())
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = PayUpThemeExtended.borderColor, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Payments in ${info.category.name} (${transactions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            "No payments recorded for this category yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PayUpThemeExtended.secondaryTextColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        transactions.forEach { txn ->
                            CategoryPaymentItem(txn = txn, onClick = { onTransactionClick(txn.id) })
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onViewFullHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search & full history for ${info.category.name}", style = MaterialTheme.typography.labelMedium)
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PayUpThemeExtended.subtleBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    txn.merchant.ifBlank { "Unknown Merchant" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    DateUtils.formatDateTime(txn.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PayUpThemeExtended.secondaryTextColor
                )
            }
            Text(
                (if (txn.type == TransactionType.INCOME) "+" else "-") + CurrencyUtils.formatPaiseCompact(txn.amountPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Apple-style minimalist transaction row.
 * Monochromatic high contrast, crisp typography. Every transaction even ₹1 is visible.
 */
@Composable
fun AppleTransactionRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    PayUpCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (transaction.type) {
                        TransactionType.INCOME -> PayUpIcons.Income
                        TransactionType.EXPENSE -> PayUpIcons.Expense
                        TransactionType.TRANSFER -> PayUpIcons.Transfer
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant.ifBlank { "Direct Payment" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateUtils.relativeDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                    val source = transaction.sourceApp ?: transaction.detectionMethod.name.replace("_", " ").lowercase()
                    Text(
                        " · $source",
                        style = MaterialTheme.typography.bodySmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
            }

            val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
            Text(
                sign + CurrencyUtils.formatPaise(transaction.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
