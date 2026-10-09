package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import id.p2kd.kalisalak.coklit.data.models.BatchSyncRequest
import id.p2kd.kalisalak.coklit.data.models.QueueItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val queueManager = remember { OfflineQueueManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var queueList by remember { mutableStateOf(queueManager.getQueue()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        queueList = queueManager.getQueue()
    }

    fun syncAll() {
        val pending = queueList.filter { it.status == "PENDING_SYNC" || it.status == "ERROR" }
        if (pending.isEmpty()) {
            syncMessage = "Tidak ada data antrean yang perlu disinkronkan."
            return
        }

        isSyncing = true
        syncMessage = null
        coroutineScope.launch {
            try {
                val req = BatchSyncRequest(
                    items = pending.map { it.payload }
                )
                val res = ApiClient.api.syncBatch(req)
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()
                    syncMessage = "Sinkronisasi berhasil! Tersimpan: ${body?.synced_count}, Gagal: ${body?.errors?.size ?: 0}"
                    // Mark as synced
                    pending.forEach {
                        queueManager.updateStatus(it.id, "SYNCED")
                    }
                    refreshList()
                } else {
                    syncMessage = "Gagal sinkron: ${res.body()?.message ?: "Server menolak data"}"
                    pending.forEach {
                        queueManager.updateStatus(it.id, "ERROR", res.body()?.message)
                    }
                    refreshList()
                }
            } catch (e: Exception) {
                syncMessage = "Koneksi gagal: ${e.localizedMessage}"
                pending.forEach {
                    queueManager.updateStatus(it.id, "ERROR", e.localizedMessage)
                }
                refreshList()
            } finally {
                isSyncing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Sinkronisasi Offline", fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            Surface(tonalElevation = 6.dp) {
                Button(
                    onClick = { syncAll() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sedang Menyinkronkan...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinkronkan Semua Sekarang")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (syncMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (queueList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Antrean lokal kosong.\nSemua data Coklit tersinkronkan langsung ke server.",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(queueList) { item ->
                            val statusColor = when (item.status) {
                                "SYNCED" -> Color(0xFF2E7D32)
                                "ERROR" -> Color(0xFFC62828)
                                "PENDING_SYNC" -> Color(0xFFE65100)
                                else -> Color.Gray
                            }

                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Rumah: ${item.rumah_id.take(8)}...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            color = statusColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = item.status,
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Stiker: ${item.payload.nama_stiker_manual} (${if (item.payload.stiker_ditempel) "Ditempel" else "Belum Ditempel"})",
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Anggota: ${item.payload.verifikasi_anggota.size} orang diverifikasi",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.created_at))
                                    Text(
                                        text = "Waktu Catat: $dateStr",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    if (item.error_message != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Error: ${item.error_message}",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
