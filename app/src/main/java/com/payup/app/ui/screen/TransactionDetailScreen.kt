package com.payup.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.Account
import com.payup.app.data.model.Budget
import com.payup.app.data.model.Category
import com.payup.app.data.model.Transaction
import com.payup.app.data.model.TransactionType
import com.payup.app.ui.component.*
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
import com.payup.app.ui.viewmodel.TransactionDetailViewModel
import com.payup.app.util.CurrencyUtils
import com.payup.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this transaction from your ledger?") },
            containerColor = PayUpThemeExtended.cardColor,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction()
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PayUpThemeExtended.secondaryTextColor)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(PayUpIcons.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(PayUpIcons.Delete, contentDescription = "Delete", tint = PayUpThemeExtended.secondaryTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
            state.error != null -> {
                PayUpEmptyState(
                    icon = PayUpIcons.Info,
                    title = "Transaction Not Found",
                    description = state.error ?: "This transaction could not be located in your local database.",
                    actionText = "Go Back",
                    onActionClick = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
            state.transaction != null -> {
                val txn = state.transaction!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Amount Card
                    PayUpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PayUpBadge(
                                    text = txn.type.name,
                                    icon = if (txn.type == TransactionType.INCOME) PayUpIcons.Income else PayUpIcons.Expense
                                )
                                PayUpBadge(
                                    text = txn.status.name,
                                    icon = PayUpIcons.Check
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            val sign = if (txn.type == TransactionType.INCOME) "+" else "-"
                            Text(
                                sign + CurrencyUtils.formatPaise(txn.amountPaise),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (txn.merchant.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    txn.merchant,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PayUpThemeExtended.secondaryTextColor
                                )
                            }
                        }
                    }

                    // Metadata Group Card
                    PayUpSectionHeader(title = "Payment Information")
                    PayUpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            DetailRow("Merchant / Payee", txn.merchant.ifBlank { "Direct Payment" })
                            DetailDivider()
                            DetailRow("Date & Time", DateUtils.formatDateTime(txn.timestamp))
                            DetailDivider()
                            DetailRow("Category", state.category?.name ?: "Uncategorized")
                            DetailDivider()
                            DetailRow("Budget Envelope", state.budget?.name ?: "None assigned")
                            DetailDivider()
                            DetailRow("Payment Account", state.account?.name ?: "UPI / Wallet")
                            DetailDivider()
                            DetailRow("Detection Method", txn.detectionMethod.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                            if (txn.sourceApp != null) {
                                DetailDivider()
                                DetailRow("Source Application", txn.sourceApp!!)
                            }
                            if (txn.referenceId != null) {
                                DetailDivider()
                                DetailRow("Reference ID / UTR", txn.referenceId!!)
                            }
                        }
                    }

                    // Notes Section (if any)
                    if (!txn.notes.isNullOrBlank()) {
                        PayUpSectionHeader(title = "Notes")
                        PayUpCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                txn.notes,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // System Ledger & Sync Metadata
                    PayUpSectionHeader(title = "Ledger Audit Info")
                    PayUpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            DetailRow("Paise Value", "${txn.amountPaise} paise", isSecondary = true)
                            DetailDivider()
                            DetailRow("Internal UUID", txn.id, isSecondary = true)
                            if (txn.dedupHash != null) {
                                DetailDivider()
                                DetailRow("Dedup Hash", txn.dedupHash!!.take(16) + "...", isSecondary = true)
                            }
                            DetailDivider()
                            DetailRow("Created Timestamp", DateUtils.formatDateTime(txn.createdAt), isSecondary = true)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    PayUpPrimaryButton(
                        text = "Edit Transaction",
                        icon = PayUpIcons.Edit,
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showEditSheet && state.transaction != null) {
        EditTransactionSheet(
            transaction = state.transaction!!,
            categories = viewModel.categories,
            budgets = viewModel.budgets,
            accounts = viewModel.accounts,
            onDismiss = { showEditSheet = false },
            onSave = { amount, merchant, categoryId, budgetId, accountId, notes ->
                viewModel.updateTransaction(
                    amountPaise = amount,
                    merchant = merchant,
                    categoryId = categoryId,
                    budgetId = budgetId,
                    accountId = accountId,
                    notes = notes
                )
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isSecondary: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = PayUpThemeExtended.secondaryTextColor
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSecondary) FontWeight.Normal else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(
        color = PayUpThemeExtended.subtleBorderColor,
        thickness = 1.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionSheet(
    transaction: Transaction,
    categories: Flow<List<Category>>,
    budgets: Flow<List<Budget>>,
    accounts: Flow<List<Account>>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String?, String?, String?, String?) -> Unit
) {
    val categoryList by categories.collectAsState(initial = emptyList())
    val budgetList by budgets.collectAsState(initial = emptyList())
    val accountList by accounts.collectAsState(initial = emptyList())

    var merchant by remember { mutableStateOf(transaction.merchant) }
    var amountText by remember { mutableStateOf((transaction.amountPaise / 100.0).toString().removeSuffix(".0")) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedBudgetId by remember { mutableStateOf(transaction.budgetId) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PayUpThemeExtended.cardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                )
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Payee") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                )
            )

            PayUpPrimaryButton(
                text = "Save Changes",
                onClick = {
                    val paise = CurrencyUtils.parseToPaise(amountText)
                    onSave(
                        if (paise > 0) paise else transaction.amountPaise,
                        merchant,
                        selectedCategoryId,
                        selectedBudgetId,
                        selectedAccountId,
                        notes.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
