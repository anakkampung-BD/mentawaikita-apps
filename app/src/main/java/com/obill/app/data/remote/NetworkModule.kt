package com.obill.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.obill.app.TokenHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.Request
import retrofit2.Retrofit
import java.net.URI
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
     * Ambil response text dari URL absolut atau path relatif ke base URL API.
     */
    suspend fun fetchText(
        urlOrPath: String,
        headers: Map<String, String> = emptyMap(),
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val target = if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                urlOrPath
            } else {
                baseUrlNormalized + urlOrPath.removePrefix("/")
            }
            val reqBuilder = Request.Builder()
                .url(target)
                .get()
            headers.forEach { (key, value) ->
                if (value.isNotBlank()) reqBuilder.header(key, value)
            }
            val req = reqBuilder.build()
            pingClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: gagal cek update")
                }
                resp.body?.string()?.trim().orEmpty()
            }
        }
    }

    /**
     * Unduh PDF dari [receiptUrl] — field **`receipt_url`** pada respons
     * `GET api/seller/receipt?id=...` (lihat API_SELLER.md). Ini sumber utama unduh bukti transaksi.
     * Header `Accept` / `Referer` membantu server mengembalikan body PDF, bukan halaman HTML.
     * Tetap memakai [client] yang sama (Bearer + interceptor) agar autentikasi konsisten.
     */
    suspend fun downloadReceiptPdfFromReceiptUrl(receiptUrl: String, outputFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolved = resolveReceiptDownloadUrl(receiptUrl)
                val req = Request.Builder()
                    .url(resolved)
                    .get()
                    .header("Accept", "application/pdf, application/octet-stream;q=0.9, */*;q=0.1")
                    .header("Referer", baseUrlNormalized.trimEnd('/'))
                    .build()

                outputFile.parentFile?.mkdirs()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error("HTTP ${resp.code}: gagal unduh dari receipt_url")
                    }
                    val body = resp.body ?: error("Response body kosong")
                    FileOutputStream(outputFile).use { out ->
                        body.byteStream().use { input -> input.copyTo(out) }
                    }
                }
                outputFile
            }
        }

    /**
     * Unduh file dari URL langsung ke [outputFile] (generic).
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

    /**
     * Cadangan jika unduh dari [receipt_url] gagal / bukan PDF.
     * Mencoba beberapa pola URL yang umum di server (query, path segment, api vs api_seller, pola web seller).
     */
    suspend fun downloadReceiptPdfBySaleId(id: Int, outputFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val candidates = buildReceiptPdfCandidateUrls(id)
                var lastFailure: Throwable? = null
                for (httpUrl in candidates) {
                    try {
                        client.newCall(
                            Request.Builder()
                                .url(httpUrl)
                                .get()
                                .header("Accept", "application/pdf, application/octet-stream;q=0.9, */*;q=0.1")
                                .header("Referer", baseUrlNormalized.trimEnd('/'))
                                .build(),
                        ).execute().use { resp ->
                            if (!resp.isSuccessful) {
                                lastFailure = IllegalStateException("HTTP ${resp.code} (${httpUrl.encodedPath})")
                                return@use
                            }
                            val body = resp.body ?: run {
                                lastFailure = IllegalStateException("Response body kosong")
                                return@use
                            }
                            outputFile.parentFile?.mkdirs()
                            FileOutputStream(outputFile).use { out ->
                                body.byteStream().use { input -> input.copyTo(out) }
                            }
                            if (outputFile.isPdfContent()) {
                                return@runCatching outputFile
                            }
                            val jsonMsg = outputFile.readJsonErrorMessageIfPresent()
                            outputFile.delete()
                            lastFailure = IllegalStateException(
                                jsonMsg ?: "Server tidak mengembalikan file PDF (bukan dokumen valid).",
                            )
                        }
                    } catch (e: Exception) {
                        lastFailure = e
                        outputFile.delete()
                    }
                }
                throw lastFailure ?: IllegalStateException("Gagal mengunduh PDF bukti transaksi (404 / tidak ditemukan).")
            }
        }

    /**
     * Gabungkan [baseUrlNormalized] dengan path/query; beberapa server memakai pola berbeda.
     */
    private fun buildReceiptPdfCandidateUrls(id: Int): List<HttpUrl> {
        val base = baseUrlNormalized.toHttpUrlOrNull() ?: return emptyList()
        val strings = mutableListOf<String>()
        // Query ?id= dan ?sale_id=
        for (prefix in listOf("api/seller/receipt_pdf", "api_seller/receipt_pdf")) {
            strings += "${baseUrlNormalized}$prefix?id=$id"
            strings += "${baseUrlNormalized}$prefix?sale_id=$id"
        }
        // Path segment .../receipt_pdf/{id}
        for (prefix in listOf("api/seller/receipt_pdf", "api_seller/receipt_pdf")) {
            strings += "${baseUrlNormalized}$prefix/$id"
        }
        // Dokumentasi: .../seller/receipt_pdf/{id} (bukan di bawah api/)
        strings += HttpUrl.Builder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .addPathSegment("seller")
            .addPathSegment("receipt_pdf")
            .addPathSegment(id.toString())
            .build()
            .toString()

        return strings.mapNotNull { it.toHttpUrlOrNull() }.distinctBy { it.toString() }
    }

    /**
     * Jika API mengembalikan path relatif (mis. `/seller/receipt_pdf/123`), resolve terhadap host API.
     */
    private fun resolveReceiptDownloadUrl(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            return t
        }
        return try {
            URI(baseUrlNormalized).resolve(t).normalize().toASCIIString()
        } catch (_: Exception) {
            baseUrlNormalized.trimEnd('/') + "/" + t.trimStart('/')
        }
    }
}

private fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"
