package com.obill.app.feature.report

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.LaporanData
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

data class ReportUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val data: LaporanData? = null,
    val pdfLoading: Boolean = false,
    val pdfError: String? = null,
    val pdfUri: android.net.Uri? = null,
)

class ReportViewModel(
    private val sellerRepository: SellerRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun load(dateDari: String, dateSampai: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, data = null) }
            sellerRepository.laporan(dateDari, dateSampai).fold(
                onSuccess = { d -> _state.update { it.copy(loading = false, data = d) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Gagal")
                    }
                },
            )
        }
    }

    fun downloadPdf(dateDari: String, dateSampai: String) {
        viewModelScope.launch {
            _state.update { it.copy(pdfLoading = true, pdfError = null, pdfUri = null) }
            val cacheDir = File(appContext.cacheDir, "reports")
            cacheDir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "laporan-$stamp.pdf")
            sellerRepository.laporanPdfToFile(dateDari, dateSampai, file).fold(
                onSuccess = { f ->
                    val uri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        f,
                    )
                    _state.update { it.copy(pdfLoading = false, pdfUri = uri) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            pdfLoading = false,
                            pdfError = e.message ?: "Gagal unduh PDF",
                        )
                    }
                },
            )
        }
    }

    fun clearPdfUri() {
        _state.update { it.copy(pdfUri = null) }
    }

    companion object {
        fun factory(repo: SellerRepository, context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReportViewModel(repo, context.applicationContext) as T
        }
    }
}
