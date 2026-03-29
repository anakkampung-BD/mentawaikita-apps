package com.obill.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.repository.AuthRepository
import com.obill.app.data.repository.SellerRepository
import com.obill.app.ui.saldo.formatSaldoForLowBalanceWarning
import com.obill.app.ui.saldo.saldoNeedsLowBalanceWarning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginPostLogin {
    data object None : LoginPostLogin
    data object NavigateToMain : LoginPostLogin
    data class LowBalanceWarning(val saldoFormatted: String) : LoginPostLogin
}

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val postLogin: LoginPostLogin = LoginPostLogin.None,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, postLogin = LoginPostLogin.None)
            }
            authRepository.login(email, password).fold(
                onSuccess = {
                    sellerRepository.profile().fold(
                        onSuccess = { profile ->
                            val formatted = formatSaldoForLowBalanceWarning(profile.saldo)
                            if (saldoNeedsLowBalanceWarning(profile.saldo)) {
                                _state.update {
                                    it.copy(
                                        loading = false,
                                        postLogin = LoginPostLogin.LowBalanceWarning(formatted),
                                    )
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        loading = false,
                                        postLogin = LoginPostLogin.NavigateToMain,
                                    )
                                }
                            }
                        },
                        onFailure = {
                            _state.update {
                                it.copy(
                                    loading = false,
                                    postLogin = LoginPostLogin.NavigateToMain,
                                )
                            }
                        },
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Login gagal")
                    }
                },
            )
        }
    }

    fun resetPostLogin() {
        _state.update { it.copy(postLogin = LoginPostLogin.None) }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            sellerRepository: SellerRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LoginViewModel(authRepository, sellerRepository) as T
        }
    }
}
