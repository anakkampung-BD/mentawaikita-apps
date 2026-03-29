package com.obill.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface QuotaApi {
    /** Endpoint publik: tanpa Bearer token (kredensial user hotspot di body). */
    @POST("api/quota/check")
    suspend fun check(@Body body: QuotaCheckRequest): Response<QuotaCheckEnvelope>
}
