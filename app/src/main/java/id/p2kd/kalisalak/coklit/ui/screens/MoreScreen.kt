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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
    sessionManager: EncryptedSessionManager,
    offlineQueue: OfflineQueueManager,
    onNavigateToSync: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val user = remember { sessionManager.getUserProfile() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(sessionManager.getServerUrl()) }
    val queueCount = remember { offlineQueue.getQueue().size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Officer Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user?.nama ?: "Petugas P2KD",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@ â€¢ ",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Wilayah: TPS  â€¢ ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 2. Navigation Actions Group
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.Sync,
                    title = "Riwayat Sinkronisasi & Offline",
                    subtitle = " antrean tersimpan lokal",
                    onClick = onNavigateToSync
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                MoreMenuItem(
                    icon = Icons.Default.Dns,
                    title = "Pengaturan Server API",
                    subtitle = sessionManager.getServerUrl(),
                    onClick = { showServerDialog = true }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                MoreMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "Regulasi & Tahapan Pilkades",
                    subtitle = "SK P2KD Kalisalak 2026",
                    onClick = { showInfoDialog = true }
                )
            }
        }

        // 3. Security & Logout Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.ExitToApp,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Keluar Sesi & Bersihkan Token",
                    subtitle = "Mencabut token perangkat dan mengakhiri sesi kerja",
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App Version Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "P2KD Coklit Mobile v1.1.0 (Production Release)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Desa Kalisalak, Kec. Margasari, Kab. Tegal",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    // Dialog Konfirmasi Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Apakah Anda yakin ingin keluar? Token sesi dan token push notification akan dicabut secara aman dari backend.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            try {
                                ApiClient.api.revokeDeviceToken()
                                ApiClient.api.logout()
                            } catch (_: Exception) {}
                            sessionManager.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Pengaturan Server
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Pengaturan URL Server") },
            text = {
                Column {
                    Text("URL Endpoint API Backend P2KD:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { newVal: String -> serverUrlInput = newVal },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionManager.setServerUrl(serverUrlInput.trim())
                        showServerDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Regulasi & Tahapan
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Tahapan Pemilih Pilkades") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. DP4 â€” Data Awal Pemerintah", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("2. Bahan Coklit â€” Hasil Sinkronisasi Lapangan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("3. DPS â€” Daftar Pemilih Sementara", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("4. DPS Tambahan â€” Usulan Warga Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("5. DPSHP â€” Hasil Perbaikan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("6. DPSHP Akhir â€” Finalisasi Pleno", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("7. DPT â€” Daftar Pemilih Tetap Resmi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Prinsip Utama: 1 QR Code C6 = 1 Rumah Fisik = Banyak KK = Banyak Anggota Keluarga. Nama penghuni pada stiker fisik ditulis manual oleh petugas menggunakan pena.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}