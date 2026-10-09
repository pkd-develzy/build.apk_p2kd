package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.AnggotaKeluargaData
import id.p2kd.kalisalak.coklit.data.models.MemberVerificationPayload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberListScreen(
    rumahId: String,
    kkId: String,
    noKk: String,
    onBack: () -> Unit,
    onVerificationChanged: (List<MemberVerificationPayload>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<AnggotaKeluargaData>>(emptyList()) }

    // Dialog state for editing verification of an individual
    var selectedMember by remember { mutableStateOf<AnggotaKeluargaData?>(null) }
    var editStatus by remember { mutableStateOf("SESUAI") }
    var editCatatan by remember { mutableStateOf("") }
    var editNama by remember { mutableStateOf("") }
    var editTps by remember { mutableStateOf("") }

    fun refreshMembers() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val res = ApiClient.api.getTasks()
                if (res.isSuccessful && res.body()?.success == true) {
                    val house = res.body()?.data?.find { it.id == rumahId }
                    val kk = house?.kartu_keluarga?.find { it.id == kkId }
                    if (kk != null) {
                        members = kk.anggota
                    } else {
                        errorMessage = "Data KK tidak ditemukan pada rumah ini"
                    }
                } else {
                    errorMessage = "Gagal memuat data anggota"
                }
            } catch (e: Exception) {
                errorMessage = "Gagal memuat: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(kkId) {
        refreshMembers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Anggota KK: $noKk", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Verifikasi Per-Individu Sesuai Kondisi Riil",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { refreshMembers() }) {
                        Text("Coba Lagi")
                    }
                }
            } else if (members.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada data pemilih terdaftar pada KK ini.\nSilakan periksa kembali nomor KK.",
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(members) { item ->
                        val statusColor = when (item.verifikasi_status) {
                            "SESUAI" -> Color(0xFF2E7D32)
                            "UBAH_DATA" -> Color(0xFFE65100)
                            "TMS" -> Color(0xFFC62828)
                            else -> Color(0xFF757575)
                        }

                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.nama,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "NIK: ${item.nik}",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Kelamin: ${item.jenis_kelamin ?: "-"} • Usia: ${item.usia ?: "-"} thn",
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "TPS: ${item.tps ?: "-"} • Status: ${item.status_kependudukan ?: "DPS"}",
                                            fontSize = 13.sp
                                        )
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                                    ) {
                                        Text(
                                            text = item.verifikasi_status ?: "BELUM_DITEMUI",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }

                                if (!item.verifikasi_catatan.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Catatan: ${item.verifikasi_catatan}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedMember = item
                                            editStatus = item.verifikasi_status ?: "SESUAI"
                                            editCatatan = item.verifikasi_catatan ?: ""
                                            editNama = item.nama
                                            editTps = item.tps ?: ""
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ubah Status Coklit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedMember != null) {
        val currentTarget = selectedMember!!
        AlertDialog(
            onDismissRequest = { selectedMember = null },
            title = { Text("Verifikasi: ${currentTarget.nama}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih Hasil Verifikasi di Lapangan:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    val options = listOf(
                        "SESUAI" to "Sesuai Data Pemilih",
                        "UBAH_DATA" to "Ada Perubahan Data",
                        "TMS" to "Tidak Memenuhi Syarat (Meninggal/Pindah)",
                        "BELUM_DITEMUI" to "Belum Dapat Ditemui"
                    )

                    options.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (editStatus == key),
                                onClick = { editStatus = key }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }

                    if (editStatus == "UBAH_DATA") {
                        OutlinedTextField(
                            value = editNama,
                            onValueChange = { editNama = it },
                            label = { Text("Koreksi Nama") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = editCatatan,
                        onValueChange = { editCatatan = it },
                        label = { Text("Catatan Verifikasi (Opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Update in local state
                        members = members.map { m ->
                            if (m.id == currentTarget.id) {
                                m.copy(
                                    verifikasi_status = editStatus,
                                    verifikasi_catatan = editCatatan.ifEmpty { null }
                                )
                            } else m
                        }

                        // Prepare payload list for parent/sync
                        val payloadList = members.map { m ->
                            MemberVerificationPayload(
                                pemilih_id = m.id,
                                status = m.verifikasi_status ?: "BELUM_DITEMUI",
                                catatan = m.verifikasi_catatan,
                                perbaikan_data = if (m.verifikasi_status == "UBAH_DATA") {
                                    mapOf("nama" to editNama)
                                } else null
                            )
                        }
                        onVerificationChanged(payloadList)
                        selectedMember = null
                    }
                ) {
                    Text("Simpan Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMember = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
