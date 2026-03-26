package com.obill.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenReceipt: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var removeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Riwayat transaksi", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        removeError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
        }
        when {
            state.loading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            else -> {
                val items = state.envelope?.data.orEmpty()
                val pg = state.envelope?.pagination
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { row ->
                        val q = state.quotaById[row.id]
                        ObillCard {
                            Text(row.profil.orEmpty(), style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${row.buyer.orEmpty()} • ${row.kodeVoucher.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                row.tanggal.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                            Text(
                                formatRupiahPlain(row.sellingPrice?.toDoubleOrNull() ?: 0.0),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            if (state.quotaLoading) {
                                Text("Memuat kuota…", style = MaterialTheme.typography.bodySmall)
                            } else if (q != null) {
                                Text(
                                    "Kuota: ${q.remainingFmt.orEmpty()} / ${q.limitFmt.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        row.id.toIntOrNull()?.let { onOpenReceipt(it) }
                                    },
                                ) {
                                    Text("Struk")
                                }
                                Button(
                                    onClick = {
                                        removeError = null
                                        viewModel.removeExpired(row.id.toIntOrNull() ?: 0) { err ->
                                            removeError = err
                                        }
                                    },
                                ) {
                                    Text("Hapus expired")
                                }
                            }
                        }
                    }
                }
                pg?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Button(
                            onClick = { viewModel.load((p.page - 1).coerceAtLeast(1)) },
                            enabled = p.page > 1,
                        ) {
                            Text("Sebelumnya")
                        }
                        Text(
                            "${p.page} / ${p.totalPages}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Button(
                            onClick = { viewModel.load(p.page + 1) },
                            enabled = p.page < p.totalPages,
                        ) {
                            Text("Berikutnya")
                        }
                    }
                }
            }
        }
    }
}
