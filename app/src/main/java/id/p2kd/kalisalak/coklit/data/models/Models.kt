package id.p2kd.kalisalak.coklit.data.models

import com.google.gson.annotations.SerializedName

// 1. Authentication Models
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class UserProfile(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("nama") val nama: String = "",
    @SerializedName("jabatan") val jabatan: String = "Pantarlih",
    @SerializedName("role") val role: String = "pantarlih",
    @SerializedName("seksi") val seksi: String = "SEKSI_1",
    @SerializedName("assignedTps") val assignedTps: String = "",
    @SerializedName("assignedRw") val assignedRw: String = "",
    @SerializedName("kontakWa") val kontakWa: String? = null,
    @SerializedName("fotoUrl") val fotoUrl: String? = null,
    @SerializedName("isSuperAdmin") val isSuperAdmin: Boolean = false
) {
    val wilayah: String get() = assignedRw
    val tps: String get() = assignedTps
}

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserProfile? = null
)

// 2. House QR Models
data class QrItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("status") val status: String,
    @SerializedName("batchRef") val batchRef: String? = null,
    @SerializedName("assignedTps") val assignedTps: String? = null,
    @SerializedName("assignedRw") val assignedRw: String? = null
) {
    val token: String get() = qrToken
}

data class RumahItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String = "",
    @SerializedName("nomorRumah") val nomorRumah: String? = null,
    @SerializedName("alamat") val alamat: String = "",
    @SerializedName("rt") val rt: String = "",
    @SerializedName("rw") val rw: String = "",
    @SerializedName("desa") val desa: String = "Kalisalak",
    @SerializedName("kecamatan") val kecamatan: String = "Margasari",
    @SerializedName("tps") val tps: String? = null,
    @SerializedName("koordinatLat") val koordinatLat: Double? = null,
    @SerializedName("koordinatLng") val koordinatLng: Double? = null,
    @SerializedName("keteranganLokasi") val keteranganLokasi: String? = null,
    @SerializedName("statusPendataan") val statusPendataan: String = "TERDAFTAR",
    @SerializedName("petugasPendata") val petugasPendata: String? = null,
    @SerializedName("kartuKeluarga") val kartuKeluarga: List<KartuKeluargaItem> = emptyList(),
    @SerializedName("qrRumah") val qrRumah: QrItem? = null
) {
    val alamat_fisik: String get() = alamat
    val dusun: String get() = desa
    val status_pendataan: String get() = statusPendataan
    val kartu_keluarga: List<KartuKeluargaItem> get() = kartuKeluarga
    val qr_rumah: QrItem? get() = qrRumah
}

typealias RumahData = RumahItem

data class KartuKeluargaItem(
    @SerializedName("id") val id: String,
    @SerializedName("rumahId") val rumahId: String? = null,
    @SerializedName("noKk") val noKk: String = "",
    @SerializedName("kepalaKeluargaNama") val kepalaKeluargaNama: String? = null,
    @SerializedName("alamat") val alamat: String? = null,
    @SerializedName("rt") val rt: String? = null,
    @SerializedName("rw") val rw: String? = null,
    @SerializedName("statusKk") val statusKk: String = "AKTIF",
    @SerializedName("anggotaCount") val anggotaCount: Int = 0,
    @SerializedName("anggota") val anggota: List<AnggotaKeluargaItem> = emptyList()
) {
    val no_kk: String get() = noKk
    val kepala_keluarga: String? get() = kepalaKeluargaNama
    val anggota_count: Int get() = anggotaCount
}

typealias KartuKeluargaData = KartuKeluargaItem

