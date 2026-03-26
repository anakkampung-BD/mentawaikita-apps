package com.obill.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.obill.app.TokenHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class NetworkModule(
    baseUrl: String,
    tokenHolder: TokenHolder,
) {
    private val baseUrlNormalized: String = baseUrl.ensureTrailingSlash()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SellerPathFallbackInterceptor())
        .addInterceptor(AuthInterceptor(tokenHolder))
        .addInterceptor(createLoggingInterceptor())
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrlNormalized)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: SellerApi = retrofit.create(SellerApi::class.java)

    /**
     * Cek koneksi ke host API (tanpa Bearer). Dipakai splash sebelum login.
     */
    private val pingClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun pingServer(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(baseUrlNormalized)
                .get()
                .build()
            pingClient.newCall(req).execute().use { }
        }
    }

    /**
     * Unduh file dari URL langsung ke [outputFile].
     * Client ini sudah punya interceptor Bearer token, jadi kalau receipt PDF butuh auth,
     * request tetap bisa berhasil.
     */
    suspend fun downloadUrlToFile(url: String, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .get()
                .build()

            outputFile.parentFile?.mkdirs()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: gagal download PDF")
                }
                val body = resp.body ?: error("Response body kosong")
                FileOutputStream(outputFile).use { out ->
                    body.byteStream().use { input -> input.copyTo(out) }
                }
            }
            outputFile
        }
    }
}

private fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"
