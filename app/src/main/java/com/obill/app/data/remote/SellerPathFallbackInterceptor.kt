package com.obill.app.data.remote

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * Jika routing server memakai `api_seller/` (bukan `api/seller/`), ulang request:
 * - respons HTML / error halaman, atau
 * - HTTP 404 (sering dari CodeIgniter jika route `api/seller/...` tidak terpasang).
 */
class SellerPathFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val bodyBytes = original.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        }
        val contentType = original.body?.contentType()

        fun buildFor(url: okhttp3.HttpUrl): Request {
            val rb = original.newBuilder().url(url)
            val newBody = if (bodyBytes != null && contentType != null) {
                bodyBytes.toRequestBody(contentType)
            } else {
                null
            }
            return when {
                newBody != null && original.method != "GET" && original.method != "HEAD" ->
                    rb.method(original.method, newBody).build()
                else -> rb.build()
            }
        }

        var response = chain.proceed(buildFor(original.url))
        if (!shouldRetry(response)) return response

        val newUrl = original.url.newBuilder()
            .encodedPath(
                original.url.encodedPath.replace("api/seller/", "api_seller/"),
            )
            .build()

        if (newUrl == original.url) {
            return response
        }
        response.close()
        return chain.proceed(buildFor(newUrl))
    }

    private fun shouldRetry(response: Response): Boolean {
        val path = response.request.url.encodedPath
        if (path.contains("api/seller/", ignoreCase = true) && response.code == 404) {
            return true
        }
        val ct = response.header("Content-Type").orEmpty()
        if (ct.contains("text/html", ignoreCase = true)) return true
        return try {
            val peek = response.peekBody(96)
            val prefix = peek.string().trimStart()
            prefix.startsWith("<!") || prefix.startsWith("<html", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
