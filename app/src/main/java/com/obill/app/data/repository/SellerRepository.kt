package com.obill.app.data.repository

import com.obill.app.data.remote.ApiMessageResponse
import com.obill.app.data.remote.DashboardData
import com.obill.app.data.remote.DeviceDto
import com.obill.app.data.remote.HistoryEnvelope
import com.obill.app.data.remote.HistoryQuotaRequest
import com.obill.app.data.remote.LaporanData
import com.obill.app.data.remote.PaketData
import com.obill.app.data.remote.ProfileData
import com.obill.app.data.remote.QuotaInfoDto
import com.obill.app.data.remote.ReceiptData
import com.obill.app.data.remote.RemoveExpiredRequest
import com.obill.app.data.remote.ResendReceiptRequest
import com.obill.app.data.remote.SaldoTopupHistoryEnvelope
import com.obill.app.data.remote.SellerApi
import com.obill.app.data.remote.SubmitSaleRequest
import com.obill.app.data.remote.SubmitSaleResponse
import com.obill.app.data.remote.isPdfContent
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class SellerRepository(
    private val api: SellerApi,
    private val json: Json,
    /** Field `receipt_url` dari GET api/seller/receipt — sumber utama unduh PDF (API_SELLER.md). */
    private val downloadReceiptPdfFromReceiptUrl: suspend (String, File) -> Result<File>,
    /** Hanya dipakai jika unduh dari receipt_url tidak menghasilkan file PDF valid. */
    private val downloadReceiptPdfBySaleId: suspend (Int, File) -> Result<File>,
) {

    suspend fun dashboard(): Result<DashboardData> = runCatching {
        val res = api.dashboard()
        if (!res.success) error(res.message ?: "Gagal memuat dashboard")
        res.data ?: error("Data kosong")
    }

    suspend fun paket(): Result<PaketData> = runCatching {
        val res = api.paket()
        if (!res.success) error(res.message ?: "Gagal memuat paket")
        res.data ?: error("Data kosong")
    }

    suspend fun devices(): Result<List<DeviceDto>> = runCatching {
        val res = api.devices()
        if (!res.success) error(res.message ?: "Gagal memuat perangkat")
        res.data
    }

    suspend fun submitSale(
        paketId: Int,
        deviceId: String,
        noWa: String,
        kodeVoucher: String,
    ): Result<SubmitSaleResponse> = runCatching {
        api.submitSale(
            SubmitSaleRequest(
                paketId = paketId,
                deviceId = deviceId,
                noWa = noWa,
                kodeVoucher = kodeVoucher,
            ),
        ).also {
            if (!it.success) error(it.message ?: "Transaksi gagal")
        }
    }

    suspend fun history(page: Int, perPage: Int): Result<HistoryEnvelope> = runCatching {
        val res = api.history(page = page, perPage = perPage)
        if (!res.success) error(res.message ?: "Gagal memuat riwayat")
        res
    }

    suspend fun receipt(id: Int): Result<ReceiptData> = runCatching {
        val res = api.receipt(id)
        if (!res.success) error(res.message ?: "Struk tidak ditemukan")
        res.data ?: error("Data kosong")
    }

    suspend fun receiptPdfToFile(id: Int, outputFile: File): Result<File> = runCatching {
        val res = api.receipt(id)
        if (!res.success) error(res.message ?: "Struk tidak ditemukan")
        val data = res.data ?: error("Data kosong")
        val url = data.receiptUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            error("receipt_url tidak tersedia dari API. Pastikan GET api/seller/receipt mengembalikan receipt_url.")
        }

        // Dokumentasi: PDF diunduh dari URL pada field receipt_url (bukan endpoint lain).
        val fromReceiptUrl = downloadReceiptPdfFromReceiptUrl(url, outputFile)
        if (fromReceiptUrl.isSuccess && outputFile.isPdfContent()) {
            return@runCatching fromReceiptUrl.getOrThrow()
        }
        outputFile.delete()

        // Cadangan jika respons bukan PDF (mis. HTML) — tetap coba endpoint API dengan Bearer.
        downloadReceiptPdfBySaleId(id, outputFile).getOrThrow()
    }

    suspend fun resendReceipt(id: Int): Result<ApiMessageResponse> = runCatching {
        val r = api.resendReceipt(ResendReceiptRequest(id))
        if (r.success != true) error(r.message ?: "Gagal kirim ulang")
        r
    }

    suspend fun laporan(dateDari: String, dateSampai: String): Result<LaporanData> = runCatching {
        val res = api.laporan(dateDari, dateSampai)
        if (!res.success) error(res.message ?: "Gagal memuat laporan")
        res.data ?: error("Data kosong")
    }

    suspend fun laporanPdfToFile(
        dateDari: String,
        dateSampai: String,
        outputFile: File,
    ): Result<File> = runCatching {
        val body: ResponseBody = api.laporanPdf(dateDari, dateSampai)
        saveResponseBodyToFile(body, outputFile)
        outputFile
    }

    suspend fun historyQuota(ids: List<Int>): Result<Map<String, QuotaInfoDto>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyMap()
        val raw = api.historyQuota(HistoryQuotaRequest(ids = ids)).string()
        json.decodeFromString<Map<String, QuotaInfoDto>>(raw)
    }

    suspend fun removeExpired(saleId: Int): Result<ApiMessageResponse> = runCatching {
        api.removeExpired(RemoveExpiredRequest(saleId)).also {
            if (it.success != true) error(it.message ?: "Gagal menghapus user")
        }
    }

    suspend fun profile(): Result<ProfileData> = runCatching {
        val res = api.profile()
        if (!res.success) error(res.message ?: "Gagal memuat profil")
        res.data ?: error("Data kosong")
    }

    /**
     * Riwayat top-up saldo (default bulan & tahun berjalan). [userId] opsional; jika null, server pakai id dari token.
     */
    suspend fun saldoTopupHistory(
        userId: Int? = null,
        month: Int? = null,
        year: Int? = null,
    ): Result<SaldoTopupHistoryEnvelope> = runCatching {
        val res = api.saldoTopupHistory(userId = userId, month = month, year = year)
        if (!res.success) error(res.message ?: "Gagal memuat riwayat top-up saldo")
        res
    }

    private fun saveResponseBodyToFile(body: ResponseBody, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            body.byteStream().use { input -> input.copyTo(out) }
        }
    }
}
