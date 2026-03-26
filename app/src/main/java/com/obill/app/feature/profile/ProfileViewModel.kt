package com.obill.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.remote.ProfileData
import com.obill.app.data.repository.AuthRepository
import com.obill.app.data.repository.SellerRepository
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
)

class ProfileViewModel(
    private val sellerRepository: SellerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            sellerRepository.profile().fold(
                onSuccess = { d -> _state.update { it.copy(loading = false, data = d) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Gagal")
                    }
                },
            )
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
