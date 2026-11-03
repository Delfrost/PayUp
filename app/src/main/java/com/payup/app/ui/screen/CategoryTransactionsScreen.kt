package com.payup.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.payup.app.ui.component.PayUpEmptyState
import com.payup.app.ui.theme.PayUpIcons
import com.payup.app.ui.theme.PayUpThemeExtended
import com.payup.app.ui.viewmodel.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    categoryId: String,
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionListViewModel = viewModel()
) {
    LaunchedEffect(categoryId) {
        viewModel.setCategoryFilter(categoryId)
    }

    val transactions = viewModel.transactions.collectAsLazyPagingItems()
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
                            placeholder = { Text("Search in category...", style = MaterialTheme.typography.bodyMedium) },
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
                                        viewModel.setCategoryFilter(categoryId)
                                    }) {
                                        Icon(PayUpIcons.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Text("Category History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.setCategoryFilter(categoryId)
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
                            Icon(PayUpIcons.Search, contentDescription = "Search Category")
                        }
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

            if (transactions.itemCount == 0) {
                item {
                    PayUpEmptyState(
                        icon = PayUpIcons.Transactions,
                        title = if (searchQuery.isNotBlank()) "No Matching Payments" else "No Payments In Category",
                        description = if (searchQuery.isNotBlank()) "No payments matched \"$searchQuery\"." else "No transactions have been recorded in this category yet."
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
