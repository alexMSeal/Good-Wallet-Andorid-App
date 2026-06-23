package com.androidstudio.myapplication.design.navgraph

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.androidstudio.myapplication.design.navgraph.NavRoutes
import com.androidstudio.myapplication.design.screens.HomeScreen
import com.androidstudio.myapplication.design.screens.DashboardScreen
import com.androidstudio.myapplication.design.screens.WalletScreen
import com.androidstudio.myapplication.design.screens.SettingsScreen
import com.androidstudio.myapplication.ui.EditTransactionScreen

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.androidstudio.myapplication.datastore.DataStoreManager
import com.androidstudio.myapplication.model.HomeScreenViewModel
import com.androidstudio.myapplication.model.HomeScreenViewModelFactory
import com.androidstudio.myapplication.model.SettingsViewModel
import com.androidstudio.myapplication.model.SettingsViewModelFactory
import com.androidstudio.myapplication.repository.AlbumRepository


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val albumRepository = remember { AlbumRepository(dataStoreManager) }
    val viewModel: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModelFactory(dataStoreManager, albumRepository)
    )
    val application = context.applicationContext as Application
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application, dataStoreManager)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .height(56.dp), // adjust height as needed
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF50C878)
                ),
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            color = Color(0xFFE8FCE8),
                            text = "Good Wallet",
                            style = MaterialTheme.typography.headlineSmall.copy( // Customize as needed
                                fontWeight = FontWeight.Bold, fontSize = 24.sp,
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF50C878))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(NavRoutes.Home.route) { HomeScreen(navController = navController, viewModel = viewModel) }
                    composable(NavRoutes.Dashboard.route) {
                        DashboardScreen(navController = navController, viewModel = viewModel, settingsViewModel = settingsViewModel )
                    }
                    composable(NavRoutes.Wallet.route) { WalletScreen(navController, viewModel = viewModel, settingsViewModel = settingsViewModel) }
                    composable(NavRoutes.Settings.route) {
                        val context = LocalContext.current
                        val application = context.applicationContext as Application
                        val dataStoreManager = remember { DataStoreManager(context) }

                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(application, dataStoreManager)
                        )

                        SettingsScreen(navController, settingsViewModel)
                    }



                    composable(NavRoutes.EditTransaction.route) {
                        EditTransactionScreen(
                            existingExpense = null,
                            navController = navController,
                            viewModel = viewModel,
                            onSave = { expense ->
                                viewModel.addTransaction(expense)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = NavRoutes.EditTransaction.route + "/{transactionId}",
                        arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                        val existingExpense = viewModel.expenseList.find { it.id == transactionId }
                        EditTransactionScreen(
                            existingExpense = existingExpense,
                            navController = navController,
                            viewModel = viewModel,
                            onSave = { expense ->
                                viewModel.updateTransaction(expense) // 🔁 update instead of add
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    )
}


fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1 - factor)).coerceIn(0f, 1f),
        green = (green * (1 - factor)).coerceIn(0f, 1f),
        blue = (blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}


@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    selectedIconColor: Color = Color(0xFFFFC674),
    unselectedIconColor: Color = Color(0xFFE8FCE8),
    selectedTextColor: Color = Color(0xFFFFC674),
    unselectedTextColor: Color = Color(0xFFE8FCE8),
    backgroundColor: Color = Color(0xFF2F4558)
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column {
        Divider(color = backgroundColor.copy(alpha = 0.2f))

        BottomNavigation(
            backgroundColor = backgroundColor.copy(alpha = 0.9f),
            contentColor = selectedIconColor
        ) {
            val items = listOf(
                Triple("Home", Icons.Filled.Home, NavRoutes.Home.route),
                Triple("Dash", Icons.Filled.BarChart, NavRoutes.Dashboard.route),
                Triple("Wallet", Icons.Filled.AccountBalanceWallet, NavRoutes.Wallet.route),
                Triple("Settings", Icons.Filled.Settings, NavRoutes.Settings.route),
            )

            items.forEach { (label, icon, route) ->
                val selected = currentRoute == route

                BottomNavigationItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) selectedIconColor else unselectedIconColor
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = if (selected) selectedTextColor else unselectedTextColor
                        )
                    },
                    selectedContentColor = selectedIconColor,
                    unselectedContentColor = unselectedIconColor
                )
            }
        }
    }
}
