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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager
import id.p2kd.kalisalak.coklit.data.models.*
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val cacheManager = remember { LocalVoterCacheManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("SEMUA") } // SEMUA | BELUM | COCOK | TMS | RT01 | RT02 ...
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var voterList by remember { mutableStateOf<List<VoterItem>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }

    // State for interactive modals
    var activeActionVoter by remember { mutableStateOf<VoterItem?>(null) }
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
                val res = ApiClient.api.getVoters(
                    tahap = "CALON_DPS",
                    search = if (searchQuery.isBlank()) null else searchQuery.trim(),
                    page = 1,
                    limit = 100
                )
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()!!
                    voterList = body.data
                    totalCount = body.total
                    errorMessage = null
                    cacheManager.saveVoters("CALON_DPS", body.data, body.total)
                } else if (voterList.isEmpty()) {
                    errorMessage = res.body()?.message ?: "Gagal memuat daftar pemilih."
                }
            } catch (e: Exception) {
                if (voterList.isEmpty()) {
                    errorMessage = "Koneksi terganggu: " + (e.localizedMessage ?: "Silakan periksa jaringan")
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
                                alasanTms = alasanTms
                            )
                        } else item
                    }
                    activeActionVoter = null
                    showTmsDialogFor = null
                    showEditDialogFor = null
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

    // Filter computation
    val filteredVoters = remember(voterList, selectedFilterChip, searchQuery) {
        voterList.filter { voter ->
            val matchFilter = when (selectedFilterChip) {
                "SEMUA" -> true
                "BELUM" -> !voter.isSudahCoklit && !voter.isTms
                "COCOK" -> voter.isSudahCoklit && !voter.isTms
                "TMS" -> voter.isTms
                else -> {
                    if (selectedFilterChip.startsWith("RT")) {
                        val rtNum = selectedFilterChip.removePrefix("RT").trim()
                        (voter.rt ?: "").replace("\\D".toRegex(), "").padStart(2, '0') == rtNum.padStart(2, '0')
                    } else true
                }
            }
            val matchSearch = searchQuery.isBlank() ||
                    voter.displayName.contains(searchQuery, ignoreCase = true) ||
                    voter.nik.contains(searchQuery, ignoreCase = true) ||
                    (voter.kk ?: "").contains(searchQuery, ignoreCase = true) ||
                    voter.alamat.orEmpty().contains(searchQuery, ignoreCase = true)
            matchFilter && matchSearch
        }
    }

    // Modal TMS Dialog
    if (showTmsDialogFor != null) {
        val voter = showTmsDialogFor!!
        var selectedReason by remember { mutableStateOf(TMS_REASONS.first()) }
        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showTmsDialogFor = null },
            title = {
                Text(
                    text = "Tandai TMS (Tidak Memenuhi Syarat)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = White
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Pemilih: ${voter.displayName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Blue400
                    )
                    Text(
                        text = "Pilih salah satu dari 8 alasan resmi TMS:",
                        fontSize = 12.sp,
                        color = Slate300,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    TMS_REASONS.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = Rose500)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = reason, fontSize = 12.sp, color = White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitCoklikAction(voter, "TMS", selectedReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    enabled = !actionInProgress
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Status TMS", color = White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTmsDialogFor = null },
                    enabled = !actionInProgress
                ) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    // Modal Edit Voter Dialog
    if (showEditDialogFor != null) {
        val voter = showEditDialogFor!!
        var editNama by remember { mutableStateOf(voter.displayName) }
        var editTglLahir by remember { mutableStateOf(voter.tanggalLahir ?: "") }
        var editKawin by remember { mutableStateOf(voter.statusPerkawinan ?: "Kawin") }
        var editAlamat by remember { mutableStateOf(voter.alamat ?: "") }
        var editRt by remember { mutableStateOf(voter.rt ?: "01") }
        var editRw by remember { mutableStateOf(voter.rw ?: "01") }
        var editDisabilitas by remember { mutableStateOf(voter.disabilitas ?: "TIDAK") }

        AlertDialog(
            onDismissRequest = { if (!actionInProgress) showEditDialogFor = null },
            title = { Text("Ubah Elemen Data Pemilih", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("NIK: ${voter.nik}", fontSize = 12.sp, color = Blue400, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama Lengkap", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editTglLahir,
                        onValueChange = { editTglLahir = it },
                        label = { Text("Tanggal Lahir (YYYY-MM-DD)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editKawin,
                        onValueChange = { editKawin = it },
                        label = { Text("Status Perkawinan (Kawin / Belum / Pernah)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAlamat,
                        onValueChange = { editAlamat = it },
                        label = { Text("Alamat Dusun", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editRt,
                            onValueChange = { editRt = it },
                            label = { Text("RT", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editRw,
                            onValueChange = { editRw = it },
                            label = { Text("RW", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = editDisabilitas,
                        onValueChange = { editDisabilitas = it },
                        label = { Text("Ragam Disabilitas (TIDAK / Fisik / Netra / dll)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updates = VoterUpdatesPayload(
                            namaLengkap = editNama,
                            tanggalLahir = editTglLahir,
                            statusPerkawinan = editKawin,
                            alamat = editAlamat,
                            rt = editRt,
                            rw = editRw,
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
                        Text("Simpan Perbaikan Data", color = White)
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
                    "RT03" to "RT 03",
                    "RT04" to "RT 04",
                    "RT05" to "RT 05",
                    "RT06" to "RT 06"
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
                            selectedBorderColor = Blue400,
                            borderColor = Slate800
                        )
                    )
                }
            }

            // Snackbar Info message
            if (snackbarMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Blue950,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = snackbarMessage!!, fontSize = 12.sp, color = Blue300, modifier = Modifier.weight(1f))
                        IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Result count & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan: ${filteredVoters.size} Pemilih",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
                IconButton(onClick = { loadVoters(forceNetwork = true) }, modifier = Modifier.size(28.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Blue400, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Blue400, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Voter Card List
            if (filteredVoters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak Ada Data Pemilih Ditemukan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
                        Spacer(modifier = Modifier.height(4.dp))
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
        modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "${voter.tempatLahir ?: "TEGAL"}, ${voter.tanggalLahir ?: "-"} (${voter.usia ?: "-"} Th) • ${voter.statusPerkawinan ?: "Kawin"}",
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

                // Tombol 3: Ubah Data
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
