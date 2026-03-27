package com.obill.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val user: LoginUserDto? = null,
    val message: String? = null,
)

@Serializable
data class LoginUserDto(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("deviceId") val deviceId: String? = null,
    val saldo: Double? = null,
)

@Serializable
data class ApiMessageResponse(
    val success: Boolean? = null,
    val message: String? = null,
)

@Serializable
data class DashboardEnvelope(
    val success: Boolean,
    val data: DashboardData? = null,
    val message: String? = null,
)

@Serializable
data class DashboardData(
    val tagihan: Double = 0.0,
    @SerialName("tagihan_bulan") val tagihanBulan: Double = 0.0,
    @SerialName("penjualan_bulan") val penjualanBulan: Double = 0.0,
    val komisi: Double = 0.0,
    @SerialName("komisi_bulan") val komisiBulan: Double = 0.0,
    @SerialName("count_transaksi") val countTransaksi: Int = 0,
    @SerialName("count_transaksi_bulan") val countTransaksiBulan: Int = 0,
    @SerialName("jml_penjualan_hari_ini") val jmlPenjualanHariIni: Int = 0,
    @SerialName("hpp_hari_ini") val hppHariIni: Double = 0.0,
    @SerialName("komisi_hari_ini") val komisiHariIni: Double = 0.0,
    val penjualan: List<PenjualanItemDto> = emptyList(),
)

@Serializable
data class PenjualanItemDto(
    val id: String,
    val buyer: String? = null,
    @SerialName("kode_voucher") val kodeVoucher: String? = null,
    val reporter: String? = null,
    val profil: String? = null,
    val tanggal: String? = null,
    @SerialName("selling_price") val sellingPrice: String? = null,
    val komisi: String? = null,
    val hpp: String? = null,
)

@Serializable
data class PaketEnvelope(
    val success: Boolean,
    val data: PaketData? = null,
    val message: String? = null,
)

@Serializable
data class PaketData(
    val paket: List<PaketItemDto> = emptyList(),
    val harga: List<HargaItemDto> = emptyList(),
)

@Serializable
data class PaketItemDto(
    val id: String,
    @SerialName("nama_bandwidth") val namaBandwidth: String? = null,
    @SerialName("time_limit") val timeLimit: String? = null,
    @SerialName("data_limit") val dataLimit: String? = null,
    @SerialName("selling_price") val sellingPrice: String? = null,
    val hpp: String? = null,
    @SerialName("is_sync") val isSync: String? = null,
)

@Serializable
data class HargaItemDto(
    val id: String,
    @SerialName("kode_profil") val kodeProfil: String? = null,
    val harga: String? = null,
)

@Serializable
data class DevicesEnvelope(
    val success: Boolean = true,
    val data: List<DeviceDto> = emptyList(),
    val message: String? = null,
)

@Serializable
data class DeviceDto(
    val id: String,
    @SerialName("deviceId") val deviceId: String,
    val host: String? = null,
    val user: String? = null,
    val pass: String? = null,
    val port: String? = null,
    @SerialName("is_remove") val isRemove: String? = null,
)

@Serializable
data class SubmitSaleRequest(
    @SerialName("paket_id") val paketId: Int,
    @SerialName("device_id") val deviceId: String,
    @SerialName("no_wa") val noWa: String,
    @SerialName("kode_voucher") val kodeVoucher: String,
)

@Serializable
data class SubmitSaleResponse(
    val success: Boolean,
    val message: String? = null,
    @SerialName("sale_id") val saleId: Int? = null,
    @SerialName("receipt_preview") val receiptPreview: String? = null,
)

@Serializable
data class HistoryEnvelope(
    val success: Boolean,
    val data: List<HistoryItemDto> = emptyList(),
    val pagination: HistoryPagination? = null,
    val message: String? = null,
)

@Serializable
data class HistoryPagination(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 50,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class HistoryItemDto(
    val id: String,
    val buyer: String? = null,
    @SerialName("kode_voucher") val kodeVoucher: String? = null,
    val reporter: String? = null,
    val profil: String? = null,
    val tanggal: String? = null,
    @SerialName("selling_price") val sellingPrice: String? = null,
    val komisi: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
data class ReceiptEnvelope(
    val success: Boolean,
    val data: ReceiptData? = null,
    val message: String? = null,
)

@Serializable
data class ReceiptData(
    val sale: JsonObject? = null,
    @SerialName("receipt_preview") val receiptPreview: String? = null,
    @SerialName("receipt_url") val receiptUrl: String? = null,
)

@Serializable
data class ResendReceiptRequest(
    val id: Int,
)

@Serializable
data class LaporanEnvelope(
    val success: Boolean,
    val data: LaporanData? = null,
    val message: String? = null,
)

@Serializable
data class LaporanData(
    @SerialName("date_dari") val dateDari: String? = null,
    @SerialName("date_sampai") val dateSampai: String? = null,
    @SerialName("jml_penjualan") val jmlPenjualan: Int = 0,
    @SerialName("total_tagihan") val totalTagihan: Double = 0.0,
    @SerialName("total_komisi") val totalKomisi: Double = 0.0,
)

@Serializable
data class ProfileEnvelope(
    val success: Boolean,
    val data: ProfileData? = null,
    val message: String? = null,
)

@Serializable
data class ProfileData(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("deviceId") val deviceId: String? = null,
    val hp: String? = null,
    val saldo: Double? = null,
    @SerialName("date_created") val dateCreated: String? = null,
)

@Serializable
data class SaldoTopupHistoryEnvelope(
    val success: Boolean,
    val data: SaldoTopupHistoryData? = null,
    val message: String? = null,
)

@Serializable
data class SaldoTopupHistoryData(
    @SerialName("user_id") val userId: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
    @SerialName("total_amount") val totalAmount: Long? = null,
    val items: List<SaldoTopupItemDto> = emptyList(),
)

@Serializable
data class SaldoTopupItemDto(
    val id: Int,
    @SerialName("user_id") val userId: Int? = null,
    val amount: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: Int? = null,
    @SerialName("created_by_name") val createdByName: String? = null,
)

@Serializable
data class HistoryQuotaRequest(
    val ids: List<Int>,
)

@Serializable
data class RemoveExpiredRequest(
    @SerialName("sale_id") val saleId: Int,
)

@Serializable
data class QuotaInfoDto(
    val remaining: Long? = null,
    val limit: Long? = null,
    @SerialName("remaining_fmt") val remainingFmt: String? = null,
    @SerialName("limit_fmt") val limitFmt: String? = null,
    val valid: Boolean? = null,
)
