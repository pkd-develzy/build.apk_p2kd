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
        // Cek info update aplikasi
        coroutineScope.launch {
            try {
                val up = AppUpdateManager.checkForUpdate(context)
                updateInfo = up
            } catch (_: Exception) {}
        }

        // Ambil daftar notifikasi resmi dari server
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
                errorMessage = e.localizedMessage ?: "Gagal menghubungi server."
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteSingleNotif(notifId: String) {
        coroutineScope.launch {
            try {
                notificationList = notificationList.filter { it.id != notifId }
                unreadCount = notificationList.count { !it.read }
                ApiClient.api.deleteNotification(DeleteNotificationRequest(id = notifId))
            } catch (_: Exception) {}
        }
    }

    fun deleteAllNotifs() {
        coroutineScope.launch {
            try {
                val allIds = notificationList.map { it.id }
                notificationList = emptyList()
                unreadCount = 0
                showDeleteAllDialog = false
                ApiClient.api.deleteNotification(DeleteNotificationRequest(deleteAll = true, ids = allIds))
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val filteredList = remember(notificationList, selectedFilter) {
        if (selectedFilter == "SEMUA") notificationList
        else notificationList.filter { it.category.equals(selectedFilter, ignoreCase = true) }
    }

    // Dialog Konfirmasi Hapus Semua
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Rose500, modifier = Modifier.size(28.dp)) },
            title = { Text("Hapus Semua Notifikasi", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Apakah Anda yakin ingin menghapus seluruh riwayat notifikasi? Notifikasi yang telah dihapus tidak akan muncul kembali.", color = Color(0xFF475569), fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { deleteAllNotifs() },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hapus Bersih", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pusat Notifikasi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Rose600
                            ) {
                                Text(
                                    text = "$unreadCount Baru",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    if (notificationList.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Semua", tint = Color(0xFFFDA4AF))
                        }
                    }
                    IconButton(onClick = { loadNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F2042)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================
            // 1. HERO CARD: PEMBARUAN APLIKASI RESMI (DESAIN INTERAKTIF)
            // =========================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = Blue600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Status Aplikasi Petugas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Versi Terpasang: v$installedVersion",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Badge Status Versi
                        val hasNewUpdate = updateInfo?.hasUpdate == true
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasNewUpdate) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hasNewUpdate) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                            )
                        ) {
                            Text(
                                text = if (hasNewUpdate) "Rilis Baru: v" + (updateInfo?.newVersion ?: "") else "Versi Terkini",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasNewUpdate) Color(0xFFB45309) else Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (updateInfo?.hasUpdate == true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tersedia pembaruan aplikasi resmi v" + (updateInfo?.newVersion ?: "") + " dengan optimalisasi mutasi 13 RW dan sistem keamanan berlapis.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val url = updateInfo?.downloadUrl ?: "https://github.com/pkd-develzy/build.apk_p2kd/releases/latest"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh Pembaruan APK Sekarang", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================
            // 2. FILTER CHIPS (TAMPILAN BERSIH & ELEGAN)
            // =========================================================
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val filters = listOf(
                    "SEMUA" to "Semua",
                    "PENTING" to "Penting",
                    "INSTRUKSI" to "Instruksi",
                    "PEMBARUAN" to "Pembaruan",
                    "ADUAN" to "Aduan"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Blue600 else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Blue600 else Color(0xFFCBD5E1)
                        ),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =========================================================
            // 3. DAFTAR KARTU NOTIFIKASI
            // =========================================================
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Blue600, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Memuat notifikasi...", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = Blue600,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Semua Notifikasi Bersih",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tidak ada pemberitahuan baru saat ini. Notifikasi khusus petugas yang dikirim oleh Admin akan muncul di sini.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { notif ->
                        NotificationCardItem(
                            item = notif,
                            onClick = { onNotificationClick(notif) },
                            onDelete = { deleteSingleNotif(notif.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (item.category.uppercase()) {
        "PENTING" -> Rose500
        "INSTRUKSI" -> Blue600
        "PEMBARUAN" -> Emerald600
        "ADUAN" -> Color(0xFFD97706)
        else -> Indigo600
    }

    val badgeBgColor = when (item.category.uppercase()) {
        "PENTING" -> Color(0xFFFEF2F2)
        "INSTRUKSI" -> Color(0xFFEFF6FF)
        "PEMBARUAN" -> Color(0xFFF0FDF4)
        "ADUAN" -> Color(0xFFFFFBEB)
        else -> Color(0xFFEEF2FF)
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Garis Aksen Vertikal Kiri
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeBgColor,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatRelativeTime(item.timestamp),
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hapus Notifikasi",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.body,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val cleanTimestamp = timestamp.substringBefore(".")
        val date = sdf.parse(cleanTimestamp) ?: return "Terkini"
        val diffMillis = System.currentTimeMillis() - date.time
        val minutes = diffMillis / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        when {
            minutes < 2 -> "Baru saja"
            minutes < 60 -> "$minutes m lalu"
            hours < 24 -> "$hours j lalu"
            days < 7 -> "$days h lalu"
            else -> java.text.SimpleDateFormat("dd MMM", java.util.Locale("id")).format(date)
        }
    } catch (_: Exception) {
        "Terkini"
    }
}
