package com.payup.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.Account
import com.payup.app.data.model.Category
import com.payup.app.data.model.TransactionType
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.viewmodel.AddTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val budgets by viewModel.budgets.collectAsState(initial = emptyList())
    var showAllCategoriesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
        }
    }

    // Auto-select first account if none selected
    LaunchedEffect(accounts) {
        if (state.selectedAccountId == null && accounts.isNotEmpty()) {
            val upiOrFirst = accounts.find { it.name.contains("UPI", ignoreCase = true) } ?: accounts.first()
            viewModel.updateAccount(upiOrFirst.id)
        }
    }

    val filteredCategories = categories.filter {
        if (state.type == TransactionType.INCOME) it.isIncome else !it.isIncome
    }

    // Auto-select first category if none selected
    LaunchedEffect(filteredCategories, state.type) {
        if (state.selectedCategoryId == null && filteredCategories.isNotEmpty()) {
            viewModel.updateCategory(filteredCategories.first().id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Modern Segmented Type Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val isSelected = state.type == type
                    val activeBg = when (type) {
                        TransactionType.EXPENSE -> Color(0xFFD32F2F)
                        TransactionType.INCOME -> Color(0xFF2E7D32)
                        TransactionType.TRANSFER -> Color(0xFF1565C0)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable { viewModel.updateType(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Large Modern Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (state.type) {
                        TransactionType.EXPENSE -> Color(0xFFD32F2F).copy(alpha = 0.08f)
                        TransactionType.INCOME -> Color(0xFF2E7D32).copy(alpha = 0.08f)
                        TransactionType.TRANSFER -> Color(0xFF1565C0).copy(alpha = 0.08f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "₹",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (state.type) {
                                TransactionType.EXPENSE -> Color(0xFFD32F2F)
                                TransactionType.INCOME -> Color(0xFF2E7D32)
                                TransactionType.TRANSFER -> Color(0xFF1565C0)
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.amountText,
                            onValueChange = { viewModel.updateAmount(it) },
                            placeholder = { Text("0.00", fontSize = 34.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                color = when (state.type) {
                                    TransactionType.EXPENSE -> Color(0xFFD32F2F)
                                    TransactionType.INCOME -> Color(0xFF2E7D32)
                                    TransactionType.TRANSFER -> Color(0xFF1565C0)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 120.dp, max = 240.dp)
                        )
                    }
                    Text(
                        "Track any amount — ₹1, ₹10 or lakhs without minimum thresholds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // 3. Payment Method / Account Pills (UPI, Bank, Cash, Card)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paid via (Payment Account)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { account ->
                        AccountPill(
                            account = account,
                            isSelected = state.selectedAccountId == account.id,
                            onSelect = { viewModel.updateAccount(account.id) }
                        )
                    }
                }
            }

            // 4. Category Grid / Quick Select
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showAllCategoriesSheet = true }) {
                        Text("View all (${filteredCategories.size})")
                    }
                }

                // Show top 6 categories as modern interactive chips
                val topCategories = filteredCategories.take(6)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topCategories.chunked(3).forEach { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { cat ->
                                CategoryChip(
                                    category = cat,
                                    isSelected = state.selectedCategoryId == cat.id,
                                    modifier = Modifier.weight(1f),
                                    onSelect = { viewModel.updateCategory(cat.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 4b. Budget Override (Optional)
            if (state.type == TransactionType.EXPENSE && budgets.isNotEmpty()) {
                Column {
                    Text(
                        "Budget Allocation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedBudgetId == null,
                            onClick = { viewModel.updateBudget(null) },
                            label = { Text("Auto (from Category)") }
                        )
                        budgets.forEach { b ->
                            val isSelected = state.selectedBudgetId == b.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateBudget(b.id) },
                                label = { Text(b.name) }
                            )
                        }
                    }
                }
            }

            // 5. Merchant / Description
            OutlinedTextField(
                value = state.merchant,
                onValueChange = { viewModel.updateMerchant(it) },
                label = { Text("Merchant or Payee (e.g. Swiggy, Chai Tapri, Metro)") },
                leadingIcon = { Icon(PayUpIcons.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 6. Notes (Optional)
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Add Note (optional)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(4.dp))

            // 7. Large Modern Save Button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = !state.isSaving && state.amountText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(PayUpIcons.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Modern Bottom Sheet for Selecting from All Categories
    if (showAllCategoriesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllCategoriesSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.updateCategory(cat.id)
                                    showAllCategoriesSheet = false
                                },
                            color = if (state.selectedCategoryId == cat.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.color.toInt() or 0xFF000000.toInt()).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        PayUpIcons.forCategory(cat.icon),
                                        contentDescription = null,
                                        tint = Color(cat.color.toInt() or 0xFF000000.toInt()),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (state.selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.selectedCategoryId == cat.id) {
                                    Icon(PayUpIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AccountPill(
    account: Account,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    account.name.contains("UPI", ignoreCase = true) -> PayUpIcons.Payments
                    account.name.contains("Bank", ignoreCase = true) -> PayUpIcons.Account
                    account.name.contains("Card", ignoreCase = true) -> PayUpIcons.Transactions
                    else -> PayUpIcons.Wallet
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                account.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val catColor = Color(category.color.toInt() or 0xFF000000.toInt())
    val bgColor = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (isSelected) catColor else Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect),
        color = bgColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                PayUpIcons.forCategory(category.icon),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
