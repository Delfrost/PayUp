package com.payup.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payup.app.ui.screen.*
import com.payup.app.ui.viewmodel.ScreenshotReviewViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction/{transactionId}"
    const val ADD_TRANSACTION = "add_transaction"
    const val CATEGORY_TRANSACTIONS = "category/{categoryId}"
    const val SCREENSHOT_REVIEW = "screenshot_review"

    fun transactionDetail(id: String) = "transaction/$id"
    fun categoryTransactions(categoryId: String) = "category/$categoryId"
}

@Composable
fun PayUpNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val screenshotReviewViewModel: ScreenshotReviewViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onTransactionClick = { navController.navigate(Routes.transactionDetail(it)) },
                onCategoryClick = { navController.navigate(Routes.categoryTransactions(it)) },
                onViewAllTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                onScanScreenshots = { uris ->
                    screenshotReviewViewModel.processScreenshots(uris)
                    navController.navigate(Routes.SCREENSHOT_REVIEW)
                }
            )
        }
        composable(Routes.SCREENSHOT_REVIEW) {
            ScreenshotReviewScreen(
                onBack = { navController.popBackStack() },
                onImportComplete = { navController.popBackStack() },
                viewModel = screenshotReviewViewModel
            )
        }
        composable(Routes.TRANSACTIONS) {
            TransactionListScreen(
                onTransactionClick = { navController.navigate(Routes.transactionDetail(it)) },
                onBack = { navController.popBackStack() },
                onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) }
            )
        }
        composable(
            Routes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: return@composable
            TransactionDetailScreen(
                transactionId = transactionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADD_TRANSACTION) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            Routes.CATEGORY_TRANSACTIONS,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
            CategoryTransactionsScreen(
                categoryId = categoryId,
                onTransactionClick = { navController.navigate(Routes.transactionDetail(it)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
