package com.obill.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuotaCheckRequest(
    val username: String,
    val password: String,
)

@Serializable
data class QuotaCheckEnvelope(
    val success: Boolean,
    val message: String? = null,
    val data: QuotaCheckData? = null,
)

@Serializable
data class QuotaCheckData(
    val username: String? = null,
    val password: String? = null,
    val profil: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("mac_address") val macAddress: String? = null,
    @SerialName("remaining_quota") val remainingQuota: QuotaRemainingQuota? = null,
    val usage: QuotaUsageInfo? = null,
    @SerialName("remaining_active_time") val remainingActiveTime: QuotaRemainingActiveTime? = null,
    @SerialName("package") val packageInfo: QuotaPackageInfo? = null,
)

@Serializable
data class QuotaRemainingQuota(
    val bytes: Long? = null,
    val formatted: String? = null,
)

@Serializable
data class QuotaUsageInfo(
    @SerialName("bytes_in") val bytesIn: Long? = null,
    @SerialName("bytes_out") val bytesOut: Long? = null,
    val formatted: String? = null,
)

@Serializable
data class QuotaRemainingActiveTime(
    val seconds: Long? = null,
    val formatted: String? = null,
)

@Serializable
data class QuotaPackageInfo(
    @SerialName("sale_tanggal") val saleTanggal: String? = null,
    @SerialName("sale_tanggal_formatted") val saleTanggalFormatted: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("expires_formatted") val expiresFormatted: String? = null,
)
