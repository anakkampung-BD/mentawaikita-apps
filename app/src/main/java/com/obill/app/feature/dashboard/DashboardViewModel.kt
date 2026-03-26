package com.obill.app.feature.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.obill.app.data.remote.DashboardData
import com.obill.app.data.remote.ProfileData
import com.obill.app.data.repository.SellerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: DashboardData? = null,
    val lastSyncAtMillis: Long? = null,
    val sellerProfile: ProfileData? = null,
    val receiptLoading: Boolean = false,
    val receiptError: String? = null,
    val receiptUri: Uri? = null,
)

class DashboardViewModel(
    private val sellerRepository: SellerRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            sellerRepository.dashboard().fold(
                onSuccess = { d ->
                    _state.update {
                        it.copy(
                            loading = false,
                            data = d,
                            lastSyncAtMillis = System.currentTimeMillis(),
                        )
                    }

                    // Muat profil seller untuk ditampilkan di header dashboard.
                    sellerRepository.profile().fold(
                        onSuccess = { profile ->
                            _state.update { it.copy(sellerProfile = profile) }
                        },
                        onFailure = {
                            // Jika gagal, tetap tampilkan dashboard utama.
                            _state.update { it.copy(sellerProfile = null) }
                        },
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Gagal memuat",
                        )
                    }
                },
            )
        }
    }

    fun downloadReceiptPdf(saleId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(receiptLoading = true, receiptError = null, receiptUri = null) }
            val cacheDir = File(appContext.cacheDir, "receipts")
            cacheDir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "receipt-$saleId-$stamp.pdf")

            sellerRepository.receiptPdfToFile(saleId, file).fold(
                onSuccess = { f ->
                    val uri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        f,
                    )
                    _state.update { it.copy(receiptLoading = false, receiptUri = uri) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            receiptLoading = false,
                            receiptError = e.message ?: "Gagal mengunduh bukti transaksi",
                        )
                    }
                },
            )
        }
    }

    fun clearReceiptUri() {
        _state.update { it.copy(receiptUri = null) }
    }

    fun setReceiptError(message: String) {
        _state.update { it.copy(receiptError = message, receiptLoading = false) }
    }

    companion object {
        fun factory(repo: SellerRepository, context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(repo, context.applicationContext) as T
        }
    }
}
