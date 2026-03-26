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

data class HistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val envelope: HistoryEnvelope? = null,
    val page: Int = 1,
    val quotaById: Map<String, QuotaInfoDto> = emptyMap(),
    val quotaLoading: Boolean = false,
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

    companion object {
        fun factory(repo: SellerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(repo) as T
        }
    }
}
