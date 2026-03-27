package com.obill.app.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenReceipt: (Int) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val _unusedOnOpenReceipt = onOpenReceipt
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(1)
    }

    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            val currentPage = state.envelope?.pagination?.page ?: 1
            viewModel.load(currentPage)
        },
    )
    LaunchedEffect(state.loading) {
        if (!state.loading) refreshing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
        Text("Riwayat transaksi", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
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
                        val price = row.sellingPrice?.toDoubleOrNull() ?: 0.0
                        val priceLabel = formatRupiahPlain(price).replace("Rp ", "Rp. ")
                        val transactionNo = buildTransactionNo(row.id, row.tanggal)
                        ObillCard(
                            modifier = Modifier.clickable {
                                row.id.toIntOrNull()?.let { viewModel.openReceiptPreview(it) }
                            },
                        ) {
                            HistoryDetailRow("No. Transaksi", transactionNo)
                            HistoryDetailRow("Pembeli", row.buyer.orEmpty().ifBlank { "-" })
                            HistoryDetailRow("Tanggal", row.tanggal.orEmpty().ifBlank { "-" })
                            HistoryDetailRow("Harga", priceLabel)
                            HistoryDetailRow("Kode", row.kodeVoucher.orEmpty().ifBlank { "-" })
                            if (state.quotaLoading) {
                                HistoryDetailRow("Kuota", "Memuat kuota…")
                            } else if (q != null) {
                                HistoryDetailRow("Kuota", "${q.remainingFmt.orEmpty()} / ${q.limitFmt.orEmpty()}")
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

        if (state.receiptPreviewLoading || state.receiptPreviewError != null || state.receiptPreviewText != null) {
            Dialog(onDismissRequest = { viewModel.closeReceiptPreview() }) {
                Surface(
                    color = Color(0xFFEDEDED),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 560.dp)
                            .padding(16.dp),
                    ) {
                        when {
                            state.receiptPreviewLoading -> CircularProgressIndicator()
                            state.receiptPreviewError != null -> Text(
                                state.receiptPreviewError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                            )
                            else -> Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .heightIn(max = 528.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    state.receiptPreviewText.orEmpty(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF111111),
                                )
                            }
                        }
                    }
                }
            }
        }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
        )
    }
}

@Composable
private fun HistoryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            modifier = Modifier.weight(0.58f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun buildTransactionNo(id: String, tanggal: String?): String {
    val parsed = parseTransactionDate(tanggal)
    val month = if (parsed != null) {
        java.text.SimpleDateFormat("MM", java.util.Locale.US).format(parsed)
    } else {
        java.text.SimpleDateFormat("MM", java.util.Locale.US).format(java.util.Date())
    }
    val year = if (parsed != null) {
        java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(parsed)
    } else {
        java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date())
    }
    return "${id}/TRX/${month}/${year}"
}

private fun parseTransactionDate(raw: String?): java.util.Date? {
    if (raw.isNullOrBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "dd MMM yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm",
    )
    return patterns.asSequence().mapNotNull { pattern ->
        runCatching { java.text.SimpleDateFormat(pattern, java.util.Locale("id", "ID")).parse(raw.trim()) }
            .getOrNull()
    }.firstOrNull()
}
