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

    // 7-Stage Voter Data Module
    @GET("api/app/pemilih")
    suspend fun getVoters(
        @Query("tahap") tahap: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 50,
        @Query("tps") tps: String? = null
    ): Response<VoterListResponse>

    // Unified Activities Hub
    @GET("api/app/activities")
    suspend fun getActivities(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int? = 50
    ): Response<ActivitiesResponse>

    // In-App Notification Center
    @GET("api/app/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    // FCM Device Token
    @POST("api/app/notifications/token")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<Map<String, Any>>

    @DELETE("api/app/notifications/token")
    suspend fun revokeDeviceToken(): Response<Map<String, Any>>


    // Voter Coklit Mutation (Cocok, TMS 8 Alasan, Ubah Data)
    @PATCH("api/app/pemilih")
    suspend fun updateVoterStatus(
        @Body request: VoterCoklitActionRequest
    ): Response<VoterCoklitActionResponse>

    // Tambah Pemilih Baru (Potensial)
    @POST("api/app/pemilih")
    suspend fun createNewVoter(
        @Body request: CreateVoterRequest
    ): Response<CreateVoterResponse>

    // Hapus Notifikasi Permanen Per-User
    @HTTP(method = "DELETE", path = "api/app/notifications", hasBody = true)
    suspend fun deleteNotification(
        @Body request: DeleteNotificationRequest
    ): Response<Map<String, Any>>

    // Ganti Kata Sandi Mandiri
    @POST("api/app/auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    // Unggah / Ganti Foto Profil
    @POST("api/app/auth/profile-photo")
    suspend fun updateProfilePhoto(
        @Body request: ProfilePhotoRequest
    ): Response<ProfilePhotoResponse>

    // App Version & In-App Update
    @GET("api/app/version")
    suspend fun checkAppVersion(
        @Header("x-app-version") currentVersion: String? = null
    ): Response<VersionCheckResponse>

    // Broadcast Announcement Banner (Pengumuman Bergambar di Beranda)
    @GET("api/app/broadcast-banner")
    suspend fun getBroadcastBanner(): Response<BroadcastBannerResponse>

}