package com.obill.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.obill.app.AppContainer
import com.obill.app.UnauthorizedBus
import com.obill.app.feature.auth.LoginScreen
import com.obill.app.feature.auth.LoginViewModel
import com.obill.app.feature.dashboard.DashboardScreen
import com.obill.app.feature.dashboard.DashboardViewModel
import com.obill.app.feature.history.HistoryScreen
import com.obill.app.feature.history.HistoryViewModel
import com.obill.app.feature.history.ReceiptScreen
import com.obill.app.feature.history.ReceiptViewModel
import com.obill.app.feature.profile.ProfileScreen
import com.obill.app.feature.profile.ProfileViewModel
import com.obill.app.feature.report.ReportScreen
import com.obill.app.feature.report.ReportViewModel
import com.obill.app.feature.sale.SaleScreen
import com.obill.app.feature.sale.SaleViewModel
import com.obill.app.ui.MainShell
import com.obill.app.ui.SplashRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ObillNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        UnauthorizedBus.events.collect {
            container.authRepository.clearSessionLocalOnly()
            navController.navigate(Routes.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
            SplashRoute(
                container = container,
                onDone = { hasToken ->
                    if (hasToken) {
                        navController.navigate(Routes.Main) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.Login) {
            val vm: LoginViewModel = viewModel(
                factory = LoginViewModel.factory(container.authRepository),
            )
            val state by vm.state.collectAsState()
            LoginScreen(
                state = state,
                onLogin = { e, p -> vm.login(e, p) },
                onLoginSuccess = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onAdjustText = { delta ->
                    scope.launch {
                        val cur = container.tokenStore.textScale.first()
                        container.tokenStore.setTextScale(cur + delta)
                    }
                },
            )
        }
        composable(Routes.Main) {
            MainShell(
                container = container,
                onLogout = {
                    scope.launch {
                        container.authRepository.logout()
                        navController.navigate(Routes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onOpenReceipt = { id ->
                    navController.navigate(Routes.receipt(id))
                },
                onAdjustText = { delta ->
                    scope.launch {
                        val cur = container.tokenStore.textScale.first()
                        container.tokenStore.setTextScale(cur + delta)
                    }
                },
            )
        }
        composable(
            route = Routes.Receipt,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: return@composable
            val vm: ReceiptViewModel = viewModel(
                key = "receipt_$id",
                factory = ReceiptViewModel.factory(container.sellerRepository, id),
            )
            val state by vm.state.collectAsState()
            ReceiptScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onResend = { vm.resend() },
                onAdjustText = { delta ->
                    scope.launch {
                        val cur = container.tokenStore.textScale.first()
                        container.tokenStore.setTextScale(cur + delta)
                    }
                },
            )
        }
    }
}

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Main = "main"
    const val Receipt = "receipt/{id}"
    fun receipt(id: Int) = "receipt/$id"
}
