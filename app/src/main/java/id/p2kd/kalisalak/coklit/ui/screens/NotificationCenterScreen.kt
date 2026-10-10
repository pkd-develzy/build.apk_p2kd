package id.p2kd.kalisalak.coklit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.DeleteNotificationRequest
import id.p2kd.kalisalak.coklit.data.models.NotificationItem
import id.p2kd.kalisalak.coklit.data.update.AppUpdateManager
import id.p2kd.kalisalak.coklit.data.update.UpdateInfo
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notificationList by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val installedVersion = remember { AppUpdateManager.getInstalledVersion(context) }

    fun loadNotifications() {
        coroutineScope.launch {
            try {
                val up = AppUpdateManager.checkForUpdate(context)
                updateInfo = up
            } catch (_: Exception) {}
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = ApiClient.api.getNotifications()
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()!!
                    notificationList = body.notifications
                    unreadCount = body.unreadCount
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat notifikasi."
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Kesalahan koneksi jaringan."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val filteredList = remember(notificationList, selectedFilter) {
        when (selectedFilter) {
            "TUGAS" -> notificationList.filter { it.category?.contains("TUGAS", ignoreCase = true) == true || it.category?.contains("COKLIT", ignoreCase = true) == true }
            "DARURAT" -> notificationList.filter { it.category?.contains("DARURAT", ignoreCase = true) == true || it.category?.contains("PERINGATAN", ignoreCase = true) == true || it.category?.contains("PENTING", ignoreCase = true) == true }
            "SISTEM" -> notificationList.filter { it.category?.contains("SISTEM", ignoreCase = true) == true || it.category?.contains("UPDATE", ignoreCase = true) == true }
            else -> notificationList
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(RoyalSapphireDark, DarkNavy)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Pusat Notifikasi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = White
                                )
                                if (unreadCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AmberVibrant,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        Text(
                                            text = "$unreadCount Baru",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Instruksi & Pengumuman Petugas P2KD",
                                fontSize = 11.sp,
                                color = Slate300
                            )
                        }
                    }

                    if (notificationList.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Bersihkan Semua",
                                tint = White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = BgPearl
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp)
        ) {
            // 1. SMART UPDATE BANNER: PEMBARUAN APLIKASI
            item {
                if (updateInfo?.hasUpdate == true) {
                    val targetApkUrl = updateInfo?.downloadUrl ?: "https://github.com/pkd-develzy/build.apk_p2kd/releases/latest"
                    val latestVer = updateInfo?.newVersion ?: "Terbaru"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldVibrant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pembaruan Versi Tersedia!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "v$installedVersion ➔ v$latestVer (Resmi)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldVibrant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tersedia pembaruan performa Coklit, sinkronisasi offline, dan perbaikan antarmuka. Unduh APK resmi sekarang.",
                                fontSize = 12.sp,
                                color = TextMain,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetApkUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unduh APK Resmi (PETUGAS_P2KD.apk)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = White)
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalSapphireBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldVibrant, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Aplikasi Anda menggunakan versi terbaru (v$installedVersion)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = RoyalSapphireDark
                            )
                        }
                    }
                }
            }

            // 2. FILTER PILLS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filterChips = listOf(
                        "SEMUA" to "Semua",
                        "TUGAS" to "Instruksi Coklit",
                        "DARURAT" to "Peringatan",
                        "SISTEM" to "Pembaruan"
                    )
                    items(filterChips) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) RoyalSapphireBright else PureWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) RoyalSapphireBright else CardBorderSubtle
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { selectedFilter = key }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) White else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // 3. DAFTAR NOTIFIKASI
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoyalSapphireBright, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CoralSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(errorMessage!!, color = CoralAccent, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { loadNotifications() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalSapphireBright),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Coba Lagi", fontSize = 12.sp, color = White)
                            }
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RoyalSapphireSoft,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = RoyalSapphireBright,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum Ada Notifikasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextMain
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Semua instruksi dan pembaruan dari Admin P2KD akan muncul di sini.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList) { notif ->
                    val isDarurat = notif.category?.contains("DARURAT", ignoreCase = true) == true ||
                            notif.category?.contains("PERINGATAN", ignoreCase = true) == true ||
                            notif.category?.contains("PENTING", ignoreCase = true) == true
                    val isTugas = notif.category?.contains("TUGAS", ignoreCase = true) == true ||
                            notif.category?.contains("COKLIT", ignoreCase = true) == true
                    val isSistem = notif.category?.contains("SISTEM", ignoreCase = true) == true ||
                            notif.category?.contains("UPDATE", ignoreCase = true) == true

                    val cardBg = when {
                        isDarurat -> AmberSoft
                        isTugas -> RoyalSapphireSoft
                        isSistem -> EmeraldSoft
                        else -> PureWhite
                    }
                    val leftBorderColor = when {
                        isDarurat -> AmberAccent
                        isTugas -> RoyalSapphireBright
                        isSistem -> EmeraldVibrant
                        else -> RoyalSapphireLight
                    }
                    val categoryTagColor = when {
                        isDarurat -> AmberAccent
                        isTugas -> RoyalSapphireBright
                        isSistem -> EmeraldVibrant
                        else -> TextMuted
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(14.dp))
                            .clickable { onNotificationClick(notif) },
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSubtle)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Left Accent Indicator Strip
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .background(leftBorderColor)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x18000000)
                                    ) {
                                        Text(
                                            text = notif.category ?: "PENGUMUMAN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryTagColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = notif.timestamp ?: "",
                                            fontSize = 11.sp,
                                            color = TextSubtle
                                        )
                                        if (!notif.read) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(RoyalSapphireBright)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextMain,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.body,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 17.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )


                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Bersihkan Notifikasi", fontWeight = FontWeight.Bold, color = TextMain) },
            text = { Text("Apakah Anda yakin ingin menghapus semua notifikasi ini dari perangkat Anda?", fontSize = 13.sp, color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        notificationList = emptyList()
                        unreadCount = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Bersihkan", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