data class AnggotaKeluargaItem(
    @SerializedName("id") val id: String,
    @SerializedName("nik") val nik: String = "",
    @SerializedName("noKk") val noKk: String = "",
    @SerializedName("namaLengkap") val namaLengkap: String = "",
    @SerializedName("jenisKelamin") val jenisKelamin: String? = "L",
    @SerializedName("usia") val usia: Int? = 0,
    @SerializedName("tempatLahir") val tempatLahir: String? = null,
    @SerializedName("tanggalLahir") val tanggalLahir: String? = null,
    @SerializedName("statusPerkawinan") val statusPerkawinan: String? = null,
    @SerializedName("alamat") val alamat: String = "",
    @SerializedName("rt") val rt: String = "",
    @SerializedName("rw") val rw: String = "",
    @SerializedName("tps") val tps: String? = "",
    @SerializedName("tahap") val tahap: String = "DPS",
    @SerializedName("statusAktif") val statusAktif: String = "AKTIF",
    @SerializedName("statusKependudukan") val statusKependudukan: String? = "DPS",
    @SerializedName("verifikasiStatus") var verifikasiStatus: String? = "BELUM_DITEMUI",
    @SerializedName("verifikasiCatatan") var verifikasiCatatan: String? = null,
    @SerializedName("rumahId") val rumahId: String? = null,
    @SerializedName("kkId") val kkId: String? = null
) {
    val nama: String get() = namaLengkap
    val jenis_kelamin: String? get() = jenisKelamin
    val status_kependudukan: String? get() = statusKependudukan
    val verifikasi_status: String? get() = verifikasiStatus
    val verifikasi_catatan: String? get() = verifikasiCatatan
}

typealias AnggotaKeluargaData = AnggotaKeluargaItem

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
    @SerializedName("message") val message: String? = null,
    @SerializedName("qr") val qr: QrItem? = null,
    @SerializedName("rumah") val rumah: RumahItem? = null,
    @SerializedName("kks") val kks: List<KartuKeluargaItem>? = null,
    @SerializedName("members") val members: List<AnggotaKeluargaItem>? = null,
    @SerializedName("kunjunganTerakhir") val kunjunganTerakhir: KunjunganTerakhir? = null
) {
    val qr_rumah: QrItem get() = qr ?: QrItem(id = "", qrToken = "", status = "UNASSIGNED")
}

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
    @SerializedName("rumah") val rumah: RumahItem? = null
)

// 4. Link KK Request
data class LinkKkRequest(
    @SerializedName("noKk") val noKk: String,
    @SerializedName("kepalaKeluargaNama") val kepalaKeluargaNama: String? = null,
    @SerializedName("rt") val rt: String? = null,
    @SerializedName("rw") val rw: String? = null,
    @SerializedName("alamat") val alamat: String? = null
) {
    constructor(no_kk: String, kepala_keluarga: String?) : this(
        noKk = no_kk,
        kepalaKeluargaNama = kepala_keluarga
    )
}

data class LinkKkResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("kk") val kk: KartuKeluargaItem? = null
)

// 5. Submit Visit Request
data class MemberVerificationPayload(
    @SerializedName("pemilihId") val pemilihId: String = "",
    @SerializedName("status") val status: String = "BELUM_DITEMUI",
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("perbaikanData") val perbaikanData: Map<String, Any>? = null
) {
    val pemilih_id: String get() = pemilihId
    val perbaikan_data: Map<String, Any>? get() = perbaikanData
}

data class SubmitVisitRequest(
    @SerializedName("qrToken") val qrToken: String = "",
    @SerializedName("namaStikerManual") val namaStikerManual: String? = null,
    @SerializedName("catatanKunjungan") val catatanKunjungan: String? = null,
    @SerializedName("stikerDitempel") val stikerDitempel: Boolean = true,
    @SerializedName("verifikasiAnggota") val verifikasiAnggota: List<MemberVerificationPayload> = emptyList(),
    @SerializedName("idempotencyKey") val idempotencyKey: String? = null
) {
    val nama_stiker_manual: String? get() = namaStikerManual
    val stiker_ditempel: Boolean get() = stikerDitempel
    val verifikasi_anggota: List<MemberVerificationPayload> get() = verifikasiAnggota
    val catatan: String? get() = catatanKunjungan
    val idempotency_key: String? get() = idempotencyKey
}

typealias KunjunganCoklitRequest = SubmitVisitRequest

data class SubmitVisitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("kunjunganId") val kunjunganId: String? = null,
    @SerializedName("statusKunjungan") val statusKunjungan: String? = null
)

// 6. Task List Response
data class TaskSummary(
    @SerializedName("totalRumah") val totalRumah: Int = 0,
    @SerializedName("selesaiRumah") val selesaiRumah: Int = 0,
    @SerializedName("perluFollowUp") val perluFollowUp: Int = 0,
    @SerializedName("stikerTersedia") val stikerTersedia: Int = 0
)

