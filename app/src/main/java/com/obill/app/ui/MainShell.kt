package com.obill.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obill.app.AppContainer
import com.obill.app.feature.dashboard.DashboardScreen
import com.obill.app.feature.dashboard.DashboardViewModel
import com.obill.app.feature.history.HistoryScreen
import com.obill.app.feature.history.HistoryViewModel
import com.obill.app.feature.profile.ProfileScreen
import com.obill.app.feature.profile.ProfileViewModel
import com.obill.app.feature.report.ReportScreen
import com.obill.app.feature.report.ReportViewModel
import com.obill.app.feature.sale.SaleScreen
import com.obill.app.feature.sale.SaleViewModel
import com.obill.app.ui.components.TextScaleControls

private data class Tab(
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("Beranda", Icons.Default.Home),
    Tab("Riwayat", Icons.Default.ReceiptLong),
    Tab("Jual", Icons.Default.AddShoppingCart),
    Tab("Laporan", Icons.Default.Assessment),
    Tab("Profil", Icons.Default.Person),
)

@Composable
fun MainShell(
    container: AppContainer,
    onLogout: () -> Unit,
    onOpenReceipt: (Int) -> Unit,
    onAdjustText: (Float) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current

    val dashVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(container.sellerRepository, context),
    )
    val histVm: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(container.sellerRepository),
    )
    val saleVm: SaleViewModel = viewModel(
        factory = SaleViewModel.factory(container.sellerRepository),
    )
    val reportVm: ReportViewModel = viewModel(
        factory = ReportViewModel.factory(container.sellerRepository, context),
    )
    val profileVm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(container.sellerRepository, container.authRepository),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                0 -> DashboardScreen(
                    viewModel = dashVm,
                    onOpenSale = { tab = 2 },
                    onOpenQuota = { tab = 1 },
                    onOpenReport = { tab = 3 },
                )
                1 -> HistoryScreen(
                    viewModel = histVm,
                    onOpenReceipt = onOpenReceipt,
                )
                2 -> SaleScreen(viewModel = saleVm)
                3 -> ReportScreen(viewModel = reportVm)
                4 -> ProfileScreen(
                    viewModel = profileVm,
                    onLogout = onLogout,
                )
            }
            TextScaleControls(
                onDecrease = { onAdjustText(-0.05f) },
                onIncrease = { onAdjustText(0.05f) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )
        }
    }
}
