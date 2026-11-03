package com.payup.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.payup.app.ui.component.*
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
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

    LaunchedEffect(accounts) {
        if (state.selectedAccountId == null && accounts.isNotEmpty()) {
            val upiOrFirst = accounts.find { it.name.contains("UPI", ignoreCase = true) } ?: accounts.first()
            viewModel.updateAccount(upiOrFirst.id)
        }
    }

    val filteredCategories = categories.filter {
        if (state.type == TransactionType.INCOME) it.isIncome else !it.isIncome
    }

    LaunchedEffect(filteredCategories, state.type) {
        if (state.selectedCategoryId == null && filteredCategories.isNotEmpty()) {
            viewModel.updateCategory(filteredCategories.first().id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Record Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Monochromatic Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val isSelected = state.type == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.updateType(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else PayUpThemeExtended.secondaryTextColor
                        )
                    }
                }
            }

            // 2. Large Amount Card
            PayUpCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PayUpThemeExtended.secondaryTextColor,
                        letterSpacing = 1.sp
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.amountText,
                            onValueChange = { viewModel.updateAmount(it) },
                            placeholder = { Text("0.00", fontSize = 34.sp, color = PayUpThemeExtended.secondaryTextColor.copy(alpha = 0.5f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .width(IntrinsicSize.Min)
                                .widthIn(min = 120.dp, max = 240.dp)
                        )
                    }
                    Text(
                        "Exact paise precision — zero floating-point rounding errors",
                        style = MaterialTheme.typography.labelSmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
            }

            // 3. Payment Method (UPI, Bank, Cash, Card)
            PayUpSectionHeader(title = "Payment Account")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    PayUpPill(
                        text = account.name,
                        isSelected = state.selectedAccountId == account.id,
                        onClick = { viewModel.updateAccount(account.id) },
                        icon = when {
                            account.name.contains("UPI", ignoreCase = true) -> PayUpIcons.Payments
                            account.name.contains("Bank", ignoreCase = true) -> PayUpIcons.Account
                            account.name.contains("Card", ignoreCase = true) -> PayUpIcons.Transactions
                            else -> PayUpIcons.Wallet
                        }
                    )
                }
            }

            // 4. Category Grid / Quick Select
            PayUpSectionHeader(
                title = "Category",
                actionText = "View All (${filteredCategories.size})",
                onActionClick = { showAllCategoriesSheet = true }
            )

            val topCategories = filteredCategories.take(6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topCategories.chunked(3).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            CategoryPill(
                                category = cat,
                                isSelected = state.selectedCategoryId == cat.id,
                                modifier = Modifier.weight(1f),
                                onSelect = { viewModel.updateCategory(cat.id) }
                            )
                        }
                    }
                }
            }

            // 4b. Budget Envelope Assignment (for Expenses)
            if (state.type == TransactionType.EXPENSE && budgets.isNotEmpty()) {
                PayUpSectionHeader(title = "Budget Envelope")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PayUpPill(
                        text = "Auto (from Category)",
                        isSelected = state.selectedBudgetId == null,
                        onClick = { viewModel.updateBudget(null) }
                    )
                    budgets.forEach { b ->
                        PayUpPill(
                            text = b.name,
                            isSelected = state.selectedBudgetId == b.id,
                            onClick = { viewModel.updateBudget(b.id) }
                        )
                    }
                }
            }

            // 5. Merchant Input
            PayUpSectionHeader(title = "Payee Details")
            OutlinedTextField(
                value = state.merchant,
                onValueChange = { viewModel.updateMerchant(it) },
                label = { Text("Merchant / Payee Name") },
                placeholder = { Text("e.g. Swiggy, Metro, Decathlon, Groceries") },
                leadingIcon = { Icon(PayUpIcons.Restaurant, contentDescription = null, tint = PayUpThemeExtended.secondaryTextColor, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 6. Notes Input
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes (Optional)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            // 7. Large Modern Save Button
            PayUpPrimaryButton(
                text = "Save Transaction",
                icon = PayUpIcons.Check,
                onClick = { viewModel.save() },
                enabled = !state.isSaving && state.amountText.isNotBlank(),
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAllCategoriesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllCategoriesSheet = false },
            containerColor = PayUpThemeExtended.cardColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        val isSelected = state.selectedCategoryId == cat.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.updateCategory(cat.id)
                                    showAllCategoriesSheet = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    PayUpIcons.forCategory(cat.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(PayUpIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategoryPill(
    category: Category,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else PayUpThemeExtended.cardColor
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) null else BorderStroke(1.dp, PayUpThemeExtended.borderColor)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                PayUpIcons.forCategory(category.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                color = contentColor
            )
        }
    }
}
