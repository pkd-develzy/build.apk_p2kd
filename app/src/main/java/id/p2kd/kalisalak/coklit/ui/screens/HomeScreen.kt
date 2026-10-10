package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.TaskSummary
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    sessionManager: EncryptedSessionManager,
    offlineQueue: OfflineQueueManager,
    onNavigateToScan: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile = sessionManager.getUserProfile()
    val queueItems by offlineQueue.itemsFlow.collectAsState()
    val pendingSyncCount = queueItems.count { it.syncState != id.p2kd.kalisalak.coklit.data.models.SyncState.SYNCED }

    var summary by remember {
        mutableStateOf(
            TaskSummary(
                totalRumah = 0,
                selesaiRumah = 0,
                perluFollowUp = 0,
                stikerTersedia = 1300,
                totalPemilihWilayah = 7787
            )
        )
    }
    var totalCocok by remember { mutableIntStateOf(0) }
    var totalTms by remember { mutableIntStateOf(0) }
    var totalDpt by remember { mutableIntStateOf(7787) }
    var isRefreshing by remember { mutableStateOf(false) }
    var taskErrorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshTasks() {
        coroutineScope.launch {
            isRefreshing = true
            taskErrorMessage = null
            try {
                val api = ApiClient.getService(sessionManager)
                val res = api.getTasks(userProfile?.assignedRw, userProfile?.assignedTps)
                if (res.isSuccessful && res.body()?.success == true) {
                    summary = res.body()!!.summary
                    taskErrorMessage = null
                } else {
                    taskErrorMessage = res.body()?.message ?: "Gagal memuat data server"
                }

                // Also fetch voter summary
                val vRes = api.getVoters(limit = 1)
                if (vRes.isSuccessful && vRes.body()?.success == true) {
                    totalDpt = vRes.body()!!.total
                }
            } catch (e: Exception) {
                taskErrorMessage = "Koneksi terganggu: ${e.localizedMessage ?: "Silakan periksa jaringan"}"
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTasks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error Banner
        if (taskErrorMessage != null) {
            Surface(
                color = Rose600.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koneksi Database",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = taskErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = White
                        )
                    }
                    Button(
                        onClick = { refreshTasks() },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Coba Lagi", fontSize = 12.sp, color = White)
                    }
                }
            }
        }

        // 1. HERO CARD: Identitas Petugas
        Surface(
            color = Navy900,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Blue900.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToProfile() }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Officer Avatar / Photo
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = Blue950,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Blue400),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (!userProfile?.fotoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = userProfile?.fotoUrl,
                                            contentDescription = "Foto Petugas",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        val initials = (userProfile?.nama ?: "P").split(" ")
                                            .take(2)
                                            .mapNotNull { it.firstOrNull()?.toString() }
                                            .joinToString("")
                                            .ifBlank { "P" }
                                        Text(
                                            text = initials,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = White
                                        )
                                    }
                                }
                            }
                            // Online indicator dot
                            Surface(
                                shape = CircleShape,
                                color = Emerald500,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Navy900),
                                modifier = Modifier.size(14.dp)
                            ) {}
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Selamat Bertugas,",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userProfile?.nama ?: "Petugas P2KD",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = "${userProfile?.jabatan ?: "Petugas Pantarlih"} • ${userProfile?.assignedRw ?: "Desa Kalisalak"}",
                                fontSize = 12.sp,
                                color = Blue400,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { refreshTasks() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Navy800, CircleShape)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Blue400, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Blue400, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Tag & Wilayah Binaan
                Surface(
                    color = Blue950.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue800.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Status: Aktif Bertugas", fontSize = 11.sp, color = Emerald400, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = userProfile?.assignedTps ?: "P2KD Kalisalak",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }
                }
            }
        }

        // 2. LIVE KPI PROGRES COKLIT LAPANGAN
        Surface(
            color = Navy900,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = Blue400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROGRES COKLIT LAPANGAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = White
                        )
                    }

                    val pct = if (totalDpt > 0) ((totalCocok.toFloat() / totalDpt) * 100).toInt() else 0
                    Surface(
                        color = Blue600.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$pct% Selesai",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                val progressFraction = if (totalDpt > 0) (totalCocok.toFloat() / totalDpt.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Emerald500,
                    trackColor = Slate800
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalCocok dari $totalDpt Warga",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    Text(
                        text = "${totalDpt - totalCocok} Belum Dicoklit",
                        fontSize = 11.sp,
                        color = Amber400
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Kotak Metrik Interaktif
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricBox(
                        title = "Total DPT",
                        count = totalDpt.toString(),
                        icon = Icons.Default.People,
                        accentColor = Blue400,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricBox(
                        title = "Cocok",
                        count = totalCocok.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricBox(
                        title = "TMS",
                        count = totalTms.toString(),
                        icon = Icons.Default.Cancel,
                        accentColor = Rose400,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricBox(
                        title = "Rumah",
                        count = summary.totalRumah.toString(),
                        icon = Icons.Default.HomeWork,
                        accentColor = Amber400,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. TOMBOL UTAMA: PINDAI QR STIKER RUMAH (Kamera Belakang)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToScan() }
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Blue600, Indigo600)
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = " AKSI UTAMA LAPANGAN ",
                                style = MaterialTheme.typography.labelSmall,
                                color = White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pindai Stiker QR Rumah",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Buka kamera untuk scan stiker fisik atau tempel stiker baru",
                            fontSize = 12.sp,
                            color = White.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        shape = CircleShape,
                        color = White.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai",
                                tint = White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. KOTAK STIKER & STATUS SINKRONISASI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTasks() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Rumah Terdata", fontSize = 11.sp, color = Slate400)
                        Icon(Icons.Default.Home, contentDescription = null, tint = Blue400, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${summary.totalRumah} Rumah",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }

            Surface(
                color = Navy900,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSync() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Sinkronisasi Cloud", fontSize = 11.sp, color = Slate400)
                        Icon(
                            if (pendingSyncCount > 0) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (pendingSyncCount > 0) Amber400 else Emerald400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (pendingSyncCount > 0) "$pendingSyncCount Menunggu" else "Otomatis Aktif",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingSyncCount > 0) Amber400 else Emerald400
                    )
                }
            }
        }
                Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun KpiMetricBox(
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate950,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Medium)
                Text(text = count, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}
