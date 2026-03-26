package com.obill.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.HistoryEnvelope
import com.obill.app.data.remote.HistoryItemDto
import com.obill.app.data.remote.QuotaInfoDto
import com.obill.app.data.repository.SellerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val envelope: HistoryEnvelope? = null,
    val page: Int = 1,
    val quotaById: Map<String, QuotaInfoDto> = emptyMap(),
    val quotaLoading: Boolean = false,
    val receiptPreviewLoading: Boolean = false,
    val receiptPreviewText: String? = null,
    val receiptPreviewError: String? = null,
)

class HistoryViewModel(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun load(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, page = page) }
            sellerRepository.history(page, perPage = 20).fold(
                onSuccess = { env ->
                    _state.update { it.copy(loading = false, envelope = env) }
                    loadQuota(env.data)
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Gagal")
                    }
                },
            )
        }
    }

    private fun loadQuota(items: List<HistoryItemDto>) {
        viewModelScope.launch {
            val ids = items.mapNotNull { it.id.toIntOrNull() }
            if (ids.isEmpty()) return@launch
            _state.update { it.copy(quotaLoading = true) }
            sellerRepository.historyQuota(ids).fold(
                onSuccess = { map ->
                    _state.update { it.copy(quotaLoading = false, quotaById = map) }
                },
                onFailure = {
                    _state.update { it.copy(quotaLoading = false) }
                },
            )
        }
    }

    fun removeExpired(saleId: Int, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            sellerRepository.removeExpired(saleId).fold(
                onSuccess = {
                    onDone(null)
                    load(_state.value.page)
                },
                onFailure = { e -> onDone(e.message) },
            )
        }
    }

    fun openReceiptPreview(saleId: Int) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    receiptPreviewLoading = true,
                    receiptPreviewError = null,
                    receiptPreviewText = null,
                )
            }
            sellerRepository.receipt(saleId).fold(
                onSuccess = { data ->
                    _state.update {
                        it.copy(
                            receiptPreviewLoading = false,
                            receiptPreviewError = null,
                            receiptPreviewText = formatDotMatrix37(
                                raw = data.receiptPreview.orEmpty(),
                                saleId = saleId,
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            receiptPreviewLoading = false,
                            receiptPreviewError = e.message ?: "Gagal memuat struk",
                            receiptPreviewText = null,
                        )
                    }
                },
            )
        }
    }

    fun closeReceiptPreview() {
        _state.update {
            it.copy(
                receiptPreviewLoading = false,
                receiptPreviewError = null,
                receiptPreviewText = null,
            )
        }
    }

    companion object {
        fun factory(repo: SellerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(repo) as T
        }
    }
}

private fun formatDotMatrix37(raw: String, saleId: Int): String {
    if (raw.isBlank()) return "-"
    val width = 37
    val separator = "=".repeat((width - 3).coerceAtLeast(1))
    val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
    val parsed = parseKeyValues(normalized)
    if (parsed.isEmpty()) return wrapRaw(normalized, width)

    val note = parsed["no nota"]
        ?: parsed["invoice"]
        ?: "$saleId/TRX/${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}"
    val tanggal = parsed["tanggal"] ?: "-"
    val sales = parsed["sales"] ?: "-"
    val paket = parsed["paket"] ?: parsed["nama paket"] ?: "-"
    val total = parsed["total"]?.replace("Rp ", "Rp. ") ?: "-"
    val username = parsed["username"] ?: parsed["nama pengguna"] ?: "-"
    val password = parsed["password"] ?: parsed["kata sandi"] ?: "-"
    val berlaku = calculateExpiredFromPackage(tanggal, paket)
        ?: parsed["berlaku sampai"]
        ?: parsed["berakhir pada"]
        ?: "-"

    val out = mutableListOf<String>()
    out += centerText("@onesky.id", width)
    out += ""
    out += separator
    out += lineLR("No. Invoice", note, width)
    out += lineLR("Tanggal", tanggal, width)
    out += lineLR("Sales", sales, width)
    out += separator
    out += lineLR("Nama Paket", paket, width)
    out += lineLR("Total", total, width)
    out += separator
    out += lineLR("Nama Pengguna", username, width)
    out += lineLR("Kata Sandi", password, width)
    out += lineLR("Berakhir pada", berlaku, width)
    out += separator
    out += centerText("Terima kasih.", width)
    return out.joinToString("\n")
}

private fun parseKeyValues(raw: String): Map<String, String> {
    val map = linkedMapOf<String, String>()
    raw.split('\n').forEach { line ->
        val idx = line.indexOf(':')
        if (idx <= 0 || idx >= line.length - 1) return@forEach
        val key = line.substring(0, idx).trim().lowercase()
        val value = line.substring(idx + 1).trim()
        if (key.isNotBlank() && value.isNotBlank()) map[key] = value
    }
    return map
}

private fun wrapRaw(raw: String, width: Int): String {
    val out = mutableListOf<String>()
    raw.split('\n').forEach { original ->
        if (original.isEmpty()) {
            out += ""
            return@forEach
        }
        var remaining = original
        while (remaining.length > width) {
            out += remaining.take(width)
            remaining = remaining.drop(width)
        }
        out += remaining
    }
    return out.joinToString("\n")
}

private fun lineLR(left: String, right: String, width: Int): String {
    val l = left.trim()
    val r = right.trim()
    if (l.isEmpty()) return r.take(width)
    if (r.isEmpty()) return l.take(width)

    val minGap = 1
    var rightPart = r
    var maxLeft = width - minGap - rightPart.length

    // Jika value terlalu panjang, potong value dulu agar tetap 1 baris.
    if (maxLeft <= 0) {
        rightPart = rightPart.takeLast((width - minGap).coerceAtLeast(1))
        maxLeft = width - minGap - rightPart.length
    }

    val leftPart = if (l.length > maxLeft) l.take(maxLeft.coerceAtLeast(1)) else l
    val spaces = (width - leftPart.length - rightPart.length - 3).coerceAtLeast(minGap)
    return leftPart + " ".repeat(spaces) + rightPart
}

private fun centerText(text: String, width: Int): String {
    if (text.length >= width) return text.take(width)
    val leftPad = (width - text.length) / 2
    return " ".repeat(leftPad) + text
}

private fun calculateExpiredFromPackage(tanggal: String, paket: String): String? {
    val days = Regex("(\\d+)").find(paket)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
    if (days <= 0) return null

    val inputFormats = listOf(
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")),
    )
    val parsedDate = inputFormats.asSequence().mapNotNull { fmt ->
        runCatching { fmt.parse(tanggal.trim()) }.getOrNull()
    }.firstOrNull() ?: return null

    val cal = Calendar.getInstance().apply {
        time = parsedDate
        add(Calendar.DAY_OF_MONTH, days)
    }
    return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(cal.time)
}
