package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.KartuKeluargaData
import id.p2kd.kalisalak.coklit.data.models.LinkKkRequest
import id.p2kd.kalisalak.coklit.data.models.RumahData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KKListScreen(
    rumahId: String,
    onBack: () -> Unit,
    onSelectKk: (kkId: String, noKk: String) -> Unit,
    onProceedToVisitConfirmation: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rumah by remember { mutableStateOf<RumahData?>(null) }
    var showAddKkDialog by remember { mutableStateOf(false) }

    // Dialog state
    var inputNoKk by remember { mutableStateOf("") }
    var inputKepalaKeluarga by remember { mutableStateOf("") }
    var inputAlamatKk by remember { mutableStateOf("") }
    var isSubmittingKk by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                // Fetch latest rumah details through lookup or dedicated endpoint
                // If lookup by ID isn't directly exposed, task list or house lookup can serve
                val res = ApiClient.api.getTasks()
                if (res.isSuccessful && res.body()?.success == true) {
                    val found = res.body()?.data?.find { it.id == rumahId }
                    if (found != null) {
                        rumah = found
                    } else {
                        errorMessage = "Data rumah tidak ditemukan di penugasan aktif"
                    }
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat data keluarga"
                }
            } catch (e: Exception) {
                errorMessage = "Koneksi bermasalah: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(rumahId) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daftar KK di Rumah Ini", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "1 Rumah Bisa Berisi Banyak KK",
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddKkDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tautkan / Tambah KK") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onProceedToVisitConfirmation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lanjut ke Konfirmasi Kunjungan & Stiker")
                }
            }
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
                    Button(onClick = { refreshData() }) {
                        Text("Coba Lagi")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rumah?.alamat_fisik ?: "Alamat Rumah",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "RT ${rumah?.rt ?: "-"} / RW ${rumah?.rw ?: "-"} • Dusun: ${rumah?.dusun ?: "-"}",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Total KK Terdaftar: ${rumah?.kartu_keluarga?.size ?: 0} KK",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    val kks = rumah?.kartu_keluarga ?: emptyList()
                    if (kks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Belum ada KK yang ditautkan ke rumah ini.\nTekan tombol '+ Tautkan / Tambah KK' di bawah.",
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(kks) { kk ->
                            Card(
                                onClick = { onSelectKk(kk.id, kk.no_kk) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No. KK: ${kk.no_kk}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Kepala Keluarga: ${kk.kepala_keluarga ?: "Belum ditentukan"}",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Jumlah Anggota: ${kk.anggota_count} orang",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    if (showAddKkDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingKk) showAddKkDialog = false },
            title = { Text("Tautkan KK ke Rumah Ini") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Masukkan No. KK (16 Digit). Jika sudah ada di database, KK akan ditautkan langsung. Jika baru, akan dibuat baru.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = inputNoKk,
                        onValueChange = { if (it.length <= 16) inputNoKk = it },
                        label = { Text("Nomor Kartu Keluarga") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputKepalaKeluarga,
                        onValueChange = { inputKepalaKeluarga = it },
                        label = { Text("Nama Kepala Keluarga") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputNoKk.length >= 10) {
                            isSubmittingKk = true
                            coroutineScope.launch {
                                try {
                                    val req = LinkKkRequest(
                                        no_kk = inputNoKk.trim(),
                                        kepala_keluarga = inputKepalaKeluarga.trim().ifEmpty { null }
                                    )
                                    val res = ApiClient.api.linkKk(rumahId, req)
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        showAddKkDialog = false
                                        inputNoKk = ""
                                        inputKepalaKeluarga = ""
                                        refreshData()
                                    } else {
                                        errorMessage = res.body()?.message ?: "Gagal menautkan KK"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Gagal: ${e.localizedMessage}"
                                } finally {
                                    isSubmittingKk = false
                                }
                            }
                        }
                    },
                    enabled = !isSubmittingKk && inputNoKk.isNotBlank()
                ) {
                    if (isSubmittingKk) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Tautkan")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddKkDialog = false },
                    enabled = !isSubmittingKk
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
