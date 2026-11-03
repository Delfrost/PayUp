package com.payup.app.ui.screen

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.data.model.TransactionType
import com.payup.app.service.ScreenshotCandidate
import com.payup.app.ui.theme.PayUpIcons
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

    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) {
            onImportComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Scanned Payments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            val validCount = state.candidates.count { !it.isExcluded }
            val totalAmount = state.candidates.filter { !it.isExcluded }.sumOf { it.amountPaise }
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$validCount transaction${if (validCount != 1) "s" else ""} ready",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Total: ${CurrencyUtils.formatPaiseCompact(totalAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.confirmImport() },
                        enabled = validCount > 0 && !state.isImporting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Confirm & Import ($validCount)")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isScanning) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Scanning screenshots on-device with ML Kit...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (state.candidates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(PayUpIcons.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(state.error ?: "No transaction data detected in image.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Verify and edit payments extracted from your screenshot. Uncheck any item you don't want to import.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(state.candidates, key = { it.id }) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onToggleExclude = { viewModel.toggleExclude(candidate.id) },
                        onUpdateMerchant = { newMerchant ->
                            viewModel.updateCandidate(candidate.id) { it.copy(merchant = newMerchant) }
                        },
                        onUpdateAmount = { newPaise ->
                            viewModel.updateCandidate(candidate.id) { it.copy(amountPaise = newPaise) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ScreenshotCandidate,
    onToggleExclude: () -> Unit,
    onUpdateMerchant: (String) -> Unit,
    onUpdateAmount: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (candidate.isExcluded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = !candidate.isExcluded,
                    onCheckedChange = { onToggleExclude() }
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        candidate.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        DateUtils.formatDateTime(candidate.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    (if (candidate.type == TransactionType.INCOME) "+" else "-") +
                            CurrencyUtils.formatPaiseCompact(candidate.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (candidate.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }

            if (candidate.isLikelyDuplicate) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(PayUpIcons.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Likely duplicate: Similar transaction already in ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (candidate.referenceId != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ref / UTR: ${candidate.referenceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
