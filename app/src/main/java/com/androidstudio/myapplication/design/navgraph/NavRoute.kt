package com.androidstudio.myapplication.design.navgraph

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Dashboard : NavRoutes("dashboard")
    object Wallet : NavRoutes("wallet")
    object Settings : NavRoutes("settings")
    object EditTransaction : NavRoutes("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    object EditTransactionWithId : NavRoutes("edit_transaction_screen/{id}")
}