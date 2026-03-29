package com.obill.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.obill.app.data.repository.SellerRepository
import com.obill.app.ui.saldo.formatSaldoForLowBalanceWarning
import com.obill.app.ui.saldo.saldoNeedsLowBalanceWarning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionSaldoGateViewModel(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _warningFormatted = MutableStateFlow<String?>(null)
    val warningFormatted: StateFlow<String?> = _warningFormatted.asStateFlow()

    private var checkStarted = false

    fun startCheckIfNeeded(skipSessionSaldoCheck: Boolean) {
        if (skipSessionSaldoCheck || checkStarted) return
        checkStarted = true
        viewModelScope.launch {
            sellerRepository.profile().fold(
                onSuccess = { profile ->
                    if (saldoNeedsLowBalanceWarning(profile.saldo)) {
                        _warningFormatted.value = formatSaldoForLowBalanceWarning(profile.saldo)
                    }
                },
                onFailure = { },
            )
        }
    }

    fun dismissWarning() {
        _warningFormatted.value = null
    }

    companion object {
        fun factory(sellerRepository: SellerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SessionSaldoGateViewModel(sellerRepository) as T
        }
    }
}
