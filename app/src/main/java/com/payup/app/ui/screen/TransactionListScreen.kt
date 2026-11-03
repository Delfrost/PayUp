package com.payup.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.payup.app.ui.component.PayUpEmptyState
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
import com.payup.app.ui.viewmodel.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: TransactionListViewModel = viewModel()
) {
    val transactions = viewModel.transactions.collectAsLazyPagingItems()
    val count by viewModel.transactionCount.collectAsState(initial = 0)
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.setSearch(it)
                            },
                            placeholder = { Text("Search merchant, notes, UTR...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = PayUpThemeExtended.borderColor,
                                focusedContainerColor = PayUpThemeExtended.cardColor,
                                unfocusedContainerColor = PayUpThemeExtended.cardColor
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.setSearch("")
                                    }) {
                                        Icon(PayUpIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Column {
                            Text("All Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("$count recorded", style = MaterialTheme.typography.labelSmall, color = PayUpThemeExtended.secondaryTextColor)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.clearFilter()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(PayUpIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(PayUpIcons.Search, contentDescription = "Search")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                count = transactions.itemCount,
                key = { transactions[it]?.id ?: it }
            ) { index ->
                val txn = transactions[index]
                if (txn != null) {
                    AppleTransactionRow(txn, onClick = { onTransactionClick(txn.id) })
                }
            }

            when (transactions.loadState.append) {
                is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                is LoadState.Error -> {
                    item {
                        Text(
                            "Error loading more transactions",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = PayUpThemeExtended.secondaryTextColor
                        )
                    }
                }
                else -> {}
            }

            if (transactions.loadState.refresh is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            if (transactions.itemCount == 0 && transactions.loadState.refresh !is LoadState.Loading) {
                item {
                    PayUpEmptyState(
                        icon = PayUpIcons.Transactions,
                        title = if (searchQuery.isNotBlank()) "No Matching Payments" else "No Transactions Yet",
                        description = if (searchQuery.isNotBlank()) "No recorded transaction contains \"$searchQuery\"." else "Tap '+' to add your first transaction.",
                        actionText = if (searchQuery.isBlank()) "Add Transaction" else null,
                        onActionClick = if (searchQuery.isBlank()) onAddTransaction else null
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
