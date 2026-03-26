package com.obill.app

import android.content.Context
import com.obill.app.data.local.TokenStore
import com.obill.app.data.remote.NetworkModule
import com.obill.app.data.repository.AppUpdateRepository
import com.obill.app.data.repository.NoOpAppUpdateRepository
import com.obill.app.data.repository.AuthRepository
import com.obill.app.data.repository.SellerRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val tokenStore = TokenStore(appContext)
    val tokenHolder = TokenHolder()

    private val networkModule = NetworkModule(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenHolder = tokenHolder,
    )

    val sellerRepository = SellerRepository(
        networkModule.api,
        networkModule.json,
        downloadUrlToFile = networkModule::downloadUrlToFile,
    )
    val authRepository = AuthRepository(
        api = networkModule.api,
        tokenStore = tokenStore,
        tokenHolder = tokenHolder,
    )

    /** Cek host API dapat dijangkau (splash, tanpa login). */
    suspend fun pingServer(): Result<Unit> = networkModule.pingServer()

    /**
     * Pengecekan release aplikasi untuk update wajib.
     * Implementasi endpoint-nya akan diganti setelah API update diberikan.
     */
    val appUpdateRepository: AppUpdateRepository = NoOpAppUpdateRepository()
}
