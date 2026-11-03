package com.payup.app.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.TransactionType
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.viewmodel.TransactionDetailViewModel
import com.payup.app.util.CurrencyUtils
import com.payup.app.util.DateUtils

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
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action is permanent.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction()
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(PayUpIcons.Edit, contentDescription = "Edit Transaction")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(PayUpIcons.Delete, contentDescription = "Delete Transaction", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Transaction not found", color = MaterialTheme.colorScheme.error)
                }
            }
            state.transaction != null -> {
                val txn = state.transaction!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Amount Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (txn.type) {
                                TransactionType.INCOME -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                TransactionType.EXPENSE -> Color(0xFFD32F2F).copy(alpha = 0.12f)
                                TransactionType.TRANSFER -> Color(0xFF1565C0).copy(alpha = 0.12f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                txn.type.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when (txn.type) {
                                    TransactionType.INCOME -> Color(0xFF2E7D32)
                                    TransactionType.EXPENSE -> Color(0xFFD32F2F)
                                    TransactionType.TRANSFER -> Color(0xFF1565C0)
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                (if (txn.type == TransactionType.INCOME) "+" else "-") +
                                        CurrencyUtils.formatPaise(txn.amountPaise),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (txn.type) {
                                    TransactionType.INCOME -> Color(0xFF2E7D32)
                                    TransactionType.EXPENSE -> Color(0xFFD32F2F)
                                    TransactionType.TRANSFER -> Color(0xFF1565C0)
                                }
                            )
                            if (txn.merchant.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    txn.merchant,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Detailed Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    DetailItem("Amount (Paise)", "${txn.amountPaise} paise (deterministic integer)")
                    DetailItem("Merchant / Payee", txn.merchant.ifBlank { "Not specified" })
                    DetailItem("Date & Time", DateUtils.formatDateTime(txn.timestamp))
                    DetailItem("Status", txn.status.name)
                    DetailItem("Category", state.category?.name ?: "Uncategorized")
                    DetailItem("Budget", state.budget?.name ?: "None assigned")
                    DetailItem("Account", state.account?.name ?: "None assigned")
                    DetailItem("Source App", txn.sourceApp ?: "None (Direct / Manual)")
                    DetailItem("Reference ID", txn.referenceId ?: "None")
                    DetailItem(
                        "Detection Method",
                        txn.detectionMethod.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )

                    if (!txn.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                txn.notes,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Text("System Identifiers & Sync Readiness", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    DetailItem("Stable UUID", txn.id, isMuted = true)
                    DetailItem("Dedup SHA-256", txn.dedupHash ?: "N/A", isMuted = true)
                    DetailItem("Created At", DateUtils.formatDateTime(txn.createdAt), isMuted = true)
                    DetailItem("Updated At", DateUtils.formatDateTime(txn.updatedAt), isMuted = true)

                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(PayUpIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Transaction")
                    }
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
private fun DetailItem(label: String, value: String, isMuted: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (isMuted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = if (isMuted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isMuted) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionSheet(
    transaction: com.payup.app.data.model.Transaction,
    categories: kotlinx.coroutines.flow.Flow<List<com.payup.app.data.model.Category>>,
    budgets: kotlinx.coroutines.flow.Flow<List<com.payup.app.data.model.Budget>>,
    accounts: kotlinx.coroutines.flow.Flow<List<com.payup.app.data.model.Account>>,
    onDismiss: () -> Unit,
    onSave: (Long?, String?, String?, String?, String?, String?) -> Unit
) {
    val categoriesList by categories.collectAsState(initial = emptyList())
    val budgetsList by budgets.collectAsState(initial = emptyList())
    val accountsList by accounts.collectAsState(initial = emptyList())
    var amountText by remember { mutableStateOf((transaction.amountPaise / 100.0).toString()) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedBudgetId by remember { mutableStateOf(transaction.budgetId) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Edit Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            // Category selection pills
            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriesList.forEach { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(PayUpIcons.forCategory(cat.icon), contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Budget override selection pills
            Text("Budget Override", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedBudgetId == null,
                    onClick = { selectedBudgetId = null },
                    label = { Text("Auto (from Category)") }
                )
                budgetsList.forEach { bud ->
                    val isSelected = selectedBudgetId == bud.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBudgetId = bud.id },
                        label = { Text(bud.name) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Account selection pills
            Text("Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accountsList.forEach { acc ->
                    val isSelected = selectedAccountId == acc.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text(acc.name) },
                        leadingIcon = {
                            Icon(PayUpIcons.Account, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val newAmount = CurrencyUtils.parseToPaise(amountText).takeIf { it > 0 }
                    onSave(newAmount, merchant, selectedCategoryId, selectedBudgetId, selectedAccountId, notes.ifBlank { null })
                }) {
                    Text("Save Changes")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
