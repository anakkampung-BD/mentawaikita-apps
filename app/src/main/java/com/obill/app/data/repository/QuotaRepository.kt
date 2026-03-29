package com.obill.app.data.repository

import com.obill.app.data.remote.QuotaApi
import com.obill.app.data.remote.QuotaCheckData
import com.obill.app.data.remote.QuotaCheckEnvelope
import com.obill.app.data.remote.QuotaCheckRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response

data class QuotaCheckOutcome(
    val data: QuotaCheckData,
    /** Pesan lengkap dari server (jika ada), mis. teks berformat WhatsApp. */
    val serverMessage: String? = null,
)

class QuotaRepository(
    private val api: QuotaApi,
    private val json: Json,
) {

    suspend fun checkQuota(username: String, password: String): Result<QuotaCheckOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val u = username.trim()
                val p = password.trim()
                if (u.isEmpty() || p.isEmpty()) {
                    error("Parameter username dan password wajib diisi.")
                }
                val response = api.check(QuotaCheckRequest(username = u, password = p))
                val envelope = parseEnvelope(response)
                if (!envelope.success || envelope.data == null) {
                    error(envelope.message ?: "Gagal memuat sisa kuota.")
                }
                QuotaCheckOutcome(
                    data = envelope.data,
                    serverMessage = envelope.message,
                )
            }
        }

    private fun parseEnvelope(response: Response<QuotaCheckEnvelope>): QuotaCheckEnvelope {
        response.body()?.let { return it }
        val raw = response.errorBody()?.string().orEmpty()
        if (raw.isNotBlank()) {
            return try {
                json.decodeFromString<QuotaCheckEnvelope>(raw)
            } catch (_: Exception) {
                QuotaCheckEnvelope(success = false, message = raw)
            }
        }
        return QuotaCheckEnvelope(
            success = false,
            message = "HTTP ${response.code()}",
        )
    }
}
