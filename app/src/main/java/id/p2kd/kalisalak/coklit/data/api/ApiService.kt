package id.p2kd.kalisalak.coklit.data.api

import id.p2kd.kalisalak.coklit.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/app/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/app/auth/me")
    suspend fun getProfile(): Response<LoginResponse>

    @POST("api/app/auth/logout")
    suspend fun logout(): Response<Map<String, Any>>

    @GET("api/app/tasks")
    suspend fun getTasks(
        @Query("rw") rw: String? = null,
        @Query("tps") tps: String? = null
    ): Response<TaskResponse>

    @GET("api/app/qr/lookup")
    suspend fun lookupQr(
        @Query("token") token: String
    ): Response<QrLookupResponse>

    @POST("api/app/rumah")
    suspend fun registerRumah(
        @Body request: RumahRequest
    ): Response<RumahResponse>

    @POST("api/app/rumah/{id}/kk")
    suspend fun linkKk(
        @Path("id") rumahId: String,
        @Body request: LinkKkRequest
    ): Response<LinkKkResponse>

    @POST("api/app/rumah/{id}/kunjungan")
    suspend fun submitVisit(
        @Path("id") rumahId: String,
        @Body request: SubmitVisitRequest
    ): Response<SubmitVisitResponse>

    @POST("api/app/sync")
    suspend fun syncBatch(
        @Body payload: BatchSyncRequest
    ): Response<BatchSyncResponse>
}
