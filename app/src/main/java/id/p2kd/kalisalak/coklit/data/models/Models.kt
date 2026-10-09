package id.p2kd.kalisalak.coklit.data.models

import com.google.gson.annotations.SerializedName

// 1. Authentication Models
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class UserProfile(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("jabatan") val jabatan: String,
    @SerializedName("role") val role: String,
    @SerializedName("seksi") val seksi: String,
    @SerializedName("assignedTps") val assignedTps: String,
    @SerializedName("assignedRw") val assignedRw: String,
    @SerializedName("kontakWa") val kontakWa: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("isSuperAdmin") val isSuperAdmin: Boolean
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserProfile?
)

// 2. House QR Models
data class QrItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("status") val status: String,
    @SerializedName("batchRef") val batchRef: String?,
    @SerializedName("assignedTps") val assignedTps: String?,
    @SerializedName("assignedRw") val assignedRw: String?
)

data class RumahItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("nomorRumah") val nomorRumah: String?,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("rt") val rt: String,
    @SerializedName("rw") val rw: String,
    @SerializedName("desa") val desa: String,
    @SerializedName("kecamatan") val kecamatan: String,
    @SerializedName("tps") val tps: String?,
    @SerializedName("koordinatLat") val koordinatLat: Double?,
    @SerializedName("koordinatLng") val koordinatLng: Double?,
    @SerializedName("keteranganLokasi") val keteranganLokasi: String?,
    @SerializedName("statusPendataan") val statusPendataan: String,
    @SerializedName("petugasPendata") val petugasPendata: String?
)

data class KartuKeluargaItem(
    @SerializedName("id") val id: String,
    @SerializedName("rumahId") val rumahId: String?,
    @SerializedName("noKk") val noKk: String,
    @SerializedName("kepalaKeluargaNama") val kepalaKeluargaNama: String,
    @SerializedName("alamat") val alamat: String?,
    @SerializedName("rt") val rt: String?,
    @SerializedName("rw") val rw: String?,
    @SerializedName("statusKk") val statusKk: String
)

data class AnggotaKeluargaItem(
    @SerializedName("id") val id: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("noKk") val noKk: String,
    @SerializedName("namaLengkap") val namaLengkap: String,
    @SerializedName("jenisKelamin") val jenisKelamin: String,
    @SerializedName("tempatLahir") val tempatLahir: String?,
    @SerializedName("tanggalLahir") val tanggalLahir: String?,
    @SerializedName("statusPerkawinan") val statusPerkawinan: String?,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("rt") val rt: String,
    @SerializedName("rw") val rw: String,
    @SerializedName("tps") val tps: String,
    @SerializedName("tahap") val tahap: String,
    @SerializedName("statusAktif") val statusAktif: String,
    @SerializedName("verifikasiStatus") var verifikasiStatus: String,
    @SerializedName("verifikasiCatatan") var verifikasiCatatan: String?,
    @SerializedName("rumahId") val rumahId: String?,
    @SerializedName("kkId") val kkId: String?
)

data class KunjunganTerakhir(
    @SerializedName("id") val id: String,
    @SerializedName("statusKunjungan") val statusKunjungan: String,
    @SerializedName("petugasNama") val petugasNama: String,
    @SerializedName("waktuKunjungan") val waktuKunjungan: String,
    @SerializedName("namaStikerManual") val namaStikerManual: String?
)

data class QrLookupResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("qr") val qr: QrItem?,
    @SerializedName("rumah") val rumah: RumahItem?,
    @SerializedName("kks") val kks: List<KartuKeluargaItem>?,
    @SerializedName("members") val members: List<AnggotaKeluargaItem>?,
    @SerializedName("kunjunganTerakhir") val kunjunganTerakhir: KunjunganTerakhir?
)

// 3. Register House Request
data class RumahRequest(
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("rt") val rt: String,
    @SerializedName("rw") val rw: String,
    @SerializedName("nomorRumah") val nomorRumah: String? = null,
    @SerializedName("keteranganLokasi") val keteranganLokasi: String? = null,
    @SerializedName("koordinatLat") val koordinatLat: Double? = null,
    @SerializedName("koordinatLng") val koordinatLng: Double? = null
)

data class RumahResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("rumah") val rumah: RumahItem?
)

// 4. Link KK Request
data class LinkKkRequest(
    @SerializedName("noKk") val noKk: String,
    @SerializedName("kepalaKeluargaNama") val kepalaKeluargaNama: String,
    @SerializedName("rt") val rt: String? = null,
    @SerializedName("rw") val rw: String? = null,
    @SerializedName("alamat") val alamat: String? = null
)

data class LinkKkResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("kk") val kk: KartuKeluargaItem?
)

// 5. Submit Visit Request
data class MemberVerificationPayload(
    @SerializedName("pemilihId") val pemilihId: String,
    @SerializedName("status") val status: String,
    @SerializedName("catatan") val catatan: String? = null
)

data class SubmitVisitRequest(
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("namaStikerManual") val namaStikerManual: String? = null,
    @SerializedName("catatanKunjungan") val catatanKunjungan: String? = null,
    @SerializedName("stikerDitempel") val stikerDitempel: Boolean = true,
    @SerializedName("verifikasiAnggota") val verifikasiAnggota: List<MemberVerificationPayload>,
    @SerializedName("idempotencyKey") val idempotencyKey: String? = null
)

data class SubmitVisitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("kunjunganId") val kunjunganId: String?,
    @SerializedName("statusKunjungan") val statusKunjungan: String?
)

// 6. Task List Response
data class TaskSummary(
    @SerializedName("totalRumah") val totalRumah: Int,
    @SerializedName("selesaiRumah") val selesaiRumah: Int,
    @SerializedName("perluFollowUp") val perluFollowUp: Int,
    @SerializedName("stikerTersedia") val stikerTersedia: Int
)

data class UnassignedQrItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("batchRef") val batchRef: String?,
    @SerializedName("assignedRw") val assignedRw: String?
)

data class TaskResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("assignedRw") val assignedRw: String,
    @SerializedName("summary") val summary: TaskSummary,
    @SerializedName("rumahList") val rumahList: List<RumahItem>,
    @SerializedName("unassignedQrs") val unassignedQrs: List<UnassignedQrItem>
)

// 7. Offline Queue Models
enum class SyncState {
    SAVED_LOCAL,
    PENDING_SYNC,
    SYNCING,
    SYNCED,
    ERROR
}

data class OfflineQueueItem(
    val localId: String,
    val idempotencyKey: String,
    val qrToken: String,
    val rumahData: RumahRequest,
    val kks: List<LinkKkRequest>,
    val visitData: SubmitVisitRequest,
    var syncState: SyncState = SyncState.SAVED_LOCAL,
    var errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
