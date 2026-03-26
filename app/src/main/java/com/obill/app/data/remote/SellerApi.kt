package com.obill.app.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SellerApi {

    @POST("api/seller/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/seller/logout")
    suspend fun logout(): ApiMessageResponse

    @GET("api/seller/dashboard")
    suspend fun dashboard(): DashboardEnvelope

    @GET("api/seller/paket")
    suspend fun paket(): PaketEnvelope

    @GET("api/seller/devices")
    suspend fun devices(): DevicesEnvelope

    @POST("api/seller/submit_sale")
    suspend fun submitSale(@Body body: SubmitSaleRequest): SubmitSaleResponse

    @GET("api/seller/history")
    suspend fun history(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): HistoryEnvelope

    @GET("api/seller/receipt")
    suspend fun receipt(@Query("id") id: Int): ReceiptEnvelope

    @POST("api/seller/resend_receipt")
    suspend fun resendReceipt(@Body body: ResendReceiptRequest): ApiMessageResponse

    @GET("api/seller/laporan")
    suspend fun laporan(
        @Query("date_dari") dateDari: String,
        @Query("date_sampai") dateSampai: String,
    ): LaporanEnvelope

    @Streaming
    @GET("api/seller/laporan_pdf")
    suspend fun laporanPdf(
        @Query("date_dari") dateDari: String,
        @Query("date_sampai") dateSampai: String,
    ): ResponseBody

    @POST("api/seller/history_quota")
    suspend fun historyQuota(@Body body: HistoryQuotaRequest): ResponseBody

    @POST("api/seller/remove_expired_user")
    suspend fun removeExpired(@Body body: RemoveExpiredRequest): ApiMessageResponse

    @GET("api/seller/profile")
    suspend fun profile(): ProfileEnvelope
}
