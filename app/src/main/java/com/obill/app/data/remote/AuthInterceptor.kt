package com.obill.app.data.remote

import com.obill.app.TokenHolder
import com.obill.app.UnauthorizedBus
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenHolder: TokenHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.endsWith("/login") || path.endsWith("login")) {
            return chain.proceed(request)
        }
        val token = tokenHolder.token
        val authenticated = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        val response = chain.proceed(authenticated)
        if (response.code == 401) {
            tokenHolder.token = null
            UnauthorizedBus.emitUnauthorized()
        }
        return response
    }
}
