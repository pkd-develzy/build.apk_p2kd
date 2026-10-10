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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.BroadcastBannerItem
import id.p2kd.kalisalak.coklit.data.models.TaskSummary
import id.p2kd.kalisalak.coklit.data.models.UserProfile
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    sessionManager: EncryptedSessionManager? = null,
    offlineQueue: OfflineQueueManager? = null,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToKkList: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRegisteredHouses: () -> Unit = {}
) {
    val context = LocalContext.current
    val actualSessionManager = remember { sessionManager ?: EncryptedSessionManager(context) }
    val actualOfflineManager = remember { offlineQueue ?: OfflineQueueManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val userProfile = remember { actualSessionManager.getUserProfile() }
    val queueItems by actualOfflineManager.queueFlow.collectAsState(initial = emptyList())
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
    var taskErrorMessage by remember { mutableStateOf<String?>(null) }
    var broadcastBanner by remember { mutableStateOf<BroadcastBannerItem?>(null) }

    fun refreshTasks() {
        coroutineScope.launch {
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

                // Ambil total pemilih resmi
                val vRes = api.getVoters(limit = 1)
                if (vRes.isSuccessful && vRes.body()?.success == true) {
                    totalDpt = vRes.body()!!.total
                }

                // Ambil banner notifikasi aktif dari dashboard
                try {
                    val bRes = api.getBroadcastBanner()
                    if (bRes.isSuccessful && bRes.body()?.hasActiveBanner == true && bRes.body()?.banner != null) {
                        broadcastBanner = bRes.body()!!.banner
                    } else {
                        broadcastBanner = null
                    }
                } catch (_: Exception) {
                    broadcastBanner = null
                }
            } catch (e: Exception) {
                taskErrorMessage = "Koneksi terganggu: ${e.localizedMessage ?: "Silakan periksa jaringan"}"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTasks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Executive Clean White
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error Banner (Jika Ada Kendala Jaringan)
        if (taskErrorMessage != null) {
            Surface(
                color = Rose600.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koneksi Sinkronisasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose600,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = taskErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Button(
                        onClick = { refreshTasks() },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Coba Lagi", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // ========================================================
        // 1. BANNER NOTIFIKASI / PENGUMUMAN BERGAMBAR (DARI DASHBOARD)
        // Otomatis tersembunyi (HIDDEN/GONE) jika tidak ada pengumuman aktif
        // ========================================================
        if (broadcastBanner != null && broadcastBanner?.isActive == true) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFF0F2042),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📢 " + (broadcastBanner?.category ?: "PENGUMUMAN RESMI"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCD34D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(
                            onClick = { broadcastBanner = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!broadcastBanner?.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = broadcastBanner?.imageUrl,
                            contentDescription = "Poster Pengumuman",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = broadcastBanner?.title ?: "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2042)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = broadcastBanner?.content ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // ========================================================
        // 2. HERO CARD: Identitas Petugas (Eksekutif Putih & Deep Navy)
        // Tombol reload manual sudah DIHAPUS TOTAL sesuai instruksi!
        // ========================================================
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 3.dp,
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
                        // Foto Profil / Avatar Petugas
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F2042),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)),
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
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            // Indikator status aktif
                            Surface(
                                shape = CircleShape,
                                color = Emerald500,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(14.dp)
                            ) {}
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Selamat Bertugas,",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userProfile?.nama ?: "Petugas P2KD",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F2042)
                            )
                            Text(
                                text = "${userProfile?.jabatan ?: "Petugas Pantarlih"} • ${userProfile?.assignedRw ?: "Desa Kalisalak"}",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Lencana Verifikasi Resmi (Pengganti tombol reload)
                    Surface(
                        color = Color(0xFF0F2042).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("P2KD Sah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wilayah Binaan & Status
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Wilayah Penugasan", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = "RW ${(userProfile?.assignedRw ?: "01").replace("RW", "").trim()} • Desa Kalisalak",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F2042)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Status: Aktif Bertugas", fontSize = 11.sp, color = Emerald600, fontWeight = FontWeight.SemiBold)
                            Text(text = "Sinkronisasi Latar Otomatis", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // ========================================================
        // 3. METRIK UTAMA PROGRES COKLIT
        // ========================================================
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan Pemutakhiran",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2042)
                    )
                    Surface(
                        color = Color(0xFF0F2042),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DPT: $totalDpt Jiwa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBoxClean(
                        title = "Selesai",
                        value = "${summary.selesai}",
                        subtitle = "Warga",
                        color = Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBoxClean(
                        title = "Belum Coklit",
                        value = "${summary.belumSelesai}",
                        subtitle = "Warga",
                        color = Amber600,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBoxClean(
                        title = "TMS",
                        value = "${summary.perluFollowUp}",
                        subtitle = "Tidak Sah",
                        color = Rose600,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ========================================================
        // 4. MENU AKSI CEPAT COKLIT LAPANGAN
        // ========================================================
        Text(
            text = "Menu Utama Petugas",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F2042)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionCardClean(
                title = "Data Pemilih",
                subtitle = "Verifikasi warga",
                icon = Icons.Default.ListAlt,
                badgeColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTasks
            )
            HomeActionCardClean(
                title = "Scan QR Rumah",
                subtitle = "Pindai stiker fisik",
                icon = Icons.Default.QrCodeScanner,
                badgeColor = Color(0xFF059669),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToScan
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionCardClean(
                title = "Rumah Terdata",
                subtitle = "Daftar stiker sah",
                icon = Icons.Default.HomeWork,
                badgeColor = Color(0xFF0F2042),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToRegisteredHouses
            )
            HomeActionCardClean(
                title = "Cloud Sync",
                subtitle = if (pendingSyncCount > 0) "$pendingSyncCount Menunggu" else "Otomatis Aktif",
                icon = if (pendingSyncCount > 0) Icons.Default.Sync else Icons.Default.CloudDone,
                badgeColor = if (pendingSyncCount > 0) Amber600 else Emerald600,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSync
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MetricBoxClean(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun HomeActionCardClean(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
                }
            }
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2042))
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}