data class UnassignedQrItem(
    @SerializedName("id") val id: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("batchRef") val batchRef: String?,
    @SerializedName("assignedRw") val assignedRw: String?
)

data class TaskResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("assignedRw") val assignedRw: String = "",
    @SerializedName("summary") val summary: TaskSummary = TaskSummary(),
    @SerializedName("rumahList") val rumahList: List<RumahItem> = emptyList(),
    @SerializedName("unassignedQrs") val unassignedQrs: List<UnassignedQrItem> = emptyList(),
    @SerializedName("message") val message: String? = null
) {
    val data: List<RumahItem> get() = rumahList
}

// 7. Batch Sync Request
data class BatchSyncRequest(
    @SerializedName("items") val items: List<SubmitVisitRequest>
)

data class BatchSyncResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("synced_count") val synced_count: Int = 0,
    @SerializedName("errors") val errors: List<String> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// 8. Offline Queue Models
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
) {
    val id: String get() = localId
    val rumah_id: String get() = localId
    val status: String get() = syncState.name
    val payload: SubmitVisitRequest get() = visitData
    val error_message: String? get() = errorMessage
    val created_at: Long get() = createdAt
}

typealias QueueItem = OfflineQueueItem


// 9. Voter Module Models (7 Stages)
data class VoterItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("nik") val nik: String = "",
    @SerializedName("nama") val nama: String = "",
    @SerializedName("jenisKelamin") val jenisKelamin: String? = "L",
    @SerializedName("usia") val usia: Int? = 0,
    @SerializedName("alamat") val alamat: String? = "",
    @SerializedName("rt") val rt: String? = "",
    @SerializedName("rw") val rw: String? = "",
    @SerializedName("tps") val tps: String? = "",
    @SerializedName("tahap") val tahap: String = "DPS",
    @SerializedName("status") val status: String = "AKTIF",
    @SerializedName("keterangan") val keterangan: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class VoterStageSummary(
    @SerializedName("totalDps") val totalDps: Int = 0,
    @SerializedName("totalDpt") val totalDpt: Int = 0,
    @SerializedName("totalDp4") val totalDp4: Int = 0,
    @SerializedName("totalBahanCoklit") val totalBahanCoklit: Int = 0,
    @SerializedName("totalDpsHp") val totalDpsHp: Int = 0,
    @SerializedName("totalDpsHpAkhir") val totalDpsHpAkhir: Int = 0,
    @SerializedName("totalDpsTambahan") val totalDpsTambahan: Int = 0
)

data class VoterListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("tahap") val tahap: String = "SEMUA",
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 50,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("summary") val summary: VoterStageSummary = VoterStageSummary(),
    @SerializedName("data") val data: List<VoterItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// 10. Unified Activities Hub Models
data class ActivityItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("kategori") val kategori: String = "KUNJUNGAN",
    @SerializedName("judul") val judul: String = "",
    @SerializedName("deskripsi") val deskripsi: String = "",
    @SerializedName("status") val status: String = "SELESAI",
    @SerializedName("waktu") val waktu: String = "",
    @SerializedName("aktor") val aktor: String = "",
    @SerializedName("tps") val tps: String? = null,
    @SerializedName("qrToken") val qrToken: String? = null,
    @SerializedName("metadata") val metadata: Map<String, Any>? = null
)

data class ActivitiesSummary(
    @SerializedName("totalAktivitas") val totalAktivitas: Int = 0,
    @SerializedName("totalAduan") val totalAduan: Int = 0,
    @SerializedName("totalPengumuman") val totalPengumuman: Int = 0,
    @SerializedName("tasks") val tasks: TaskSummary? = null
)

data class ActivitiesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("category") val category: String = "SEMUA",
    @SerializedName("summary") val summary: ActivitiesSummary = ActivitiesSummary(),
    @SerializedName("data") val data: List<ActivityItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// 11. Notification Center Models
data class NotificationItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("category") val category: String = "SISTEM",
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("read") val read: Boolean = false,
    @SerializedName("deepLink") val deepLink: String? = null
)

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("notifications") val notifications: List<NotificationItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// 12. FCM Device Token Models
data class DeviceTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("deviceModel") val deviceModel: String? = null
)