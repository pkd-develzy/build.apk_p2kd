package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.RumahItem
import id.p2kd.kalisalak.coklit.data.models.RumahRequest
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseFormScreen(
    qrToken: String,
    qrId: String = "",
    existingRumah: RumahItem? = null,
    onHouseSaved: ((RumahItem) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    onSuccess: ((RumahItem) -> Unit)? = onHouseSaved,
    onBack: (() -> Unit)? = onNavigateBack
) {
    val context = LocalContext.current
    val sessionManager = remember { ApiClient.getSessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val userProfile = sessionManager.getUserProfile()
    val defaultRw = (userProfile?.assignedRw?.replace(Regex("\\D"), "") ?: "01").padStart(2, '0')

    var alamat by remember { mutableStateOf(existingRumah?.alamat ?: "") }
    var rt by remember { mutableStateOf(existingRumah?.rt ?: "01") }
    var rw by remember { mutableStateOf(existingRumah?.rw ?: defaultRw) }
    var nomorRumah by remember { mutableStateOf(existingRumah?.nomorRumah ?: "") }
    var keteranganLokasi by remember { mutableStateOf(existingRumah?.keteranganLokasi ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (existingRumah != null) "Perbarui Identitas Rumah" else "Identifikasi Fisik Rumah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate100
                        )
                        Text(
                            text = "1 Rumah Bisa Berisi Banyak KK",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() ?: onNavigateBack?.invoke() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Slate100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card: Token QR Rumah
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy800),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Token QR Rumah Resmi:", color = Slate400, fontSize = 11.sp)
                        Text(
                            text = qrToken,
                            color = Cyan300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Form Inputs
            OutlinedTextField(
                value = alamat,
                onValueChange = { alamat = it },
                label = { Text("Alamat / Jalan / Gang (Wajib)") },
                placeholder = { Text("Contoh: Jl. Lapangan RT 01 RW 01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rt,
                    onValueChange = { rt = it },
                    label = { Text("RT") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rw,
                    onValueChange = { rw = it },
                    label = { Text("RW") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = nomorRumah,
                onValueChange = { nomorRumah = it },
                label = { Text("Nomor Rumah (Opsional)") },
                placeholder = { Text("Contoh: No. 12B") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = keteranganLokasi,
                onValueChange = { keteranganLokasi = it },
                label = { Text("Patokan / Keterangan Lokasi (Opsional)") },
                placeholder = { Text("Contoh: Depan mushola / samping warung") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (alamat.isBlank()) {
                        errorMessage = "Alamat fisik rumah wajib diisi"
                    } else {
                    isLoading = true
                    errorMessage = null

                    val req = RumahRequest(
                        qrToken = qrToken,
                        alamat = alamat.trim(),
                        rt = rt.trim().ifEmpty { "01" },
                        rw = rw.trim().ifEmpty { "01" },
                        nomorRumah = nomorRumah.trim().ifEmpty { null },
                        keteranganLokasi = keteranganLokasi.trim().ifEmpty { null }
                    )

                    coroutineScope.launch {
                        try {
                            val res = ApiClient.api.registerRumah(req)
                            if (res.isSuccessful && res.body()?.success == true) {
                                val saved = res.body()!!.rumah ?: RumahItem(
                                    id = qrId.ifEmpty { "rumah_" },
                                    qrToken = qrToken,
                                    alamat = alamat,
                                    rt = rt,
                                    rw = rw
                                )
                                onSuccess?.invoke(saved) ?: onHouseSaved?.invoke(saved)
                            } else {
                                errorMessage = res.body()?.message ?: "Gagal menyimpan data rumah"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Koneksi gagal: ${e.message ?: "Periksa koneksi internet."}"
                        } finally {
                            isLoading = false
                        }
                    }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Identitas Rumah & Lanjut ke KK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

