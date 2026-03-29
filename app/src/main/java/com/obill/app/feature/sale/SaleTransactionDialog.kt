package com.obill.app.feature.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.obill.app.data.remote.DeviceDto
import com.obill.app.data.remote.PaketItemDto
import com.obill.app.feature.history.formatReceiptDotMatrix37
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.components.ObillGradientButton
import java.util.Locale
import kotlin.random.Random

private const val WIZ_STEPS = 4

@Composable
fun SaleTransactionDialog(
    viewModel: SaleViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    var step by remember { mutableIntStateOf(1) }
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

    fun resetWizard() {
        step = 1
        selectedPaketId = null
        selectedDeviceId = null
        noWa = ""
        voucher = null
        step3Error = null
        viewModel.clearSubmitMessage()
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
                .padding(horizontal = 12.dp)
                .heightIn(max = 560.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        TextButton(onClick = {
                            viewModel.clearSubmitMessage()
                            onDismiss()
                        }) {
                            Text("Tutup")
                        }
                    }
                    Text(
                        text = "Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (step in 2..4) {
                            TextButton(
                                onClick = {
                                    step3Error = null
                                    viewModel.clearSubmitMessage()
                                    when (step) {
                                        2 -> {
                                            selectedDeviceId = null
                                            goTo(1)
                                        }
                                        3 -> {
                                            voucher = null
                                            goTo(2)
                                        }
                                        4 -> {
                                            voucher = null
                                            goTo(3)
                                        }
                                        else -> Unit
                                    }
                                },
                            ) {
                                Text("Kembali")
                            }
                        }
                    }
                }

                Text(
                    text = "Langkah $step dari $WIZ_STEPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(10.dp))

                when {
                    state.loading -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator() }
                    }
                    state.error != null -> {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        when (step) {
                            1 -> StepPilihPaket(
                                paket = state.paket,
                                onSelect = { id ->
                                    selectedPaketId = id
                                    selectedDeviceId = null
                                    voucher = null
                                    step3Error = null
                                    viewModel.clearSubmitMessage()
                                    goTo(2)
                                },
                            )
                            2 -> StepPilihRouter(
                                devices = state.devices,
                                onSelect = { deviceId ->
                                    selectedDeviceId = deviceId
                                    voucher = null
                                    step3Error = null
                                    viewModel.clearSubmitMessage()
                                    goTo(3)
                                },
                            )
                            3 -> StepInputWa(
                                noWa = noWa,
                                onNoWaChange = { noWa = it },
                                step3Error = step3Error,
                                submitLoading = state.submitLoading,
                                enabled = selectedPaketId != null && selectedDeviceId != null,
                                onGenerate = {
                                    step3Error = null
                                    val waDigits = noWa.filter(Char::isDigit)
                                    if (waDigits.length < 10) {
                                        step3Error = "Nomor WA minimal 10 digit."
                                        return@StepInputWa
                                    }
                                    noWa = waDigits
                                    voucher = generateKodeVoucher4()
                                    viewModel.clearSubmitMessage()
                                    goTo(4)
                                },
                            )
                            4 -> StepRingkasan(
                                state = state,
                                paket = state.paket,
                                devices = state.devices,
                                selectedPaketId = selectedPaketId,
                                selectedDeviceId = selectedDeviceId,
                                noWa = noWa,
                                voucher = voucher,
                                onSubmit = {
                                    val pid = selectedPaketId ?: return@StepRingkasan
                                    val did = selectedDeviceId ?: return@StepRingkasan
                                    val waDigits = noWa.filter(Char::isDigit)
                                    val voucherCode = voucher ?: return@StepRingkasan
                                    if (waDigits.length < 10) {
                                        step3Error = "Nomor WA minimal 10 digit."
                                        goTo(3)
                                        return@StepRingkasan
                                    }
                                    viewModel.clearSubmitMessage()
                                    viewModel.submit(
                                        paketId = pid,
                                        deviceId = did,
                                        noWa = waDigits,
                                        kodeVoucher = voucherCode,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val receipt = state.receiptPreview
    if (receipt != null && receipt.isNotBlank()) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Surface(
                color = Color(0xFFEDEDED),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            formatReceiptDotMatrix37(receipt, state.saleId ?: 0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF111111),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.clearSubmitMessage()
                                resetWizard()
                            },
                        ) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPilihPaket(
    paket: List<PaketItemDto>,
    onSelect: (Int) -> Unit,
) {
    Text("1. Pilih paket", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    paket.forEach { p ->
        val id = p.id.toIntOrNull() ?: return@forEach
        ObillCard(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onSelect(id) },
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(p.namaBandwidth.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text("Rp ${p.sellingPrice.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StepPilihRouter(
    devices: List<DeviceDto>,
    onSelect: (String) -> Unit,
) {
    Text("2. Pilih router / lokasi", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    devices.forEach { d ->
        ObillCard(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onSelect(d.deviceId) },
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = d.deviceId,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StepInputWa(
    noWa: String,
    onNoWaChange: (String) -> Unit,
    step3Error: String?,
    submitLoading: Boolean,
    enabled: Boolean,
    onGenerate: () -> Unit,
) {
    Text("3. Nomor WhatsApp pembeli", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = noWa,
        onValueChange = onNoWaChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("No. WA Pembeli") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        enabled = !submitLoading,
    )
    step3Error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(16.dp))
    ObillGradientButton(
        text = "Generate",
        enabled = enabled && !submitLoading,
        onClick = onGenerate,
    )
}

@Composable
private fun StepRingkasan(
    state: SaleUiState,
    paket: List<PaketItemDto>,
    devices: List<DeviceDto>,
    selectedPaketId: Int?,
    selectedDeviceId: String?,
    noWa: String,
    voucher: String?,
    onSubmit: () -> Unit,
) {
    Text("4. Ringkasan transaksi", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(10.dp))

    val paketItem = paket.firstOrNull { it.id.toIntOrNull() == selectedPaketId }
    val device = devices.firstOrNull { it.deviceId == selectedDeviceId }

    ObillCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RingkasanRow("Paket", paketItem?.namaBandwidth ?: "—")
            RingkasanRow("Harga", formatHargaRingkasan(paketItem?.sellingPrice))
            RingkasanRow("Lokasi", device?.deviceId ?: "—")
            RingkasanRow("No. WA", noWa.ifBlank { "—" })
            RingkasanRow("Kode voucher", voucher ?: "—")
        }
    }

    if (state.submitLoading) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(8.dp))
        Text("Memproses transaksi…", style = MaterialTheme.typography.bodySmall)
    } else if (state.submitError == null && state.submitSuccessMessage == null && state.saleId == null && voucher != null) {
        Spacer(Modifier.height(16.dp))
        ObillGradientButton(
            text = "Proses",
            enabled = selectedPaketId != null && selectedDeviceId != null && noWa.isNotBlank(),
            onClick = onSubmit,
        )
    }

    state.submitError?.let { err ->
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Gagal: $err",
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
}

@Composable
private fun RingkasanRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 3,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 4,
        )
    }
}

/** Format harga ringkasan: `Rp. 10.000` dari string angka server. */
private fun formatHargaRingkasan(raw: String?): String {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return "—"
    val digitsOnly = s.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return "Rp. $s"
    val n = digitsOnly.toLongOrNull() ?: return "Rp. $s"
    val formatted = String.format(Locale.US, "%,d", n).replace(',', '.')
    return "Rp. $formatted"
}

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
