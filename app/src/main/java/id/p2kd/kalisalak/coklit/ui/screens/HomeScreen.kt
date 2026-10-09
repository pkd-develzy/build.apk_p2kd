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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                stikerTersedia = 0
            )
        )
    }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshTasks() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val api = ApiClient.getService(sessionManager)
                val res = api.getTasks(userProfile?.assignedRw, userProfile?.assignedTps)
                if (res.isSuccessful && res.body()?.success == true) {
                    summary = res.body()!!.summary
                }
            } catch (_: Exception) {
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTasks()
    }

    Scaffold(
        containerColor = Navy950,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Petugas Coklit Lapangan",
                        style = MaterialTheme.typography.labelSmall,
                        color = Blue400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userProfile?.nama ?: "Petugas P2KD",
                        style = MaterialTheme.typography.titleLarge,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Navy800, CircleShape)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Profil", tint = Amber400)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Wilayah Penugasan Card
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue900.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WILAYAH BINAAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber400,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = userProfile?.assignedTps ?: "Tabung Pemilihan",
                            style = MaterialTheme.typography.titleMedium,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kalisalak • ${userProfile?.assignedRw ?: "Semua RW"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }

                    Surface(
                        color = Blue600.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.4f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Blue400)
                        }
                    }
                }
            }

            // 2. Tombol Utama: PINDAI QR RUMAH (Kamera Belakang CameraX)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToScan() }
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Emerald600, Blue600, Blue800)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "1 QR = 1 RUMAH",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pindai QR Rumah",
                                style = MaterialTheme.typography.headlineMedium,
                                color = White,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Buka Kamera Belakang Langsung",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate200
                            )
                        }

                        Surface(
                            color = White,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai QR",
                                    tint = Blue900,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Ringkasan Status Coklit
            Text(
                text = "Kemajuan Coklit Wilayah",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Rumah",
                    count = summary.totalRumah,
                    icon = Icons.Default.Home,
                    accentColor = Blue400,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Selesai",
                    count = summary.selesaiRumah,
                    icon = Icons.Default.CheckCircle,
                    accentColor = Emerald500,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Perlu Tindak Lanjut",
                    count = summary.perluFollowUp,
                    icon = Icons.Default.Warning,
                    accentColor = Amber400,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Stiker Siap Pakai",
                    count = summary.stikerTersedia,
                    icon = Icons.Default.ConfirmationNumber,
                    accentColor = Slate300,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Menu Cepat: Daftar Tugas & Sinkronisasi Offline
            Text(
                text = "Manajemen & Antrean Data",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionMenuCard(
                    title = "Daftar Rumah",
                    subtitle = "Tinjau pendataan",
                    icon = Icons.Default.FormatListBulleted,
                    onClick = onNavigateToTasks,
                    modifier = Modifier.weight(1f)
                )

                ActionMenuCard(
                    title = "Antrean Offline",
                    subtitle = if (pendingSyncCount > 0) "$pendingSyncCount antrean belum kirim" else "Semua tersinkron",
                    icon = Icons.Default.Sync,
                    badgeCount = if (pendingSyncCount > 0) pendingSyncCount else null,
                    onClick = onNavigateToSync,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Navy900,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActionMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Navy900,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Blue900.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Blue400, modifier = Modifier.size(20.dp))
                    }
                }

                if (badgeCount != null) {
                    Surface(
                        color = Amber500,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = badgeCount.toString(),
                                color = Navy950,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                maxLines = 1
            )
        }
    }
}
