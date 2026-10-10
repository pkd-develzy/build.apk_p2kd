package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager
import id.p2kd.kalisalak.coklit.data.models.*
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

val TMS_REASONS = listOf(
    "1. Meninggal Dunia",
    "2. Data Ganda",
    "3. Di Bawah Umur (< 17 th & belum kawin)",
    "4. Pindah Domisili Keluar Desa",
    "5. Tidak Dikenal / Fiktif",
    "6. Anggota TNI Aktif",
    "7. Anggota POLRI Aktif",
    "8. Hak Pilih Dicabut Pengadilan"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterDataScreen(
    onVoterClick: (VoterItem) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cacheManager = remember { LocalVoterCacheManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("SEMUA") } // SEMUA | BELUM | COCOK | TMS | RT01 | RT02 | RT03
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var voterList by remember { mutableStateOf<List<VoterItem>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }

    // State for interactive modals
    var activeActionVoter by remember { mutableStateOf<VoterItem?>(null) }
    var showDetailDialogFor by remember { mutableStateOf<VoterItem?>(null) }
    var showTmsDialogFor by remember { mutableStateOf<VoterItem?>(null) }
    var showEditDialogFor by remember { mutableStateOf<VoterItem?>(null) }
    var showPindahRwDialogFor by remember { mutableStateOf<VoterItem?>(null) }
    var showAddVoterDialog by remember { mutableStateOf(false) }
    var actionInProgress by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // NIK Unmasked tracking set
    var unmaskedNikIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun loadVoters(forceNetwork: Boolean = false) {
        val cached = cacheManager.getCachedVoters("CALON_DPS")
        if (!forceNetwork && cached != null && cached.first.isNotEmpty()) {
            voterList = cached.first
            totalCount = cached.second
            isLoading = false
        } else {
            isLoading = true
        }

        coroutineScope.launch {
            try {
                val res = ApiClient.api.getVoters(limit = 2000)
                if (res.isSuccessful && res.body()?.success == true) {
                    val rawList = res.body()?.data ?: emptyList()
                    val total = res.body()?.total ?: rawList.size
                    voterList = rawList
                    totalCount = total
                    errorMessage = null
                    cacheManager.saveVoters("CALON_DPS", rawList, total)
                } else if (voterList.isEmpty()) {
                    errorMessage = res.body()?.message ?: "Gagal memuat data dari server."
                }
            } catch (e: Exception) {
                if (voterList.isEmpty()) {
                    errorMessage = "Gagal terhubung ke server: ${e.localizedMessage}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadVoters()
    }

    fun submitCoklikAction(voter: VoterItem, action: String, alasanTms: String? = null, updates: VoterUpdatesPayload? = null) {
        coroutineScope.launch {
            actionInProgress = true
            try {
                val req = VoterCoklitActionRequest(
                    id = voter.id,
                    nik = voter.nik,
                    action = action,
                    alasanTms = alasanTms,
                    updates = updates
                )
                val res = ApiClient.api.updateVoterStatus(req)
                if (res.isSuccessful && res.body()?.success == true) {
                    snackbarMessage = "Aksi $action untuk ${voter.displayName} berhasil disimpan!"
                    // Update local state immediately
                    voterList = voterList.map { item ->
                        if (item.id == voter.id) {
                            val newRw = updates?.targetRw ?: updates?.rw ?: item.rw
                            val newRt = updates?.targetRt ?: updates?.rt ?: item.rt
                            item.copy(
                                status = if (action == "TMS") "TMS" else "AKTIF",
                                statusAktif = if (action == "TMS") "TMS" else "AKTIF",
                                coklitStatus = when (action) {
                                    "COCOK" -> "SUDAH"
                                    "PINDAH_RW" -> "PINDAH_RW"
                                    else -> action
                                },
                                alasanTms = alasanTms ?: item.alasanTms,
                                nik = updates?.nik ?: item.nik,
                                kk = updates?.noKk ?: item.kk,
                                namaLengkap = updates?.namaLengkap ?: item.namaLengkap,
                                tempatLahir = updates?.tempatLahir ?: item.tempatLahir,
                                tanggalLahir = updates?.tanggalLahir ?: item.tanggalLahir,
                                jenisKelamin = updates?.jenisKelamin ?: item.jenisKelamin,
                                statusPerkawinan = updates?.statusPerkawinan ?: item.statusPerkawinan,
                                alamat = updates?.alamat ?: item.alamat,
                                rt = newRt,
                                rw = newRw,
                                tps = if (action == "PINDAH_RW") "TPS $newRw" else item.tps,
                                disabilitas = updates?.disabilitas ?: item.disabilitas
                            )
                        } else item
                    }
                    activeActionVoter = null
                    showTmsDialogFor = null
                    showEditDialogFor = null
                    showDetailDialogFor = null
                    showPindahRwDialogFor = null
                } else {
                    snackbarMessage = res.body()?.message ?: "Gagal menyimpan aksi Coklit."
                }
            } catch (e: Exception) {
                snackbarMessage = "Kesalahan jaringan: " + (e.localizedMessage ?: "Coba lagi")
            } finally {
                actionInProgress = false
            }
        }
    }

    // Filter Logic
    val filteredVoters = remember(voterList, searchQuery, selectedFilterChip) {
        voterList.filter { voter ->
            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    voter.nik.lowercase().contains(query) ||
                    voter.noKk.lowercase().contains(query) ||
                    voter.displayName.lowercase().contains(query) ||
                    (voter.alamat ?: "").lowercase().contains(query)

            val matchesChip = when (selectedFilterChip) {
                "SEMUA" -> true
                "BELUM" -> !voter.isSudahCoklit && !voter.isTms
                "COCOK" -> voter.isSudahCoklit && !voter.isTms
                "TMS" -> voter.isTms
                "RT01" -> (voter.rt ?: "").replace("RT", "").trim().toIntOrNull() == 1
                "RT02" -> (voter.rt ?: "").replace("RT", "").trim().toIntOrNull() == 2
                "RT03" -> (voter.rt ?: "").replace("RT", "").trim().toIntOrNull() == 3
                else -> true
            }

            matchesSearch && matchesChip
        }
    }

    // ==========================================
    // 1. MODAL DETAIL PEMILIH (HANYA VIEW - READ ONLY)
    // ==========================================
    if (showDetailDialogFor != null) {
        val voter = showDetailDialogFor!!
        var copyNotice by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDetailDialogFor = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFFEFF6FF) else Color(0xFFFDF2F8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFF3B82F6) else Color(0xFFEC4899)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = voter.jenisKelamin?.take(1) ?: "L",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFF1D4ED8) else Color(0xFFBE185D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = voter.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F2042)
                        )
                        Text(
                            text = when {
                                voter.isTms -> "Status: TMS (${voter.alasanTms ?: "Tidak Memenuhi Syarat"})"
                                voter.displayStatusCoklit == "PINDAH_RW" -> "Status: MUTASI PINDAH RW"
                                voter.isSudahCoklit -> "Status: SUDAH COKLIT / SESUAI"
                                else -> "Status: BELUM COKLIT"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                voter.isTms -> Rose600
                                voter.displayStatusCoklit == "PINDAH_RW" -> Color(0xFF7C3AED)
                                voter.isSudahCoklit -> Emerald600
                                else -> Amber600
                            }
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (copyNotice != null) {
                        Surface(
                            color = Emerald500.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(copyNotice!!, fontSize = 11.sp, color = Emerald600, modifier = Modifier.padding(8.dp))
                        }
                    }

                    // Card NIK & No KK
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor Induk Kependudukan (NIK)", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(voter.nik, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(voter.nik))
                                        copyNotice = "NIK ${voter.nik} berhasil disalin!"
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin NIK", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor Kartu Keluarga (KK)", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(voter.noKk, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2042))
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(voter.noKk))
                                        copyNotice = "No KK ${voter.noKk} berhasil disalin!"
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin No KK", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Card Kelahiran & Usia Presisi
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tempat & Tanggal Lahir", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        "${voter.tempatLahir ?: "TEGAL"}, ${voter.tanggalLahir ?: "-"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F2042)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = "${voter.displayAge} Tahun",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Jenis Kelamin", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        if (voter.jenisKelamin?.startsWith("L") == true) "Laki-laki" else "Perempuan",
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F2042),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status Perkawinan", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        voter.displayStatusPerkawinan,
                                        fontSize = 12.sp,
                                        color = Amber600,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Card Wilayah Domisili
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Alamat Domisili Sah", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(voter.alamat ?: "Desa Kalisalak", fontSize = 12.sp, color = Color(0xFF0F2042), fontWeight = FontWeight.Medium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RT ${(voter.rt ?: "01").padStart(2, '0')} / RW ${(voter.rw ?: "01").padStart(2, '0')}", fontSize = 12.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                Text(voter.tps ?: "TPS 0${voter.rw ?: "1"}", fontSize = 12.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailDialogFor = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2042)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Tombol Aksi Mutasi Pindah RW
                    OutlinedButton(
                        onClick = {
                            val target = voter
                            showDetailDialogFor = null
                            showPindahRwDialogFor = target
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF7C3AED))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pindah RW", color = Color(0xFF7C3AED), fontSize = 12.sp)
                    }

                    // Tombol Ubah Data
                    OutlinedButton(
                        onClick = {
                            val target = voter
                            showDetailDialogFor = null
                            showEditDialogFor = target
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber600),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber600),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ubah", color = Amber600, fontSize = 12.sp)
                    }
                }
            },
            containerColor = Color.White
        )
    }

    // ==========================================
    // 2. MODAL AKSI "PINDAH RW" (MUTASI ANTAR-RW KALISALAK)
    // ==========================================
    if (showPindahRwDialogFor != null) {
        val voter = showPindahRwDialogFor!!
        var targetRw by remember { mutableStateOf("01") }
        var targetRt by remember { mutableStateOf("01") }
        var alasanMutasi by remember { mutableStateOf("Menikah / Pindah Keluarga") }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showPindahRwDialogFor = null },
            title = {
                Column {
                    Text("Mutasi Pindah RW (Dalam Desa)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2042))
                    Text("Pindahkan data warga yang pindah domisili antar-RW di Desa Kalisalak:", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(voter.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F2042))
                            Text("Wilayah Asal: RW ${voter.rw ?: "01"} / RT ${voter.rt ?: "01"}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Pilihan RW Tujuan
                    Column {
                        Text("Pilih RW Tujuan Baru:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2042))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("01", "02", "03").forEach { rwOpt ->
                                val isSel = targetRw == rwOpt
                                Surface(
                                    color = if (isSel) Color(0xFF0F2042) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF0F2042) else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { targetRw = rwOpt }
                                ) {
                                    Text(
                                        "RW $rwOpt",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Pilihan RT Tujuan (01, 02, 03)
                    Column {
                        Text("Pilih RT Tujuan Baru:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2042))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("01", "02", "03").forEach { rtOpt ->
                                val isSel = targetRt == rtOpt
                                Surface(
                                    color = if (isSel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { targetRt = rtOpt }
                                ) {
                                    Text(
                                        "RT $rtOpt",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Alasan Pindah RW
                    Column {
                        Text("Alasan Mutasi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2042))
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Menikah / Pindah Keluarga", "Pindah Rumah Tinggal Dalam Desa", "Penyesuaian Wilayah RT/RW").forEach { alasanOpt ->
                            val isSel = alasanMutasi == alasanOpt
                            Surface(
                                color = if (isSel) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF7C3AED) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { alasanMutasi = alasanOpt }
                            ) {
                                Text(
                                    alasanOpt,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF6D28D9) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updates = VoterUpdatesPayload(
                            targetRw = targetRw,
                            targetRt = targetRt,
                            alasanMutasi = alasanMutasi,
                            rw = targetRw,
                            rt = targetRt,
                            tps = "TPS $targetRw"
                        )
                        submitCoklikAction(voter, "PINDAH_RW", updates = updates)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    enabled = !actionInProgress
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Proses Pindah RW", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPindahRwDialogFor = null }, enabled = !actionInProgress) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // ==========================================
    // 3. MODAL TMS (TIDAK MEMENUHI SYARAT)
    // ==========================================
    if (showTmsDialogFor != null) {
        val voter = showTmsDialogFor!!
        var selectedReason by remember { mutableStateOf(TMS_REASONS[0]) }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showTmsDialogFor = null },
            title = {
                Column {
                    Text("Tandai TMS (Tidak Memenuhi Syarat)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2042))
                    Text("Pilih 1 dari 8 alasan TMS resmi sesuai temuan fisik:", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TMS_REASONS.forEach { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            color = if (isSelected) Rose600.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Rose500 else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = Rose600)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Rose700 else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitCoklikAction(voter, "TMS", alasanTms = selectedReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    enabled = !actionInProgress
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Tandai TMS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTmsDialogFor = null }, enabled = !actionInProgress) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // ==========================================
    // 4. MODAL EDIT DATA LANGSUNG (KUNCI KALISALAK & DROPDOWN RT)
    // ==========================================
    if (showEditDialogFor != null) {
        val voter = showEditDialogFor!!

        var editNik by remember { mutableStateOf(voter.nik) }
        var editNoKk by remember { mutableStateOf(voter.noKk) }
        var editNama by remember { mutableStateOf(voter.displayName) }
        var editTempatLahir by remember { mutableStateOf(voter.tempatLahir ?: "TEGAL") }
        var editTglLahir by remember { mutableStateOf(voter.tanggalLahir ?: "") }
        var editKelamin by remember { mutableStateOf(if (voter.jenisKelamin?.startsWith("P") == true) "P" else "L") }
        var editKawin by remember { mutableStateOf(voter.displayStatusPerkawinan) }
        var editRt by remember { mutableStateOf((voter.rt ?: "01").padStart(2, '0')) }
        var editDisabilitas by remember { mutableStateOf(voter.disabilitas ?: "Bukan Penyandang Disabilitas") }

        var validationError by remember { mutableStateOf<String?>(null) }
        var showDatePickerModal by remember { mutableStateOf(false) }

        val liveAge = remember(editTglLahir) {
            var calculated = 0
            val raw = editTglLahir.trim()
            if (raw.isNotEmpty()) {
                try {
                    val parts = raw.split("-", "/")
                    if (parts.size >= 3) {
                        val y = if (parts[0].length == 4) parts[0].toIntOrNull() else parts[2].toIntOrNull()
                        val m = parts[1].toIntOrNull()
                        val d = if (parts[0].length == 4) parts[2].toIntOrNull() else parts[0].toIntOrNull()
                        if (y != null && y in 1900..2026) {
                            val cal = Calendar.getInstance()
                            val curYear = cal.get(Calendar.YEAR)
                            val curMonth = cal.get(Calendar.MONTH) + 1
                            val curDay = cal.get(Calendar.DAY_OF_MONTH)
                            calculated = curYear - y
                            if (m != null && d != null && (curMonth < m || (curMonth == m && curDay < d))) {
                                calculated--
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            if (calculated >= 0) calculated else 0
        }

        if (showDatePickerModal) {
            val initialMillis = remember(editTglLahir) {
                try {
                    val parts = editTglLahir.split("-", "/")
                    if (parts.size >= 3) {
                        val y = if (parts[0].length == 4) parts[0].toInt() else parts[2].toInt()
                        val m = parts[1].toInt() - 1
                        val d = if (parts[0].length == 4) parts[2].toInt() else parts[0].toInt()
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            clear()
                            set(y, m, d, 0, 0, 0)
                        }
                        cal.timeInMillis
                    } else null
                } catch (_: Exception) { null }
            }

            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

            DatePickerDialog(
                onDismissRequest = { showDatePickerModal = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val y = cal.get(Calendar.YEAR)
                            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
                            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
                            editTglLahir = "$y-$m-$d"
                        }
                        showDatePickerModal = false
                    }) {
                        Text("Pilih Tanggal", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerModal = false }) {
                        Text("Batal", color = Color(0xFF64748B))
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F2042),
                    headlineContentColor = Color(0xFF0F2042)
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F2042),
                        headlineContentColor = Color(0xFF0F2042),
                        selectedDayContainerColor = Color(0xFF2563EB),
                        selectedDayContentColor = Color.White,
                        todayDateBorderColor = Amber600
                    )
                )
            }
        }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showEditDialogFor = null },
            title = {
                Column {
                    Text("Perbaiki Data Pemilih", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2042))
                    Text("Semua kolom dapat langsung diubah sesuai dokumen sah:", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (validationError != null) {
                        Surface(
                            color = Rose600.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(validationError!!, fontSize = 11.sp, color = Rose600, modifier = Modifier.padding(8.dp))
                        }
                    }

                    // KUNCI WILAYAH KALISALAK (TERKUNCI / READ ONLY)
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Desa / Domisili Wajib (Terkunci)", fontSize = 10.sp, color = Color(0xFF1E40AF))
                                Text("DESA KALISALAK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                            }
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("RW ${voter.rw ?: "01"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }

                    // 1. NIK
                    OutlinedTextField(
                        value = editNik,
                        onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) editNik = it },
                        label = { Text("Nomor NIK (16 Digit Angka)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. NO KK
                    OutlinedTextField(
                        value = editNoKk,
                        onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) editNoKk = it },
                        label = { Text("Nomor Kartu Keluarga (16 Digit)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 3. NAMA LENGKAP
                    OutlinedTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama Lengkap", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 4. TEMPAT LAHIR
                    OutlinedTextField(
                        value = editTempatLahir,
                        onValueChange = { editTempatLahir = it },
                        label = { Text("Tempat Lahir", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 5. TANGGAL LAHIR (MODERN KALENDER MATERIAL 3)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tanggal Lahir (Ketuk untuk Buka Kalender)", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(
                                    text = if (editTglLahir.isNotBlank()) editTglLahir else "Pilih Tanggal Lahir",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F2042)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = "$liveAge Th",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Kalender",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 6. JENIS KELAMIN
                    Column {
                        Text("Jenis Kelamin", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("L" to "Laki-laki (L)", "P" to "Perempuan (P)").forEach { (code, lbl) ->
                                val isSel = editKelamin == code
                                Surface(
                                    color = if (isSel) Color(0xFF0F2042) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF0F2042) else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editKelamin = code }
                                ) {
                                    Text(
                                        lbl,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 7. STATUS PERKAWINAN
                    Column {
                        Text("Status Perkawinan", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Belum Kawin", "Kawin", "Cerai Hidup", "Cerai Mati").forEach { opt ->
                                val isSel = editKawin == opt
                                Surface(
                                    color = if (isSel) Amber600 else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Amber600 else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editKawin = opt }
                                ) {
                                    Text(
                                        opt,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 8. RT DROPDOWN / EKSKLUSIF (01, 02, 03)
                    Column {
                        Text("Rukun Tetangga (RT) - Wajib 01, 02, 03", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("01", "02", "03").forEach { rtOpt ->
                                val isSel = editRt == rtOpt
                                Surface(
                                    color = if (isSel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editRt = rtOpt }
                                ) {
                                    Text(
                                        "RT $rtOpt",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 9. DISABILITAS
                    Column {
                        Text("Ragam Disabilitas", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Bukan Penyandang Disabilitas", "Fisik", "Netra", "Rungu", "Wicara", "Mental").forEach { disOpt ->
                                val isSel = editDisabilitas == disOpt
                                Surface(
                                    color = if (isSel) Emerald600 else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Emerald600 else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editDisabilitas = disOpt }
                                ) {
                                    Text(
                                        disOpt,
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNik.length != 16) {
                            validationError = "Nomor NIK harus tepat 16 digit angka."
                            return@Button
                        }
                        if (editNoKk.length != 16) {
                            validationError = "Nomor KK harus tepat 16 digit angka."
                            return@Button
                        }
                        if (editNama.isBlank()) {
                            validationError = "Nama lengkap tidak boleh kosong."
                            return@Button
                        }

                        val updates = VoterUpdatesPayload(
                            nik = editNik,
                            noKk = editNoKk,
                            namaLengkap = editNama,
                            tempatLahir = editTempatLahir,
                            tanggalLahir = editTglLahir,
                            jenisKelamin = editKelamin,
                            statusPerkawinan = editKawin,
                            alamat = "Desa Kalisalak",
                            rt = editRt,
                            disabilitas = editDisabilitas
                        )
                        submitCoklikAction(voter, "UBAH_DATA", updates = updates)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber600),
                    enabled = !actionInProgress
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Perubahan Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogFor = null }, enabled = !actionInProgress) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // Modal Add New Voter (Potensial)
    if (showAddVoterDialog) {
        var newNik by remember { mutableStateOf("") }
        var newNoKk by remember { mutableStateOf("") }
        var newNama by remember { mutableStateOf("") }
        var newTglLahir by remember { mutableStateOf("2000-01-01") }
        var newJk by remember { mutableStateOf("L") }
        var newKawin by remember { mutableStateOf("Belum Kawin") }
        var newRt by remember { mutableStateOf("01") }
        var newRw by remember { mutableStateOf("01") }
        var newDisabilitas by remember { mutableStateOf("TIDAK") }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showAddVoterDialog = false },
            title = { Text("Tambah Pemilih Baru (Potensial)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2042)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Domisili Wajib: Desa Kalisalak", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                    OutlinedTextField(
                        value = newNik,
                        onValueChange = { if (it.length <= 16) newNik = it },
                        label = { Text("NIK (16 Digit)*", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNoKk,
                        onValueChange = { if (it.length <= 16) newNoKk = it },
                        label = { Text("Nomor KK (16 Digit)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNama,
                        onValueChange = { newNama = it },
                        label = { Text("Nama Lengkap Sesuai KTP*", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newTglLahir,
                        onValueChange = { newTglLahir = it },
                        label = { Text("Tanggal Lahir (YYYY-MM-DD)*", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Dropdown RT 01-03
                    Column {
                        Text("RT:", fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("01", "02", "03").forEach { r ->
                                val sel = newRt == r
                                Surface(
                                    color = if (sel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).clickable { newRt = r }
                                ) {
                                    Text("RT $r", fontSize = 11.sp, color = if (sel) Color.White else Color(0xFF334155), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            actionInProgress = true
                            try {
                                val req = CreateVoterRequest(
                                    nik = newNik,
                                    noKk = newNoKk,
                                    namaLengkap = newNama,
                                    tanggalLahir = newTglLahir,
                                    jenisKelamin = newJk,
                                    statusPerkawinan = newKawin,
                                    alamat = "Desa Kalisalak",
                                    rt = newRt,
                                    rw = newRw,
                                    disabilitas = newDisabilitas
                                )
                                val res = ApiClient.api.createNewVoter(req)
                                if (res.isSuccessful && res.body()?.success == true) {
                                    snackbarMessage = "Pemilih baru ${newNama} berhasil didaftarkan!"
                                    showAddVoterDialog = false
                                    loadVoters(forceNetwork = true)
                                } else {
                                    snackbarMessage = res.body()?.message ?: "Gagal menambah pemilih."
                                }
                            } catch (e: Exception) {
                                snackbarMessage = "Kesalahan jaringan: " + (e.localizedMessage ?: "Coba lagi")
                            } finally {
                                actionInProgress = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2042)),
                    enabled = !actionInProgress && newNik.length == 16 && newNama.isNotBlank()
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Daftarkan Pemilih Baru", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVoterDialog = false }, enabled = !actionInProgress) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // ==========================================
    // SCAFFOLD UTAMA (EKSEKUTIF PUTIH BERSIH)
    // ==========================================
    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddVoterDialog = true },
                containerColor = Color(0xFF0F2042),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Pemilih Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar Clean
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari NIK, No KK, atau Nama Pemilih...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF2563EB)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; loadVoters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF0F2042),
                    unfocusedTextColor = Color(0xFF0F2042)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val chips = listOf(
                    "SEMUA" to "Semua ($totalCount)",
                    "BELUM" to "Belum Coklit",
                    "COCOK" to "Cocok / Sesuai",
                    "TMS" to "TMS",
                    "RT01" to "RT 01",
                    "RT02" to "RT 02",
                    "RT03" to "RT 03"
                )
                items(chips) { (key, label) ->
                    val isSelected = selectedFilterChip == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterChip = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F2042),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color(0xFF0F2042) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary Info Strip & Refresh Button (TETAP DIPERTAHANKAN DI MENU PEMILIH SESUAI INSTRUKSI)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredVoters.size} dari $totalCount pemilih",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { loadVoters(forceNetwork = true) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang Data",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Snackbar Info Feedback
            if (snackbarMessage != null) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(snackbarMessage!!, color = Color(0xFF1E3A8A), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // State Handling
            if (isLoading && voterList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat data pemilih resmi...", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else if (errorMessage != null && voterList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage!!, color = Rose600, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadVoters(forceNetwork = true) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2042))) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else if (filteredVoters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada pemilih yang cocok.", fontSize = 14.sp, color = Color(0xFF0F2042), fontWeight = FontWeight.SemiBold)
                        Text("Periksa kembali kata kunci pencarian atau filter yang dipilih.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredVoters, key = { it.id }) { voter ->
                        val isNikUnmasked = unmaskedNikIds.contains(voter.id)

                        VoterCardComplete(
                            voter = voter,
                            isNikUnmasked = isNikUnmasked,
                            onCardClick = { showDetailDialogFor = voter },
                            onToggleNikMask = {
                                unmaskedNikIds = if (isNikUnmasked) unmaskedNikIds - voter.id else unmaskedNikIds + voter.id
                            },
                            onActionCocok = { submitCoklikAction(voter, "COCOK") },
                            onActionTms = { showTmsDialogFor = voter },
                            onActionUbah = { showEditDialogFor = voter },
                            onActionPindahRw = { showPindahRwDialogFor = voter }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoterCardComplete(
    voter: VoterItem,
    isNikUnmasked: Boolean,
    onCardClick: () -> Unit,
    onToggleNikMask: () -> Unit,
    onActionCocok: () -> Unit,
    onActionTms: () -> Unit,
    onActionUbah: () -> Unit,
    onActionPindahRw: () -> Unit
) {
    val isCocok = voter.isSudahCoklit && !voter.isTms
    val isTms = voter.isTms
    val isPindahRw = voter.displayStatusCoklit == "PINDAH_RW"

    val statusBadgeColor = when {
        isTms -> Rose600
        isPindahRw -> Color(0xFF7C3AED)
        isCocok -> Emerald600
        else -> Color(0xFF64748B)
    }

    val statusBadgeText = when {
        isTms -> "TMS (${voter.alasanTms ?: "Tidak Memenuhi Syarat"})"
        isPindahRw -> "MUTASI PINDAH RW"
        isCocok -> "SUDAH COKLIT / SESUAI"
        else -> "BELUM COKLIT"
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCocok) Emerald500.copy(alpha = 0.5f) else if (isTms) Rose500.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Nama Lengkap & Usia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFFEFF6FF) else Color(0xFFFDF2F8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFF3B82F6) else Color(0xFFEC4899)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = voter.jenisKelamin?.take(1) ?: "L",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (voter.jenisKelamin?.startsWith("L") == true) Color(0xFF1D4ED8) else Color(0xFFBE185D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = voter.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2042)
                        )
                        Text(
                            text = "${voter.tempatLahir ?: "TEGAL"}, ${voter.tanggalLahir ?: "-"} (${voter.displayAge} Th) • ${voter.displayStatusPerkawinan}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // NIK & No KK Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "NIK: ", fontSize = 12.sp, color = Color(0xFF64748B))
                        val displayNik = if (isNikUnmasked) {
                            voter.nik
                        } else {
                            if (voter.nik.length >= 16) "${voter.nik.take(4)}********${voter.nik.takeLast(4)}" else voter.nik
                        }
                        Text(
                            text = displayNik,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        IconButton(onClick = onToggleNikMask, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (isNikUnmasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Buka/Tutup NIK",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "No KK: ${voter.noKk}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RT ${(voter.rt ?: "01").padStart(2, '0')} / RW ${(voter.rw ?: "01").padStart(2, '0')}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2042)
                    )
                    Text(
                        text = voter.alamat ?: "Desa Kalisalak",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge Strip
            Surface(
                color = statusBadgeColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusBadgeColor
                    )
                    if (!voter.disabilitas.isNullOrBlank() && voter.disabilitas != "TIDAK" && voter.disabilitas != "0") {
                        Surface(
                            color = Amber500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Disabilitas: ${voter.disabilitas}",
                                fontSize = 10.sp,
                                color = Amber700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Action Buttons Lapangan Coklit (Cocok, TMS, Pindah RW, Ubah)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tombol 1: Konfirmasi Cocok
                Button(
                    onClick = onActionCocok,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCocok) Emerald700 else Emerald600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isCocok) "Sesuai ✓" else "Cocok", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Tombol 2: Tandai TMS
                Button(
                    onClick = onActionTms,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTms) Rose700 else Rose600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isTms) "TMS ✕" else "TMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Tombol 3: Pindah RW (Mutasi Antar-RW)
                OutlinedButton(
                    onClick = onActionPindahRw,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7C3AED)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF7C3AED))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Pindah RW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }

                // Tombol 4: Ubah Data
                OutlinedButton(
                    onClick = onActionUbah,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Amber600
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber600.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber600)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Ubah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber600)
                }
            }
        }
    }
}
