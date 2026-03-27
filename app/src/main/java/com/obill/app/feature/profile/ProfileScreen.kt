package com.obill.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obill.app.data.remote.SaldoTopupItemDto
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            viewModel.load()
        },
    )
    LaunchedEffect(state.loading, state.topupLoading) {
        if (!state.loading && !state.topupLoading) refreshing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
        Text("Profil seller", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        when {
            state.loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            }

            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.data != null -> {
                val d = state.data!!
                ObillCard {
                    Text("Profil Seller", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    DetailRow("Nama", d.name.orEmpty().ifBlank { "-" })
                    DetailRow("Email", d.email.orEmpty().ifBlank { "-" })
                    DetailRow("No. Hp", d.hp.orEmpty().ifBlank { "-" })
                    DetailRow("Tanggal bergabung", formatProfileDate(d.dateCreated))
                    DetailRow("Saldo", d.saldo?.let { formatRupiahPlain(it).replace("Rp ", "Rp. ") } ?: "-")
                }

                Spacer(Modifier.height(20.dp))
                Text("History top up", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                when {
                    state.topupLoading -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
                    }

                    state.topupError != null -> {
                        Text(
                            state.topupError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    else -> {
                        val envMsg = state.topupApiMessage?.takeIf { it.isNotBlank() }
                        if (envMsg != null) {
                            Text(
                                envMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        val h = state.topupHistory
                        if (h != null) {
                            ObillCard {
                                Text(
                                    "Total Saldo Bulan ${currentMonthName()}",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(Modifier.height(8.dp))
                                DetailRow("Bulan", monthName(h.month))
                                DetailRow("Tahun", h.year?.toString() ?: "-")
                                DetailRow(
                                    "Total Saldo",
                                    h.totalAmount?.let { formatRupiahPlain(it.toDouble()).replace("Rp ", "Rp. ") } ?: "-",
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            if (h.items.isEmpty()) {
                                Text(
                                    "Belum ada riwayat top-up pada periode ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                )
                            } else {
                                h.items.forEach { item ->
                                    Spacer(Modifier.height(8.dp))
                                    TopupItemCard(item)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    enabled = !state.logoutLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.logoutLoading) "Memproses…" else "Keluar")
                }
            }
        }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun TopupItemCard(item: SaldoTopupItemDto) {
    val amountLabel = item.amount?.let { formatRupiahPlain(it.toDouble()).replace("Rp ", "Rp. ") } ?: "-"
    val dateLabel = formatTopupDate(item.createdAt)
    val timeLabel = formatTopupTime(item.createdAt)
    val transactionNo = "${item.id}/TOPUP/${currentMonthNumber()}/2026"
    ObillCard {
        Text(transactionNo, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        DetailRow("Jumlah", amountLabel)
        DetailRow("Tanggal", dateLabel)
        DetailRow("Waktu", timeLabel)
        DetailRow("Action by", item.createdByName.orEmpty().ifBlank { "-" })
    }
}

private fun currentMonthName(): String =
    SimpleDateFormat("MMMM", Locale("id", "ID")).format(Date())

private fun currentMonthNumber(): String =
    SimpleDateFormat("MM", Locale("id", "ID")).format(Date())

private fun monthName(month: Int?): String {
    if (month == null) return "-"
    val cal = Calendar.getInstance().apply {
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
    }
    return SimpleDateFormat("MMMM", Locale("id", "ID")).format(cal.time)
}

private fun formatProfileDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
        ).asSequence().mapNotNull { fmt ->
            runCatching { fmt.parse(raw) }.getOrNull()
        }.firstOrNull() ?: return raw
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(parsed)
    }.getOrElse { raw }
}

private fun formatTopupDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw) ?: return raw
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(parsed)
    }.getOrElse { raw }
}

private fun formatTopupTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw) ?: return "-"
        SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(parsed)
    }.getOrElse { "-" }
}
