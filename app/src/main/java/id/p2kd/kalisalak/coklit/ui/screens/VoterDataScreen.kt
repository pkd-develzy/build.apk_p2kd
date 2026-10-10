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
                            item.copy(
                                status = if (action == "TMS") "TMS" else "AKTIF",
                                statusAktif = if (action == "TMS") "TMS" else "AKTIF",
                                coklitStatus = if (action == "COCOK") "SUDAH" else action,
                                alasanTms = alasanTms ?: item.alasanTms,
                                nik = updates?.nik ?: item.nik,
                                kk = updates?.noKk ?: item.kk,
                                namaLengkap = updates?.namaLengkap ?: item.namaLengkap,
                                tempatLahir = updates?.tempatLahir ?: item.tempatLahir,
                                tanggalLahir = updates?.tanggalLahir ?: item.tanggalLahir,
                                jenisKelamin = updates?.jenisKelamin ?: item.jenisKelamin,
                                statusPerkawinan = updates?.statusPerkawinan ?: item.statusPerkawinan,
                                alamat = updates?.alamat ?: item.alamat,
                                rt = updates?.rt ?: item.rt,
                                rw = updates?.rw ?: item.rw,
                                disabilitas = updates?.disabilitas ?: item.disabilitas
                            )
                        } else item
                    }
                    activeActionVoter = null
                    showTmsDialogFor = null
                    showEditDialogFor = null
                    showDetailDialogFor = null
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
            // Search filter
            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    voter.nik.lowercase().contains(query) ||
                    voter.noKk.lowercase().contains(query) ||
                    voter.displayName.lowercase().contains(query) ||
                    (voter.alamat ?: "").lowercase().contains(query)

            // Chip filter
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
    // 1. MODAL DETAIL PEMILIH (HANYA VIEW)
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
                        color = if (voter.jenisKelamin?.startsWith("L") == true) Blue950 else Indigo950,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (voter.jenisKelamin?.startsWith("L") == true) Blue400 else Indigo400),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = voter.jenisKelamin?.take(1) ?: "L",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (voter.jenisKelamin?.startsWith("L") == true) Blue400 else Indigo400
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = voter.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = White
                        )
                        Text(
                            text = when {
                                voter.isTms -> "Status: TMS (${voter.alasanTms ?: "Tidak Memenuhi Syarat"})"
                                voter.isSudahCoklit -> "Status: SUDAH COKLIT / SESUAI"
                                else -> "Status: BELUM COKLIT"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                voter.isTms -> Rose400
                                voter.isSudahCoklit -> Emerald400
                                else -> Amber400
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
                            color = Emerald900.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(copyNotice!!, fontSize = 11.sp, color = Emerald300, modifier = Modifier.padding(8.dp))
                        }
                    }

                    // Card 1: Identitas NIK & No KK (dengan tombol salin)
                    Surface(
                        color = Navy800.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor Induk Kependudukan (NIK)", fontSize = 10.sp, color = Slate400)
                                    Text(voter.nik, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue300)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(voter.nik))
                                        copyNotice = "NIK ${voter.nik} berhasil disalin!"
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin NIK", tint = Blue400, modifier = Modifier.size(16.dp))
                                }
                            }

                            HorizontalDivider(color = Slate800, thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor Kartu Keluarga (KK)", fontSize = 10.sp, color = Slate400)
                                    Text(voter.noKk, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(voter.noKk))
                                        copyNotice = "No KK ${voter.noKk} berhasil disalin!"
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin No KK", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Card 2: Kelahiran & Usia Presisi
                    Surface(
                        color = Navy800.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tempat & Tanggal Lahir", fontSize = 10.sp, color = Slate400)
                                    Text(
                                        "${voter.tempatLahir ?: "TEGAL"}, ${voter.tanggalLahir ?: "-"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                }
                                Surface(
                                    color = Blue900.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue500)
                                ) {
                                    Text(
                                        text = "${voter.displayAge} Tahun",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blue300,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate800, thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Jenis Kelamin", fontSize = 10.sp, color = Slate400)
                                    Text(
                                        if (voter.jenisKelamin?.startsWith("L") == true) "Laki-laki" else "Perempuan",
                                        fontSize = 12.sp,
                                        color = White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status Perkawinan", fontSize = 10.sp, color = Slate400)
                                    Text(
                                        voter.displayStatusPerkawinan,
                                        fontSize = 12.sp,
                                        color = Amber400,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Card 3: Wilayah Domisili & TPS
                    Surface(
                        color = Navy800.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Alamat Domisili", fontSize = 10.sp, color = Slate400)
                            Text(voter.alamat ?: "Desa Kalisalak", fontSize = 12.sp, color = White, fontWeight = FontWeight.Medium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RT ${(voter.rt ?: "01").padStart(2, '0')} / RW ${(voter.rw ?: "01").padStart(2, '0')}", fontSize = 12.sp, color = Blue300, fontWeight = FontWeight.Bold)
                                Text(voter.tps ?: "TPS 0${voter.rw ?: "1"}", fontSize = 12.sp, color = Emerald400, fontWeight = FontWeight.Bold)
                            }
                            if (!voter.disabilitas.isNullOrBlank() && voter.disabilitas != "TIDAK" && voter.disabilitas != "0") {
                                HorizontalDivider(color = Slate800, thickness = 0.8.dp)
                                Text("Ragam Disabilitas: ${voter.disabilitas}", fontSize = 11.sp, color = Amber400)
                            }
                        }
                    }

                    // Card 4: Status Coklit & Petugas
                    Surface(
                        color = Navy800.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Petugas Pencoklit", fontSize = 10.sp, color = Slate400)
                                Text(voter.coklitPetugas ?: "Belum Dicoklit", fontSize = 11.sp, color = White)
                            }
                            if (!voter.coklitTanggal.isNullOrBlank()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Waktu Coklit", fontSize = 10.sp, color = Slate400)
                                    Text(voter.coklitTanggal ?: "-", fontSize = 11.sp, color = Slate300)
                                }
                            }
                            if (voter.isTms && !voter.alasanTms.isNullOrBlank()) {
                                Text("Alasan TMS: ${voter.alasanTms}", fontSize = 11.sp, color = Rose400, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailDialogFor = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val target = voter
                        showDetailDialogFor = null
                        showEditDialogFor = target
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber400),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber500),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber400)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubah Data", color = Amber400, fontSize = 12.sp)
                }
            },
            containerColor = Navy900
        )
    }

    // ==========================================
    // 2. MODAL TMS (TIDAK MEMENUHI SYARAT)
    // ==========================================
    if (showTmsDialogFor != null) {
        val voter = showTmsDialogFor!!
        var selectedReason by remember { mutableStateOf(TMS_REASONS[0]) }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showTmsDialogFor = null },
            title = {
                Column {
                    Text("Tandai TMS (Tidak Memenuhi Syarat)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
                    Text("Pilih 1 dari 8 alasan TMS resmi sesuai temuan fisik:", fontSize = 11.sp, color = Slate400)
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
                            color = if (isSelected) Rose900.copy(alpha = 0.5f) else Navy800,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Rose500 else Slate800),
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
                                    colors = RadioButtonDefaults.colors(selectedColor = Rose500)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    color = if (isSelected) White else Slate300
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Tandai TMS", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTmsDialogFor = null }, enabled = !actionInProgress) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    // ==========================================
    // 3. MODAL EDIT DATA LANGSUNG (BEBAS EDIT KAPAN SAJA TANPA CENTANG)
    // ==========================================
    if (showEditDialogFor != null) {
        val voter = showEditDialogFor!!

        // Nilai input form (langsung terisi data lama dan bebas diedit)
        var editNik by remember { mutableStateOf(voter.nik) }
        var editNoKk by remember { mutableStateOf(voter.noKk) }
        var editNama by remember { mutableStateOf(voter.displayName) }
        var editTempatLahir by remember { mutableStateOf(voter.tempatLahir ?: "TEGAL") }
        var editTglLahir by remember { mutableStateOf(voter.tanggalLahir ?: "") }
        var editKelamin by remember { mutableStateOf(if (voter.jenisKelamin?.startsWith("P") == true) "P" else "L") }
        var editKawin by remember { mutableStateOf(voter.displayStatusPerkawinan) }
        var editAlamat by remember { mutableStateOf(voter.alamat ?: "") }
        var editRt by remember { mutableStateOf((voter.rt ?: "01").padStart(2, '0')) }
        var editDisabilitas by remember { mutableStateOf(voter.disabilitas ?: "Bukan Penyandang Disabilitas") }

        var validationError by remember { mutableStateOf<String?>(null) }
        var showDatePickerModal by remember { mutableStateOf(false) }

        // Live calculation usia saat tanggal lahir dipilih
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

        // Material 3 DatePickerDialog
        if (showDatePickerModal) {
            val initialMillis = remember(editTglLahir) {
                try {
                    val parts = editTglLahir.trim().split("-", "/")
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
                        Text("Pilih Tanggal", color = Blue400, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerModal = false }) {
                        Text("Batal", color = Slate400)
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Navy900,
                    titleContentColor = White,
                    headlineContentColor = White
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Navy900,
                        titleContentColor = White,
                        headlineContentColor = White,
                        selectedDayContainerColor = Blue600,
                        selectedDayContentColor = White,
                        todayDateBorderColor = Amber400
                    )
                )
            }
        }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showEditDialogFor = null },
            title = {
                Column {
                    Text("Perbaiki Data Pemilih", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
                    Text("Semua kolom dapat langsung diubah sesuai KTP/KK warga:", fontSize = 11.sp, color = Amber400)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (validationError != null) {
                        Surface(
                            color = Rose900.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(validationError!!, fontSize = 11.sp, color = Rose400, modifier = Modifier.padding(8.dp))
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
                        color = Navy800.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.5f)),
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
                                Text("Tanggal Lahir (Ketuk untuk Buka Kalender)", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = if (editTglLahir.isNotBlank()) editTglLahir else "Pilih Tanggal Lahir",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Blue900.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue500)
                                ) {
                                    Text(
                                        text = "$liveAge Th",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blue300,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Kalender",
                                    tint = Blue400,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 6. JENIS KELAMIN (TOGGLE BUTTON)
                    Column {
                        Text("Jenis Kelamin", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("L" to "Laki-laki (L)", "P" to "Perempuan (P)").forEach { (code, lbl) ->
                                val isSel = editKelamin == code
                                Surface(
                                    color = if (isSel) Blue900.copy(alpha = 0.5f) else Navy800,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Blue500 else Slate800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editKelamin = code }
                                ) {
                                    Text(
                                        lbl,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) White else Slate400,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 7. STATUS PERKAWINAN (NORMALISASI REAL LAPANGAN)
                    Column {
                        Text("Status Perkawinan", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Belum Kawin", "Kawin", "Cerai Hidup", "Cerai Mati").forEach { opt ->
                                val isSel = editKawin == opt
                                Surface(
                                    color = if (isSel) Amber900.copy(alpha = 0.4f) else Navy800,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Amber500 else Slate800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editKawin = opt }
                                ) {
                                    Text(
                                        opt,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Amber400 else Slate400,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 8. RT
                    Column {
                        Text("Rukun Tetangga (RT)", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("01", "02", "03").forEach { rtOpt ->
                                val isSel = editRt == rtOpt
                                Surface(
                                    color = if (isSel) Blue900.copy(alpha = 0.5f) else Navy800,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Blue500 else Slate800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editRt = rtOpt }
                                ) {
                                    Text(
                                        "RT $rtOpt",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) White else Slate400,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 9. ALAMAT
                    OutlinedTextField(
                        value = editAlamat,
                        onValueChange = { editAlamat = it },
                        label = { Text("Alamat Dusun / Jalan", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 10. DISABILITAS
                    Column {
                        Text("Ragam Disabilitas", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Bukan Penyandang Disabilitas", "Fisik", "Netra", "Rungu", "Wicara", "Mental").forEach { disOpt ->
                                val isSel = editDisabilitas == disOpt
                                Surface(
                                    color = if (isSel) Emerald900.copy(alpha = 0.4f) else Navy800,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Emerald500 else Slate800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editDisabilitas = disOpt }
                                ) {
                                    Text(
                                        disOpt,
                                        fontSize = 11.sp,
                                        color = if (isSel) Emerald400 else Slate300,
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
                            alamat = editAlamat,
                            rt = editRt,
                            disabilitas = editDisabilitas
                        )
                        submitCoklikAction(voter, "UBAH_DATA", updates = updates)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber600),
                    enabled = !actionInProgress
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Perubahan Data", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogFor = null }, enabled = !actionInProgress) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
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
        var newAlamat by remember { mutableStateOf("Kalisalak") }
        var newRt by remember { mutableStateOf("01") }
        var newRw by remember { mutableStateOf("01") }
        var newDisabilitas by remember { mutableStateOf("TIDAK") }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showAddVoterDialog = false },
            title = { Text("Tambah Pemilih Baru (Potensial)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Daftarkan pemilih baru yang memenuhi syarat:", fontSize = 12.sp, color = Slate400)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Jenis Kelamin: ", fontSize = 12.sp, color = White)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { newJk = "L" }) {
                            RadioButton(selected = newJk == "L", onClick = { newJk = "L" })
                            Text("Laki-laki", fontSize = 12.sp, color = White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { newJk = "P" }) {
                            RadioButton(selected = newJk == "P", onClick = { newJk = "P" })
                            Text("Perempuan", fontSize = 12.sp, color = White)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newRt,
                            onValueChange = { newRt = it },
                            label = { Text("RT", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newRw,
                            onValueChange = { newRw = it },
                            label = { Text("RW", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = newAlamat,
                        onValueChange = { newAlamat = it },
                        label = { Text("Alamat Dusun", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                                    alamat = newAlamat,
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
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    enabled = !actionInProgress && newNik.length == 16 && newNama.isNotBlank()
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Daftarkan Pemilih Baru", color = White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVoterDialog = false }, enabled = !actionInProgress) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    Scaffold(
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddVoterDialog = true },
                containerColor = Blue600,
                contentColor = White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari NIK, No KK, atau Nama Pemilih...", fontSize = 13.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Blue400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; loadVoters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Slate400)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Navy900,
                    unfocusedContainerColor = Navy900,
                    focusedBorderColor = Blue500,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Filter Chips (Horizontal Scroll)
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
                            selectedContainerColor = Blue600,
                            selectedLabelColor = White,
                            containerColor = Navy900,
                            labelColor = Slate300
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Blue500 else Slate800
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary Info Strip & Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredVoters.size} dari $totalCount pemilih",
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { loadVoters(forceNetwork = true) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang Data",
                        tint = Blue400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Snackbar Info Feedback
            if (snackbarMessage != null) {
                Surface(
                    color = Blue950,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue500),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(snackbarMessage!!, color = White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // State Handling: Loading, Error, Empty, List
            if (isLoading && voterList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Blue500)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat data pemilih resmi...", color = Slate400, fontSize = 13.sp)
                    }
                }
            } else if (errorMessage != null && voterList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose500, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage!!, color = Rose400, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadVoters(forceNetwork = true) }, colors = ButtonDefaults.buttonColors(containerColor = Blue600)) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else if (filteredVoters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada pemilih yang cocok.", fontSize = 14.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                        Text("Periksa kembali kata kunci pencarian atau filter yang dipilih.", fontSize = 12.sp, color = Slate400)
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
                            onActionUbah = { showEditDialogFor = voter }
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
    onActionUbah: () -> Unit
) {
    val isCocok = voter.isSudahCoklit && !voter.isTms
    val isTms = voter.isTms

    val statusBadgeColor = when {
        isTms -> Rose500
        isCocok -> Emerald500
        else -> Slate400
    }

    val statusBadgeText = when {
        isTms -> "TMS (${voter.alasanTms ?: "Tidak Memenuhi Syarat"})"
        isCocok -> "SUDAH COKLIT / SESUAI"
        else -> "BELUM COKLIT"
    }

    Surface(
        color = Navy900,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCocok) Emerald900 else if (isTms) Rose900 else Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Nama Lengkap & Status Coklit Badge
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
                        color = if (voter.jenisKelamin?.startsWith("L") == true) Blue950 else Indigo950,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (voter.jenisKelamin?.startsWith("L") == true) Blue400 else Indigo400),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = voter.jenisKelamin?.take(1) ?: "L",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (voter.jenisKelamin?.startsWith("L") == true) Blue400 else Indigo400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = voter.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        // Perhitungan umur cerdas otomatis (bukan 0 Th) & status perkawinan riil
                        Text(
                            text = "${voter.tempatLahir ?: "TEGAL"}, ${voter.tanggalLahir ?: "-"} (${voter.displayAge} Th) • ${voter.displayStatusPerkawinan}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Garis Pemisah Halus
            HorizontalDivider(color = Slate800, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // NIK & No KK Row dengan tombol intip mata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "NIK: ", fontSize = 12.sp, color = Slate400)
                        val displayNik = if (isNikUnmasked) {
                            voter.nik
                        } else {
                            if (voter.nik.length >= 16) "${voter.nik.take(4)}********${voter.nik.takeLast(4)}" else voter.nik
                        }
                        Text(
                            text = displayNik,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue300
                        )
                        IconButton(onClick = onToggleNikMask, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (isNikUnmasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Buka/Tutup NIK",
                                tint = Blue400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "No KK: ${voter.noKk}",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RT ${(voter.rt ?: "01").padStart(2, '0')} / RW ${(voter.rw ?: "01").padStart(2, '0')}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Text(
                        text = voter.alamat ?: "Desa Kalisalak",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge Strip
            Surface(
                color = statusBadgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.4f)),
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
                            color = Amber900.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Disabilitas: ${voter.disabilitas}",
                                fontSize = 10.sp,
                                color = Amber400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Action Buttons Lapangan Coklit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol 1: Konfirmasi Cocok
                Button(
                    onClick = onActionCocok,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCocok) Emerald700 else Emerald600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCocok) "Sesuai ✓" else "Cocok", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = White)
                }

                // Tombol 2: Tandai TMS
                Button(
                    onClick = onActionTms,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTms) Rose700 else Rose600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp), tint = White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTms) "TMS ✕" else "TMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = White)
                }

                // Tombol 3: Ubah Data (Langsung membuka form edit tanpa mencentang)
                OutlinedButton(
                    onClick = onActionUbah,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Amber400
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber400)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber400)
                }
            }
        }
    }
}
