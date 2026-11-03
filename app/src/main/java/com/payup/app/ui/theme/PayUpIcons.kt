package com.payup.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.vector.ImageVector

object PayUpIcons {
    val Dashboard: ImageVector = Icons.Filled.Dashboard
    val Transactions: ImageVector = Icons.Filled.Receipt
    val Add: ImageVector = Icons.Filled.Add
    val Search: ImageVector = Icons.Filled.Search
    val Settings: ImageVector = Icons.Filled.Settings
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Edit: ImageVector = Icons.Filled.Edit
    val Delete: ImageVector = Icons.Filled.Delete
    val Category: ImageVector = Icons.Filled.Category
    val Account: ImageVector = Icons.Filled.AccountBalance
    val ExpandMore: ImageVector = Icons.Filled.ExpandMore
    val ExpandLess: ImageVector = Icons.Filled.ExpandLess
    val Income: ImageVector = Icons.AutoMirrored.Filled.TrendingUp
    val Expense: ImageVector = Icons.AutoMirrored.Filled.TrendingDown
    val Savings: ImageVector = Icons.Filled.Savings
    val Transfer: ImageVector = Icons.Filled.SwapHoriz
    val Calendar: ImageVector = Icons.Filled.CalendarMonth
    val Close: ImageVector = Icons.Filled.Close
    val Check: ImageVector = Icons.Filled.Check
    val Info: ImageVector = Icons.Outlined.Info
    val FilterList: ImageVector = Icons.Filled.FilterList
    val MoreVert: ImageVector = Icons.Filled.MoreVert
    val ChevronRight: ImageVector = Icons.Filled.ChevronRight
    val Wallet: ImageVector = Icons.Filled.AccountBalanceWallet
    val Restaurant: ImageVector = Icons.Filled.Restaurant
    val DirectionsCar: ImageVector = Icons.Filled.DirectionsCar
    val ShoppingBag: ImageVector = Icons.Filled.ShoppingBag
    val ReceiptLong: ImageVector = Icons.AutoMirrored.Filled.ReceiptLong
    val Movie: ImageVector = Icons.Filled.Movie
    val LocalHospital: ImageVector = Icons.Filled.LocalHospital
    val School: ImageVector = Icons.Filled.School
    val Home: ImageVector = Icons.Filled.Home
    val Payments: ImageVector = Icons.Filled.Payments
    val Work: ImageVector = Icons.Filled.Work
    val PhotoCamera: ImageVector = Icons.Filled.PhotoCamera
    val Warning: ImageVector = Icons.Filled.Warning

    fun forCategory(iconName: String): ImageVector = when (iconName) {
        "restaurant" -> Restaurant
        "directions_car" -> DirectionsCar
        "shopping_bag" -> ShoppingBag
        "receipt_long" -> ReceiptLong
        "movie" -> Movie
        "local_hospital" -> LocalHospital
        "school" -> School
        "home" -> Home
        "payments" -> Payments
        "work" -> Work
        "account_balance_wallet" -> Wallet
        "subscriptions" -> Transactions
        "local_grocery_store" -> ShoppingBag
        "money" -> Payments
        "category" -> Category
        "help_outline" -> Info
        else -> Category
    }
}
