package com.obill.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.ProfileData
import com.obill.app.data.remote.SaldoTopupHistoryData
import com.obill.app.data.repository.AuthRepository
import com.obill.app.data.repository.SellerRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: ProfileData? = null,
    val logoutLoading: Boolean = false,
    val topupLoading: Boolean = true,
    val topupError: String? = null,
    val topupHistory: SaldoTopupHistoryData? = null,
    /** Pesan dari envelope API (mis. tabel belum tersedia). */
    val topupApiMessage: String? = null,
)

class ProfileViewModel(
    private val sellerRepository: SellerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    topupLoading = true,
                    topupError = null,
                    topupHistory = null,
                    topupApiMessage = null,
                )
            }
            coroutineScope {
                val profileDef = async { sellerRepository.profile() }
                val topupDef = async { sellerRepository.saldoTopupHistory() }
                profileDef.await().fold(
                    onSuccess = { d ->
                        _state.update { it.copy(loading = false, data = d) }
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(loading = false, error = e.message ?: "Gagal")
                        }
                    },
                )
                topupDef.await().fold(
                    onSuccess = { env ->
                        _state.update {
                            it.copy(
                                topupLoading = false,
                                topupHistory = env.data,
                                topupApiMessage = env.message,
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(
                                topupLoading = false,
                                topupError = e.message ?: "Gagal memuat riwayat top-up saldo",
                            )
                        }
                    },
                )
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(logoutLoading = true) }
            authRepository.logout()
            _state.update { it.copy(logoutLoading = false) }
            onDone()
        }
    }

    companion object {
        fun factory(
            sellerRepository: SellerRepository,
            authRepository: AuthRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(sellerRepository, authRepository) as T
        }
    }
}
