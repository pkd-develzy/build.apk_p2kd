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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.RumahItem
import id.p2kd.kalisalak.coklit.data.models.RumahRequest
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseFormScreen(
    qrToken: String,
    existingRumah: RumahItem?,
    sessionManager: EncryptedSessionManager,
    onHouseSaved: (RumahItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile = sessionManager.getUserProfile()
    val defaultRw = (userProfile?.assignedRw?.replace(/\D/g.toRegex(), "") ?: "01").padStart(2, '0')

    var alamat by remember { mutableStateOf(existingRumah?.alamat ?: "") }
    var rt by remember { mutableStateOf(existingRumah?.rt ?: "01") }
    var rw by remember { mutableStateOf(existingRumah?.rw ?: defaultRw) }
    var nomorRumah by remember { mutableStateOf(existingRumah?.nomorRumah ?: "") }
    var keteranganLokasi by remember { mutableStateOf(existingRumah?.keteranganLokasi ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Navy950,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Identifikasi Fisik Rumah", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("1 QR Code Mewakili 1 Rumah", color = Blue400, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy950)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. QR Code Banner Badge
            Surface(
                color = Blue900.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue600.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = Blue600,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = White)
                        }
                    }

                    Column {
                        Text(
                            text = "TOKEN IDENTITAS RUMAH (VALID)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber400,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = qrToken,
                            style = MaterialTheme.typography.titleMedium,
                            color = White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "QR fisik ditempel di rumah. Nama tidak dicetak pada QR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = Rose600.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Rose500),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Rose500,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 2. Formulir Alamat & Lokasi Rumah
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Data Alamat Fisik Rumah", style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = alamat,
                        onValueChange = { alamat = it },
                        label = { Text("Alamat / Jalan / Blok Rumah") },
                        placeholder = { Text("Contoh: Jl. Lapangan RT 02") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = Blue400,
                            unfocusedBorderColor = Navy700
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = rt,
                            onValueChange = { rt = it },
                            label = { Text("RT") },
                            placeholder = { Text("01") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )

                        OutlinedTextField(
                            value = rw,
                            onValueChange = { rw = it },
                            label = { Text("RW") },
                            placeholder = { Text("01") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = nomorRumah,
                        onValueChange = { nomorRumah = it },
                        label = { Text("Nomor Rumah (Opsional)") },
                        placeholder = { Text("Contoh: No. 14 / Blok C") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )

                    OutlinedTextField(
                        value = keteranganLokasi,
                        onValueChange = { keteranganLokasi = it },
                        label = { Text("Keterangan Lokasi / Patokan (Opsional)") },
                        placeholder = { Text("Contoh: Rumah cat hijau samping masjid") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )
                }
            }

            // 3. Tombol Simpan & Lanjut ke KK
            Button(
                onClick = {
                    if (alamat.isBlank() || rt.isBlank() || rw.isBlank()) {
                        errorMessage = "Alamat, RT, dan RW wajib diisi."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            val api = ApiClient.getService(sessionManager)
                            val req = RumahRequest(
                                qrToken = qrToken,
                                alamat = alamat.trim(),
                                rt = rt.trim(),
                                rw = rw.trim(),
                                nomorRumah = nomorRumah.trim().ifBlank { null },
                                keteranganLokasi = keteranganLokasi.trim().ifBlank { null }
                            )

                            val res = api.registerRumah(req)
                            if (res.isSuccessful && res.body()?.success == true && res.body()?.rumah != null) {
                                onHouseSaved(res.body()!!.rumah!!)
                            } else {
                                errorMessage = res.body()?.message ?: "Gagal menyimpan identitas rumah."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Terjadi kesalahan: ${e.message ?: "Koneksi terputus"}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                disabled = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan...", color = White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan & Lanjutkan ke Pendataan KK", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
