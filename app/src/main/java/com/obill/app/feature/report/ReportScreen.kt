package com.obill.app.feature.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
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
        ContextCompat.startActivity(context, Intent.createChooser(intent, "Buka PDF"), null)
        viewModel.clearPdfUri()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Laporan penjualan", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showFrom = true }) {
                Text("Dari: $dateFromStr")
            }
            OutlinedButton(onClick = { showTo = true }) {
                Text("Sampai: $dateToStr")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.load(dateFromStr, dateToStr) },
                enabled = !state.loading,
            ) {
                Text("Muat ringkasan")
            }
            Button(
                onClick = { viewModel.downloadPdf(dateFromStr, dateToStr) },
                enabled = !state.pdfLoading,
            ) {
                Text("Unduh PDF")
            }
        }
        if (state.loading || state.pdfLoading) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.pdfError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.data?.let { d ->
            Spacer(Modifier.height(12.dp))
            ObillCard {
                Text("Jumlah penjualan: ${d.jmlPenjualan}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Total tagihan: ${formatRupiahPlain(d.totalTagihan)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Total komisi: ${formatRupiahPlain(d.totalKomisi)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
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
