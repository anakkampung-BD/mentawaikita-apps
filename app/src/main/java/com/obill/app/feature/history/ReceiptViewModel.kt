package com.obill.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.ReceiptData
import com.obill.app.data.repository.SellerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: ReceiptData? = null,
    val resendLoading: Boolean = false,
    val resendMessage: String? = null,
)

class ReceiptViewModel(
    private val sellerRepository: SellerRepository,
    private val saleId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            sellerRepository.receipt(saleId).fold(
                onSuccess = { d -> _state.update { it.copy(loading = false, data = d) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Gagal")
                    }
                },
            )
        }
    }

    fun resend() {
        viewModelScope.launch {
            _state.update { it.copy(resendLoading = true, resendMessage = null) }
            sellerRepository.resendReceipt(saleId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(resendLoading = false, resendMessage = "Bukti dikirim ulang.")
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            resendLoading = false,
                            resendMessage = e.message ?: "Gagal kirim",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(repo: SellerRepository, saleId: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReceiptViewModel(repo, saleId) as T
        }
    }
}
