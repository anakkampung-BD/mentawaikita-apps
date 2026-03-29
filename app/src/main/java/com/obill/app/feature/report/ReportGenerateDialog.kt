package com.obill.app.feature.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportGenerateDialog(
    viewModel: ReportViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var fromMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var toMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dateFromStr = remember(fromMillis) { fmt.format(Date(fromMillis)) }
    val dateToStr = remember(toMillis) { fmt.format(Date(toMillis)) }

    LaunchedEffect(state.pdfUri) {
        val uri = state.pdfUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Buka PDF"))
        viewModel.clearPdfUri()
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Laporan penjualan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Pilih rentang tanggal lalu unduh PDF atau lihat ringkasan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showFrom = true },
                        modifier = Modifier.weight(1f),
                        enabled = !state.loading && !state.pdfLoading,
                    ) {
                        Text(
                            "Dari: $dateFromStr",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                        )
                    }
                    OutlinedButton(
                        onClick = { showTo = true },
                        modifier = Modifier.weight(1f),
                        enabled = !state.loading && !state.pdfLoading,
                    ) {
                        Text(
                            "Sampai: $dateToStr",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                        )
                    }
                }

                if (state.loading || state.pdfLoading) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                state.pdfError?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                state.data?.let { d ->
                    Spacer(Modifier.height(12.dp))
                    ObillCard {
                        Text("Ringkasan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Jumlah penjualan: ${d.jmlPenjualan}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Total tagihan: ${formatRupiahPlain(d.totalTagihan)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !state.loading && !state.pdfLoading,
                    ) {
                        Text("Tutup")
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.downloadPdf(dateFromStr, dateToStr) },
                            enabled = !state.pdfLoading && !state.loading,
                        ) {
                            Text("Download")
                        }
                        Button(
                            onClick = { viewModel.load(dateFromStr, dateToStr) },
                            enabled = !state.loading && !state.pdfLoading,
                        ) {
                            Text("Lihat")
                        }
                    }
                }
            }
        }
    }

    if (showFrom) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fromMillis)
        DatePickerDialog(
            onDismissRequest = { showFrom = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { fromMillis = it }
                        showFrom = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFrom = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showTo) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toMillis)
        DatePickerDialog(
            onDismissRequest = { showTo = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { toMillis = it }
                        showTo = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTo = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
