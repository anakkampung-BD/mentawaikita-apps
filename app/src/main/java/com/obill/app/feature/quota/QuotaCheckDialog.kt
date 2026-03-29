package com.obill.app.feature.quota

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.obill.app.data.remote.QuotaCheckData

private val WhatsAppGreen = Color(0xFF25D366)

@Composable
fun QuotaCheckDialog(
    viewModel: QuotaCheckViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    var showResultModal by remember { mutableStateOf(false) }
    var resultSnapshot by remember { mutableStateOf<QuotaCheckSuccessSnapshot?>(null) }

    LaunchedEffect(state) {
        when (val s = state) {
            is QuotaCheckUiState.Success -> {
                resultSnapshot = QuotaCheckSuccessSnapshot(s.data, s.serverMessage)
                showResultModal = true
            }
            else -> Unit
        }
    }

    val context = LocalContext.current

    fun dismissEntireFlow() {
        showResultModal = false
        resultSnapshot = null
        viewModel.reset()
        onDismiss()
    }

    fun dismissResultOnly() {
        showResultModal = false
        resultSnapshot = null
        viewModel.reset()
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
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    text = "Cek sisa kuota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Masukkan kode voucher (username) dan nomor pembeli (password) sesuai transaksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Kode voucher (username)") },
                    enabled = state !is QuotaCheckUiState.Loading,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nomor pembeli (password)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = state !is QuotaCheckUiState.Loading,
                )

                Spacer(Modifier.height(16.dp))

                when (val s = state) {
                    is QuotaCheckUiState.Idle -> { }
                    is QuotaCheckUiState.Loading -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(Modifier.padding(8.dp))
                        }
                    }
                    is QuotaCheckUiState.Error -> {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    is QuotaCheckUiState.Success -> { }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { dismissEntireFlow() },
                        enabled = state !is QuotaCheckUiState.Loading,
                    ) {
                        Text("Tutup")
                    }
                    Button(
                        onClick = { viewModel.check(username, password) },
                        enabled = state !is QuotaCheckUiState.Loading,
                    ) {
                        Text("Cek kuota")
                    }
                }
            }
        }
    }

    if (showResultModal) {
        val snap = resultSnapshot
        if (snap != null) {
            QuotaResultModal(
                snapshot = snap,
                onWhatsApp = {
                    shareQuotaViaWhatsApp(context, snap)
                },
                onClose = { dismissResultOnly() },
            )
        }
    }
}

private data class QuotaCheckSuccessSnapshot(
    val data: QuotaCheckData,
    val serverMessage: String?,
)

@Composable
private fun QuotaResultModal(
    snapshot: QuotaCheckSuccessSnapshot,
    onWhatsApp: () -> Unit,
    onClose: () -> Unit,
) {
    val scroll = rememberScrollState()
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
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    text = "Hasil cek kuota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                QuotaResultSection(
                    data = snapshot.data,
                    serverMessage = snapshot.serverMessage,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onWhatsApp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Kirim via WhatsApp")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

private fun shareQuotaViaWhatsApp(context: Context, snapshot: QuotaCheckSuccessSnapshot) {
    val text = buildQuotaShareText(snapshot.data, snapshot.serverMessage)
    val uri = Uri.parse(
        "https://api.whatsapp.com/send?text=${Uri.encode(text)}",
    )
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun buildQuotaShareText(data: QuotaCheckData, serverMessage: String?): String {
    val fromServer = serverMessage?.trim().orEmpty()
    if (fromServer.isNotEmpty()) return fromServer
    return buildString {
        appendLine("Info sisa kuota hotspot")
        data.username?.let { appendLine("User: $it") }
        data.profil?.let { appendLine("Profil: $it") }
        data.ipAddress?.let { appendLine("IP: $it") }
        data.macAddress?.let { appendLine("MAC: $it") }
        data.remainingQuota?.formatted?.let { appendLine("Sisa kuota: $it") }
        data.usage?.formatted?.let { appendLine("Penggunaan: $it") }
        data.remainingActiveTime?.formatted?.let { appendLine("Sisa masa aktif: $it") }
        data.packageInfo?.let { pkg ->
            pkg.saleTanggalFormatted?.let { appendLine("Transaksi: $it") }
            (pkg.expiresFormatted ?: pkg.expiresAt)?.let { appendLine("Berakhir: $it") }
        }
    }.trim()
}

@Composable
private fun QuotaResultSection(
    data: QuotaCheckData,
    serverMessage: String?,
) {
    val msg = serverMessage?.trim().orEmpty()
    if (msg.isNotEmpty()) {
        Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        QuotaDetailRow("User", data.username)
        QuotaDetailRow("Profil", data.profil)
        QuotaDetailRow("IP Address", data.ipAddress)
        QuotaDetailRow("MAC Address", data.macAddress)
        QuotaDetailRow("Sisa kuota", data.remainingQuota?.formatted)
        QuotaDetailRow("Penggunaan", data.usage?.formatted)
        QuotaDetailRow("Sisa masa aktif", data.remainingActiveTime?.formatted)
        data.packageInfo?.let { pkg ->
            Spacer(Modifier.height(4.dp))
            Text("Paket", style = MaterialTheme.typography.labelLarge)
            QuotaDetailRow("Tanggal transaksi", pkg.saleTanggalFormatted ?: pkg.saleTanggal)
            QuotaDetailRow("Berakhir", pkg.expiresFormatted ?: pkg.expiresAt)
        }
    }
}

@Composable
private fun QuotaDetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
