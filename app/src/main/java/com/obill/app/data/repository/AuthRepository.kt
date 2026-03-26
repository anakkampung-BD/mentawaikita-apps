package com.obill.app.data.repository

import com.obill.app.TokenHolder
import com.obill.app.data.local.TokenStore
import com.obill.app.data.remote.LoginRequest
import com.obill.app.data.remote.SellerApi
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val api: SellerApi,
    private val tokenStore: TokenStore,
    private val tokenHolder: TokenHolder,
) {
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(email = email.trim(), password = password))
        if (!res.success) {
            error(res.message ?: "Login gagal")
        }
        val token = res.token ?: error("Token tidak ada")
        tokenStore.setToken(token)
        tokenHolder.token = token
    }

    suspend fun logout(): Result<Unit> = runCatching {
        runCatching { api.logout() }
        tokenStore.clearToken()
        tokenHolder.token = null
    }

    suspend fun hydrateTokenHolder() {
        tokenHolder.token = tokenStore.token.first()
    }

    suspend fun clearSessionLocalOnly() {
        tokenStore.clearToken()
        tokenHolder.token = null
    }
}
