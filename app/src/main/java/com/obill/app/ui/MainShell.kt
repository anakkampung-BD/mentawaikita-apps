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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.obill.app.feature.quota.QuotaCheckDialog
import com.obill.app.feature.quota.QuotaCheckViewModel
import com.obill.app.feature.report.ReportGenerateDialog
import com.obill.app.feature.report.ReportScreen
import com.obill.app.feature.report.ReportViewModel
import com.obill.app.feature.sale.SaleScreen
import com.obill.app.feature.sale.SaleTransactionDialog
import com.obill.app.feature.sale.SaleViewModel
import com.obill.app.ui.components.LowBalanceSweetAlertDialog
import com.obill.app.ui.components.TextScaleControls
import com.obill.app.ui.saldo.SaleSaldoTier
import com.obill.app.ui.saldo.formatSaldoForLowBalanceWarning
import com.obill.app.ui.saldo.saleSaldoTierForEntry
import kotlinx.coroutines.launch

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

private sealed class SaleEntryDialogState {
    abstract val saldoFormatted: String

    data class AfterOkGoDashboard(override val saldoFormatted: String) : SaleEntryDialogState()
    data class AfterOkGoSale(override val saldoFormatted: String) : SaleEntryDialogState()
}

@Composable
fun MainShell(
    container: AppContainer,
    onLogout: () -> Unit,
    onOpenReceipt: (Int) -> Unit,
    onAdjustText: (Float) -> Unit,
    skipSessionSaldoCheck: Boolean = false,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var saleEntryDialog by remember { mutableStateOf<SaleEntryDialogState?>(null) }
    var showQuotaCheck by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showSaleModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val sessionSaldoGateVm: SessionSaldoGateViewModel = viewModel(
        factory = SessionSaldoGateViewModel.factory(container.sellerRepository),
    )
    LaunchedEffect(skipSessionSaldoCheck) {
        sessionSaldoGateVm.startCheckIfNeeded(skipSessionSaldoCheck)
    }
    val lowBalanceSaldoText by sessionSaldoGateVm.warningFormatted.collectAsState()

    val dashVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(container.sellerRepository, container.appUpdateRepository, context),
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
    val quotaCheckVm: QuotaCheckViewModel = viewModel(
        factory = QuotaCheckViewModel.factory(container.quotaRepository),
    )

    fun openSaleWithSaldoCheck() {
        scope.launch {
            val profile = container.sellerRepository.profile().getOrNull()
            if (profile == null) {
                showSaleModal = true
                return@launch
            }
            val formatted = formatSaldoForLowBalanceWarning(profile.saldo)
            when (saleSaldoTierForEntry(profile.saldo)) {
                SaleSaldoTier.Sufficient -> showSaleModal = true
                SaleSaldoTier.BelowCritical ->
                    saleEntryDialog = SaleEntryDialogState.AfterOkGoDashboard(formatted)
                SaleSaldoTier.LowNeedTopUp ->
                    saleEntryDialog = SaleEntryDialogState.AfterOkGoSale(formatted)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, t ->
                        NavigationBarItem(
                            selected = when {
                                index == 2 -> showSaleModal || tab == 2
                                index == 3 -> showReportDialog || tab == 3
                                else -> tab == index
                            },
                            onClick = {
                                when (index) {
                                    2 -> openSaleWithSaldoCheck()
                                    3 -> {
                                        showReportDialog = true
                                    }
                                    else -> tab = index
                                }
                            },
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
                        onOpenSale = { openSaleWithSaldoCheck() },
                        onOpenQuota = {
                            quotaCheckVm.reset()
                            showQuotaCheck = true
                        },
                        onOpenReport = { showReportDialog = true },
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
        lowBalanceSaldoText?.let { saldoFormatted ->
            LowBalanceSweetAlertDialog(
                saldoFormatted = saldoFormatted,
                onConfirm = { sessionSaldoGateVm.dismissWarning() },
            )
        }
        saleEntryDialog?.let { state ->
            LowBalanceSweetAlertDialog(
                saldoFormatted = state.saldoFormatted,
                onConfirm = {
                    saleEntryDialog = null
                    when (state) {
                        is SaleEntryDialogState.AfterOkGoDashboard -> tab = 0
                        is SaleEntryDialogState.AfterOkGoSale -> showSaleModal = true
                    }
                },
            )
        }
        if (showQuotaCheck) {
            QuotaCheckDialog(
                viewModel = quotaCheckVm,
                onDismiss = {
                    showQuotaCheck = false
                    quotaCheckVm.reset()
                },
            )
        }
        if (showReportDialog) {
            ReportGenerateDialog(
                viewModel = reportVm,
                onDismiss = { showReportDialog = false },
            )
        }
        if (showSaleModal) {
            SaleTransactionDialog(
                viewModel = saleVm,
                onDismiss = {
                    showSaleModal = false
                    saleVm.clearSubmitMessage()
                },
            )
        }
    }
}
