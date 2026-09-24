package com.buztrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buztrack.app.data.repository.BuztrackRepository
import com.buztrack.app.data.session.SessionManager
import com.buztrack.app.domain.repository.AuthRepository
import com.buztrack.app.ui.components.BuztrackBottomNav
import com.buztrack.app.ui.components.NavTab
import com.buztrack.app.ui.screens.auth.AuthViewModel
import com.buztrack.app.ui.screens.auth.LoginScreen
import com.buztrack.app.ui.screens.auth.RegisterScreen
import com.buztrack.app.ui.screens.auth.SplashScreen
import com.buztrack.app.ui.screens.bills.BillsScreen
import com.buztrack.app.ui.screens.cashbook.CashBookScreen
import com.buztrack.app.ui.screens.customers.CustomerDetailScreen
import com.buztrack.app.ui.screens.customers.CustomersScreen
import com.buztrack.app.ui.screens.customers.CustomersViewModel
import com.buztrack.app.ui.screens.dailyclosing.DailyClosingScreen
import com.buztrack.app.ui.screens.home.HomeScreen
import com.buztrack.app.ui.screens.home.HomeViewModel
import com.buztrack.app.ui.screens.more.MoreScreen
import com.buztrack.app.ui.screens.reports.ReportsScreen
import com.buztrack.app.ui.screens.scan.ScanBillScreen
import com.buztrack.app.ui.screens.scan.ScanBillViewModel
import com.buztrack.app.ui.screens.suppliers.SupplierDetailScreen
import com.buztrack.app.ui.screens.suppliers.SuppliersScreen
import com.buztrack.app.ui.screens.suppliers.SuppliersViewModel
import com.buztrack.app.ui.screens.transactions.TransactionsScreen
import com.buztrack.app.ui.screens.transactions.TransactionsViewModel

@Composable
fun BuztrackMainApp(
    repository: BuztrackRepository,
    authRepository: AuthRepository,
    sessionManager: SessionManager,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

    // Show bottom nav for primary tab routes
    val showBottomNav = currentRoute in listOf(
        NavTab.HOME.route,
        NavTab.TRANSACTIONS.route,
        NavTab.SCAN.route,
        NavTab.REPORTS.route,
        NavTab.MORE.route
    )

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(authRepository, sessionManager))
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory(repository))
    val scanBillViewModel: ScanBillViewModel = viewModel(factory = ScanBillViewModel.Factory(repository))
    val customersViewModel: CustomersViewModel = viewModel(factory = CustomersViewModel.Factory(repository))
    val suppliersViewModel: SuppliersViewModel = viewModel(factory = SuppliersViewModel.Factory(repository))

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BuztrackBottomNav(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        if (tab == NavTab.HOME) {
                            navController.popBackStack(NavTab.HOME.route, inclusive = false)
                        } else {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(paddingValues)
        ) {
            // AUTH 1: SPLASH
            composable("splash") {
                SplashScreen(
                    isLoggedIn = authViewModel.isLoggedIn(),
                    onNavigateToHome = {
                        navController.navigate(NavTab.HOME.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            // AUTH 2: LOGIN
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(NavTab.HOME.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            // AUTH 3: REGISTER
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(NavTab.HOME.route) {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            // TAB 1: HOME
            composable(NavTab.HOME.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToScan = { navController.navigate(NavTab.SCAN.route) },
                    onNavigateToTransactions = { navController.navigate(NavTab.TRANSACTIONS.route) },
                    onNavigateToCustomers = { navController.navigate("customers") },
                    onNavigateToSuppliers = { navController.navigate("suppliers") },
                    onNavigateToProfile = { navController.navigate(NavTab.MORE.route) }
                )
            }

            // TAB 2: TRANSACTIONS
            composable(NavTab.TRANSACTIONS.route) {
                TransactionsScreen(viewModel = transactionsViewModel)
            }

            // TAB 3: SCAN BILL
            composable(NavTab.SCAN.route) {
                ScanBillScreen(
                    viewModel = scanBillViewModel,
                    onBillSavedSuccess = { navController.navigate(NavTab.HOME.route) }
                )
            }

            // TAB 4: REPORTS
            composable(NavTab.REPORTS.route) {
                ReportsScreen(repository = repository)
            }

            // TAB 5: MORE
            composable(NavTab.MORE.route) {
                MoreScreen(
                    repository = repository,
                    onNavigateToCustomers = { navController.navigate("customers") },
                    onNavigateToSuppliers = { navController.navigate("suppliers") },
                    onNavigateToCashBook = { navController.navigate("cashbook") },
                    onNavigateToBills = { navController.navigate("bills") },
                    onNavigateToDailyClosing = { navController.navigate("dailyclosing") },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // CUSTOMERS
            composable("customers") {
                CustomersScreen(
                    viewModel = customersViewModel,
                    onCustomerClick = { customerId ->
                        navController.navigate("customer_detail/$customerId")
                    }
                )
            }

            // CUSTOMER DETAIL
            composable(
                route = "customer_detail/{customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                CustomerDetailScreen(
                    viewModel = customersViewModel,
                    customerId = customerId,
                    onBack = { navController.popBackStack() }
                )
            }

            // SUPPLIERS
            composable("suppliers") {
                SuppliersScreen(
                    viewModel = suppliersViewModel,
                    onSupplierClick = { supplierId ->
                        navController.navigate("supplier_detail/$supplierId")
                    }
                )
            }

            // SUPPLIER DETAIL
            composable(
                route = "supplier_detail/{supplierId}",
                arguments = listOf(navArgument("supplierId") { type = NavType.StringType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getString("supplierId") ?: ""
                SupplierDetailScreen(
                    viewModel = suppliersViewModel,
                    supplierId = supplierId,
                    onBack = { navController.popBackStack() }
                )
            }

            // CASH BOOK
            composable("cashbook") {
                CashBookScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            // BILLS
            composable("bills") {
                BillsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            // DAILY CLOSING
            composable("dailyclosing") {
                DailyClosingScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
