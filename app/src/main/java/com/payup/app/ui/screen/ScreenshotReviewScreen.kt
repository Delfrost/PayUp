package com.payup.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.Account
import com.payup.app.data.model.Category
import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.service.ScreenshotCandidate
import com.payup.app.ui.component.*
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
import com.payup.app.ui.viewmodel.ScreenshotReviewViewModel
import com.payup.app.util.CurrencyUtils
import com.payup.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotReviewScreen(
    onBack: () -> Unit,
    onImportComplete: () -> Unit,
    viewModel: ScreenshotReviewViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())

    var editingCandidate by remember { mutableStateOf<ScreenshotCandidate?>(null) }

    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) {
            onImportComplete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Review Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "On-device OCR Extraction",
                            style = MaterialTheme.typography.labelSmall,
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.candidates.isNotEmpty()) {
                        val allSelected = state.candidates.none { it.isExcluded }
                        TextButton(
                            onClick = {
                                if (allSelected) viewModel.deselectAll() else viewModel.selectAll()
                            }
                        ) {
                            Text(
                                if (allSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (state.candidates.isNotEmpty() && !state.isScanning) {
                val validCount = state.candidates.count { !it.isExcluded }
                val totalPaise = state.candidates.filter { !it.isExcluded }.sumOf { it.amountPaise }

                Surface(
                    color = PayUpThemeExtended.cardColor,
                    border = BorderStroke(1.dp, PayUpThemeExtended.borderColor),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "$validCount selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = PayUpThemeExtended.secondaryTextColor
                            )
                            Text(
                                CurrencyUtils.formatPaiseCompact(totalPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        PayUpPrimaryButton(
                            text = "Import (${validCount})",
                            onClick = { viewModel.confirmImport() },
                            enabled = validCount > 0 && !state.isImporting,
                            isLoading = state.isImporting
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isScanning -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Analyzing Screenshots",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Extracting payment receipts on-device with ML Kit...",
                            style = MaterialTheme.typography.bodySmall,
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                }
            }
            state.candidates.isEmpty() -> {
                PayUpEmptyState(
                    icon = PayUpIcons.Info,
                    title = "No Transactions Found",
                    description = state.error ?: "Could not detect transaction amounts or merchants in the selected images. Try taking a clearer screenshot.",
                    actionText = "Choose Again",
                    onActionClick = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Verify detected payments before importing. Every transaction can be edited or excluded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PayUpThemeExtended.secondaryTextColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    items(state.candidates, key = { it.id }) { candidate ->
                        CandidateReviewCard(
                            candidate = candidate,
                            categories = categories,
                            accounts = accounts,
                            onToggleExclude = { viewModel.toggleExclude(candidate.id) },
                            onEdit = { editingCandidate = candidate },
                            onDelete = { viewModel.deleteCandidate(candidate.id) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Modal Sheet for Editing Candidate Details
    editingCandidate?.let { cand ->
        EditCandidateSheet(
            candidate = cand,
            categories = categories,
            accounts = accounts,
            onDismiss = { editingCandidate = null },
            onSave = { updated ->
                viewModel.updateCandidate(cand.id) { updated }
                editingCandidate = null
            }
        )
    }
}

@Composable
private fun CandidateReviewCard(
    candidate: ScreenshotCandidate,
    categories: List<Category>,
    accounts: List<Account>,
    onToggleExclude: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryName = categories.find { it.id == candidate.categoryId }?.name ?: "Uncategorized"
    val accountName = accounts.find { it.id == candidate.accountId }?.name ?: "UPI / Wallet"

    PayUpCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Checkbox, Source App Pill, Status Badge, Trash
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = !candidate.isExcluded,
                    onCheckedChange = { onToggleExclude() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                // Source App Badge
                PayUpBadge(
                    text = candidate.sourceApp,
                    icon = PayUpIcons.PhotoCamera
                )
                Spacer(Modifier.width(8.dp))
                // Status Badge
                when (candidate.status) {
                    TransactionStatus.COMPLETED -> PayUpBadge(text = "Completed", icon = PayUpIcons.Check)
                    TransactionStatus.PENDING -> PayUpBadge(text = "Pending", icon = PayUpIcons.Calendar, isWarning = true)
                    TransactionStatus.FAILED -> PayUpBadge(text = "Failed", icon = PayUpIcons.Close, isNegative = true)
                    TransactionStatus.REFUNDED -> PayUpBadge(text = "Refunded", icon = PayUpIcons.Transfer)
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        PayUpIcons.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = PayUpThemeExtended.secondaryTextColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Middle Row: Merchant & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        candidate.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (candidate.isExcluded) PayUpThemeExtended.secondaryTextColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        candidate.rawDateText ?: DateUtils.formatDateTime(candidate.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                val sign = if (candidate.type == TransactionType.INCOME) "+" else "-"
                Text(
                    sign + CurrencyUtils.formatPaise(candidate.amountPaise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (candidate.isExcluded) PayUpThemeExtended.secondaryTextColor else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            // Bottom metadata pills: Category, Account, UTR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, PayUpThemeExtended.borderColor)
                    ) {
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, PayUpThemeExtended.borderColor)
                    ) {
                        Text(
                            accountName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                }

                if (candidate.referenceId != null) {
                    Text(
                        "UTR: ${candidate.referenceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PayUpThemeExtended.secondaryTextColor
                    )
                }
            }

            // Duplicate warning banner
            if (candidate.isLikelyDuplicate) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, PayUpThemeExtended.borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            PayUpIcons.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Likely duplicate: Similar transaction already recorded",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCandidateSheet(
    candidate: ScreenshotCandidate,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (ScreenshotCandidate) -> Unit
) {
    var merchantText by remember { mutableStateOf(candidate.merchant) }
    var amountText by remember { mutableStateOf((candidate.amountPaise / 100.0).toString().removeSuffix(".0")) }
    var selectedCategoryId by remember { mutableStateOf(candidate.categoryId) }
    var selectedAccountId by remember { mutableStateOf(candidate.accountId ?: accounts.firstOrNull()?.id) }
    var selectedType by remember { mutableStateOf(candidate.type) }

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
            Text(
                "Edit Scanned Payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Direction Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PayUpPill(
                    text = "Expense",
                    isSelected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    modifier = Modifier.weight(1f)
                )
                PayUpPill(
                    text = "Income",
                    isSelected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    modifier = Modifier.weight(1f)
                )
            }

            // Amount Input
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

            // Merchant / Recipient
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text("Merchant / Recipient") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = PayUpThemeExtended.borderColor
                )
            )

            // Category Selection
            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(
                modifier = Modifier.heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategoryId = cat.id },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Save Button
            PayUpPrimaryButton(
                text = "Apply Changes",
                onClick = {
                    val paise = CurrencyUtils.parseToPaise(amountText)
                    onSave(
                        candidate.copy(
                            merchant = merchantText.ifBlank { "UPI Payment" },
                            amountPaise = if (paise > 0) paise else candidate.amountPaise,
                            categoryId = selectedCategoryId,
                            accountId = selectedAccountId,
                            type = selectedType
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
