package com.obill.app.feature.quota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.QuotaCheckData
import com.obill.app.data.repository.QuotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuotaCheckUiState {
    data object Idle : QuotaCheckUiState()
    data object Loading : QuotaCheckUiState()
    data class Success(
        val data: QuotaCheckData,
        val serverMessage: String? = null,
    ) : QuotaCheckUiState()

    data class Error(val message: String) : QuotaCheckUiState()
}

class QuotaCheckViewModel(
    private val quotaRepository: QuotaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<QuotaCheckUiState>(QuotaCheckUiState.Idle)
    val state: StateFlow<QuotaCheckUiState> = _state.asStateFlow()

    fun reset() {
        _state.value = QuotaCheckUiState.Idle
    }

    fun check(username: String, password: String) {
        viewModelScope.launch {
            _state.value = QuotaCheckUiState.Loading
            quotaRepository.checkQuota(username, password).fold(
                onSuccess = { outcome ->
                    _state.value = QuotaCheckUiState.Success(
                        data = outcome.data,
                        serverMessage = outcome.serverMessage,
                    )
                },
                onFailure = { e ->
                    _state.value = QuotaCheckUiState.Error(e.message ?: "Gagal memeriksa kuota.")
                },
            )
        }
    }

    companion object {
        fun factory(quotaRepository: QuotaRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuotaCheckViewModel(quotaRepository) as T
        }
    }
}
