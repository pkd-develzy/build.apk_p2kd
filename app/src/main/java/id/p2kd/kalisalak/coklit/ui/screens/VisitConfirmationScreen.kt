package id.p2kd.kalisalak.coklit.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitConfirmationScreen(
    rumahId: String,
    verifikasiList: List<MemberVerificationPayload>,
    onBack: () -> Unit,
    onSuccess: (isOffline: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(true) }
    var rumah by remember { mutableStateOf<RumahItem?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form inputs
    var namaStikerManual by remember { mutableStateOf("") }
    var stikerDitempel by remember { mutableStateOf(true) }
    var catatanPetugas by remember { mutableStateOf("") }

    LaunchedEffect(rumahId) {
        coroutineScope.launch {
            try {
                val res = ApiClient.api.getTasks()
                if (res.isSuccessful && res.body()?.success == true) {
                    rumah = res.body()?.rumahList?.find { it.id == rumahId }
                }
            } catch (e: Exception) {
                // If offline, can still proceed
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Kunjungan Coklit", fontWeight = FontWeight.Bold) },
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ringkasan Rumah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Alamat: ${rumah?.alamat ?: "-"}")
                            Text("RT ${rumah?.rt ?: "-"} / RW ${rumah?.rw ?: "-"} • Dusun: ${rumah?.dusun ?: "-"}")
                            Text("Anggota Terverifikasi: ${verifikasiList.size} orang")
                        }
                    }

                    // Notice regarding manual handwriting on physical sticker
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "PERINGATAN ARSITEKTUR RESMI:\nNama penghuni pada stiker fisik ditulis manual oleh petugas menggunakan pulpen saat Coklit. Sistem mencatat nama yang Anda tuliskan di bawah ini sebagai riwayat kontrol.",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = namaStikerManual,
                        onValueChange = { namaStikerManual = it },
                        label = { Text("Nama Penghuni Ditulis pada Stiker (Wajib)") },
                        placeholder = { Text("Contoh: Bpk. Slamet / Ibu Siti") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = stikerDitempel,
                            onCheckedChange = { stikerDitempel = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stiker Fisik Telah Ditempel di Pintu/Dinding Rumah", fontSize = 14.sp)
                    }

                    OutlinedTextField(
                        value = catatanPetugas,
                        onValueChange = { catatanPetugas = it },
                        label = { Text("Catatan Lapangan Kunjungan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (namaStikerManual.isBlank()) {
                                errorMessage = "Harap masukkan nama penghuni yang dituliskan pada stiker"
                            } else {
                            isSubmitting = true
                            errorMessage = null

                            val payload = SubmitVisitRequest(
                                qrToken = rumah?.qrToken ?: "",
                                namaStikerManual = namaStikerManual.trim(),
                                catatanKunjungan = catatanPetugas.trim().ifEmpty { null },
                                stikerDitempel = stikerDitempel,
                                verifikasiAnggota = verifikasiList,
                                idempotencyKey = UUID.randomUUID().toString()
                            )

                            coroutineScope.launch {
                                try {
                                    val res = ApiClient.api.submitVisit(rumahId, payload)
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        onSuccess(false) // Direct online success
                                    } else {
                                        // Save to offline queue if server error
                                        val offlineItem = OfflineQueueItem(
                                            localId = UUID.randomUUID().toString(),
                                            idempotencyKey = payload.idempotencyKey ?: UUID.randomUUID().toString(),
                                            qrToken = rumah?.qrToken ?: "",
                                            rumahData = RumahRequest(
                                                qrToken = rumah?.qrToken ?: "",
                                                alamat = rumah?.alamat ?: "",
                                                rt = rumah?.rt ?: "01",
                                                rw = rumah?.rw ?: "01"
                                            ),
                                            kks = emptyList(),
                                            visitData = payload,
                                            syncState = SyncState.SAVED_LOCAL
                                        )
                                        OfflineQueueManager(context).enqueue(offlineItem)
                                        onSuccess(true)
                                    }
                                } catch (e: Exception) {
                                    val offlineItem = OfflineQueueItem(
                                        localId = UUID.randomUUID().toString(),
                                        idempotencyKey = payload.idempotencyKey ?: UUID.randomUUID().toString(),
                                        qrToken = rumah?.qrToken ?: "",
                                        rumahData = RumahRequest(
                                            qrToken = rumah?.qrToken ?: "",
                                            alamat = rumah?.alamat ?: "",
                                            rt = rumah?.rt ?: "01",
                                            rw = rumah?.rw ?: "01"
                                        ),
                                        kks = emptyList(),
                                        visitData = payload,
                                        syncState = SyncState.SAVED_LOCAL
                                    )
                                    OfflineQueueManager(context).enqueue(offlineItem)
                                    onSuccess(true)
                                } finally {
                                    isSubmitting = false
                                }
                            }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Kirim Hasil Kunjungan Coklit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
