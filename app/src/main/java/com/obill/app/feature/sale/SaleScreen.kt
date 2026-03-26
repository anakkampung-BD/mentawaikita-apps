package com.obill.app.feature.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.components.ObillGradientButton

private const val WIZ_STEPS = 4

@Composable
fun SaleScreen(viewModel: SaleViewModel) {
    val state by viewModel.state.collectAsState()

    var step by rememberSaveable { mutableStateOf(1) } // 1..4
    var selectedPaketId by remember { mutableStateOf<Int?>(null) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var noWa by remember { mutableStateOf("") }
    var voucher by remember { mutableStateOf<String?>(null) }
    var step3Error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    fun goTo(newStep: Int) {
        step = newStep.coerceIn(1, WIZ_STEPS)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Wizard Jual Paket", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Langkah $step dari $WIZ_STEPS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
                return
            }
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                return
            }
        }

        when (step) {
            1 -> {
                Text("1. Pilih paket", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                state.paket.forEach { p ->
                    val id = p.id.toIntOrNull()
                    if (id == null) return@forEach
                    ObillCard(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { selectedPaketId = id },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = selectedPaketId == id,
                                onClick = { selectedPaketId = id },
                            )
                            Column {
                                Text(p.namaBandwidth.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                Text("Rp ${p.sellingPrice.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                ObillGradientButton(
                    text = "Lanjut",
                    enabled = selectedPaketId != null,
                    onClick = {
                        step3Error = null
                        viewModel.clearSubmitMessage()
                        selectedDeviceId = null
                        voucher = null
                        goTo(2)
                    },
                )
            }
            2 -> {
                Text("2. Tampil pilihan router / lokasi", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                state.devices.forEach { d ->
                    ObillCard(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { selectedDeviceId = d.deviceId },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = selectedDeviceId == d.deviceId,
                                onClick = { selectedDeviceId = d.deviceId },
                            )
                            Column {
                                Text(d.deviceId, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = listOfNotNull(d.host, d.port).joinToString(" : "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                ObillGradientButton(
                    text = "Lanjut",
                    enabled = selectedDeviceId != null,
                    onClick = {
                        step3Error = null
                        viewModel.clearSubmitMessage()
                        voucher = null
                        goTo(3)
                    },
                )
            }
            3 -> {
                Text("3. Input No. WA Pembeli", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = noWa,
                    onValueChange = { noWa = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("No. WA Pembeli") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                if (step3Error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(step3Error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
                ObillGradientButton(
                    text = "Lanjut",
                    enabled = !state.submitLoading && selectedPaketId != null && selectedDeviceId != null,
                    onClick = {
                        step3Error = null
                        val waDigits = noWa.filter(Char::isDigit)
                        if (waDigits.length < 10) {
                            step3Error = "Nomor WA minimal 10 digit."
                            return@ObillGradientButton
                        }

                        // Normalisasi nomor WA agar request sesuai format API.
                        noWa = waDigits
                        // Voucher otomatis disiapkan saat masuk step 4.
                        voucher = generateKodeVoucher4()
                        viewModel.clearSubmitMessage()
                        goTo(4)
                    },
                    fillWidth = false,
                )
            }
            4 -> {
                Text("4. Voucher & Proses Transaksi", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))

                if (state.submitLoading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Memproses transaksi…", style = MaterialTheme.typography.bodySmall)
                } else {
                    val v = voucher
                    if (v == null) {
                        Text(
                            "Voucher belum dibuat. Kembali ke langkah 3.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(12.dp))
                        ObillGradientButton(
                            text = "Kembali",
                            onClick = { goTo(3) },
                            fillWidth = false,
                        )
                    } else {
                        Text(
                            "Kode voucher: $v",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(12.dp))

                        // Jika sudah pernah submit, tampilkan bukti atau alasan gagal; tombol proses hanya tampil sebelum submit terjadi.
                        if (state.submitError == null && state.submitSuccessMessage == null && state.saleId == null) {
                            ObillGradientButton(
                                text = "Proses Transaksi",
                                enabled = selectedPaketId != null && selectedDeviceId != null && noWa.isNotBlank(),
                                onClick = {
                                    viewModel.clearSubmitMessage()
                                    val pid = selectedPaketId ?: return@ObillGradientButton
                                    val did = selectedDeviceId ?: return@ObillGradientButton
                                    val waDigits = noWa.filter(Char::isDigit)
                                    val voucherCode = voucher ?: return@ObillGradientButton
                                    if (waDigits.length < 10) {
                                        // Seharusnya tidak terjadi karena divalidasi di step 3,
                                        // tapi tetap aman untuk runtime.
                                        viewModel.clearSubmitMessage()
                                        step3Error = "Nomor WA minimal 10 digit."
                                        goTo(3)
                                        return@ObillGradientButton
                                    }
                                    viewModel.submit(
                                        paketId = pid,
                                        deviceId = did,
                                        noWa = waDigits,
                                        kodeVoucher = voucherCode,
                                    )
                                },
                                fillWidth = false,
                            )
                        }

                        state.submitError?.let { err ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Gagal: ${err}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        state.submitSuccessMessage?.let { msg ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        val receipt = state.receiptPreview
                        if (receipt != null && receipt.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            ObillCard {
                                Text(
                                    receipt,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.clearSubmitMessage()
                        selectedPaketId = null
                        selectedDeviceId = null
                        noWa = ""
                        voucher = null
                        step3Error = null
                        goTo(1)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Transaksi baru")
                }
            }
        }
    }
}

/**
 * Generate kode voucher 4 karakter yang berisi kombinasi alfabet + numerik.
 * (Minimal 1 huruf dan 1 digit, total panjang 4).
 */
private fun generateKodeVoucher4(): String {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val digits = "0123456789"
    val all = letters + digits

    while (true) {
        val s = buildString {
            repeat(4) { append(all[Random.nextInt(all.length)]) }
        }
        val hasLetter = s.any { it in letters }
        val hasDigit = s.any { it in digits }
        if (hasLetter && hasDigit) return s
    }
}
