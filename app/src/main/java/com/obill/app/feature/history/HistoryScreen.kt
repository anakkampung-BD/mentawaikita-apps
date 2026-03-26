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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                        ObillCard(
                            modifier = Modifier.clickable {
                                row.id.toIntOrNull()?.let { viewModel.openReceiptPreview(it) }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "No. ${row.id}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    row.buyer.orEmpty().ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    textAlign = TextAlign.End,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    row.tanggal.orEmpty().ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    maxLines = 2,
                                )
                                Text(
                                    priceLabel,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                                Text(
                                    row.kodeVoucher.orEmpty().ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (state.quotaLoading) {
                                Spacer(Modifier.height(6.dp))
                                Text("Memuat kuota…", style = MaterialTheme.typography.bodySmall)
                            } else if (q != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Kuota: ${q.remainingFmt.orEmpty()} / ${q.limitFmt.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
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
}
