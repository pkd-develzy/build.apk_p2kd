package id.p2kd.kalisalak.coklit.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.DeleteNotificationRequest
import id.p2kd.kalisalak.coklit.data.models.NotificationItem
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notificationList by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    fun loadNotifications() {
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

    // Confirmation Dialog for Delete All
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Hapus Semua Notifikasi", fontWeight = FontWeight.Bold, color = White) },
            text = { Text("Apakah Anda yakin ingin menghapus seluruh riwayat notifikasi? Notifikasi yang dihapus tidak akan muncul kembali.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = { deleteAllNotifs() },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Hapus Semua", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    Scaffold(
        containerColor = Slate950,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pusat Notifikasi", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = White)
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Rose600
                            ) {
                                Text(
                                    text = "$unreadCount Baru",
                                    color = White,
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = White)
                    }
                },
                actions = {
                    if (notificationList.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Semua", tint = Rose400)
                        }
                    }
                    IconButton(onClick = { loadNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan", tint = Blue400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
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
            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                val filters = listOf(
                    "SEMUA" to "Semua",
                    "PENGUMUMAN" to "Pengumuman",
                    "ADUAN" to "Aduan",
                    "SINKRONISASI" to "Pembaruan"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue600,
                            selectedLabelColor = White,
                            containerColor = Navy900,
                            labelColor = Slate300
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = Blue400,
                            borderColor = Slate800
                        )
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue400)
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
                            color = Navy900,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Semua Notifikasi Bersih",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tidak ada pemberitahuan baru saat ini. Notifikasi yang dihapus tidak akan muncul kembali.",
                            fontSize = 12.sp,
                            color = Slate400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        "PENGUMUMAN" -> Blue500
        "ADUAN" -> Amber500
        "SINKRONISASI" -> Emerald500
        else -> Indigo500
    }

    Surface(
        color = if (item.read) Navy900 else Navy800,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.read) Slate800 else Blue900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
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
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatRelativeTime(item.timestamp),
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Slate500, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontSize = 12.sp,
                    color = Slate300,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatRelativeTime(isoString: String): String {
    return try {
        // Simplified friendly time
        if (isoString.contains("T")) {
            val timePart = isoString.substringAfter("T").take(5)
            "$timePart WIB"
        } else {
            "Baru saja"
        }
    } catch (_: Exception) {
        "Hari ini"
    }
}
