package com.obill.app.feature.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.DeviceDto
import com.obill.app.data.remote.PaketItemDto
import com.obill.app.data.repository.SellerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaleUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val paket: List<PaketItemDto> = emptyList(),
    val devices: List<DeviceDto> = emptyList(),
    val submitLoading: Boolean = false,
    val submitError: String? = null,
    val submitSuccessMessage: String? = null,
    val saleId: Int? = null,
    val receiptPreview: String? = null,
)

class SaleViewModel(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SaleUiState())
    val state: StateFlow<SaleUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val p = sellerRepository.paket()
            val d = sellerRepository.devices()
            if (p.isFailure) {
                _state.update {
                    it.copy(loading = false, error = p.exceptionOrNull()?.message)
                }
                return@launch
            }
            if (d.isFailure) {
                _state.update {
                    it.copy(loading = false, error = d.exceptionOrNull()?.message)
                }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    paket = p.getOrNull()?.paket.orEmpty(),
                    devices = d.getOrNull().orEmpty(),
                )
            }
        }
    }

    fun submit(
        paketId: Int,
        deviceId: String,
        noWa: String,
        kodeVoucher: String,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    submitLoading = true,
                    submitError = null,
                    submitSuccessMessage = null,
                    saleId = null,
                    receiptPreview = null,
                )
            }
            sellerRepository.submitSale(paketId, deviceId, noWa, kodeVoucher).fold(
                onSuccess = { res ->
                    _state.update {
                        it.copy(
                            submitLoading = false,
                            submitSuccessMessage = res.message ?: "Berhasil",
                            saleId = res.saleId,
                            receiptPreview = res.receiptPreview,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitLoading = false,
                            submitError = e.message ?: "Gagal",
                            saleId = null,
                            receiptPreview = null,
                        )
                    }
                },
            )
        }
    }

    fun clearSubmitMessage() {
        _state.update {
            it.copy(
                submitError = null,
                submitSuccessMessage = null,
                saleId = null,
                receiptPreview = null,
            )
        }
    }

    companion object {
        fun factory(repo: SellerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SaleViewModel(repo) as T
        }
    }
}
