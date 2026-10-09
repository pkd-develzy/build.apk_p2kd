package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import id.p2kd.kalisalak.coklit.data.models.ActivitiesSummary
import id.p2kd.kalisalak.coklit.data.models.ActivityItem
import kotlinx.coroutines.launch

@Composable
fun ActivityHubScreen(
    onNavigateToTasks: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("SEMUA") } // SEMUA | KUNJUNGAN | ADUAN | PENGUMUMAN
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activityList by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var summary by remember { mutableStateOf(ActivitiesSummary()) }

    fun loadActivities() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = ApiClient.api.getActivities(
                    category = if (selectedCategory == "SEMUA") null else selectedCategory,
                    limit = 50
                )
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()!!
                    activityList = body.data
                    summary = body.summary
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat aktivitas."
                }
            } catch (e: Exception) {
                errorMessage = "Gagal memuat: "
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        loadActivities()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Summary Cards Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pusat Aktivitas & Pemantauan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(label = "Kunjungan", value = "")
                    SummaryMetric(label = "Aduan", value = "")
                    SummaryMetric(label = "Pengumuman", value = "")
                    summary.tasks?.let { t ->
                        SummaryMetric(label = "Rumah Coklit", value = "/")
                    }
                }
            }
        }

        // 2. Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = when (selectedCategory) {
                "SEMUA" -> 0
                "KUNJUNGAN" -> 1
                "ADUAN" -> 2
                "PENGUMUMAN" -> 3
                else -> 0
            },
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            val categories = listOf("SEMUA" to "Semua", "KUNJUNGAN" to "Kunjungan Lapangan", "ADUAN" to "Aduan Warga", "PENGUMUMAN" to "Pengumuman")
            categories.forEach { (key, label) ->
                val isSel = selectedCategory == key
                Tab(
                    selected = isSel,
                    onClick = { selectedCategory = key },
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

        // 3. Activity Items List
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Memuat data aktivitas...", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadActivities() }) {
                            Text("Muat Ulang")
                        }
                    }
                }
                activityList.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada catatan aktivitas", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activityList, key = { it.id }) { item ->
                            ActivityCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActivityCard(item: ActivityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            val (icon, tintBg, tintColor) = when (item.kategori) {
                "KUNJUNGAN" -> Triple(Icons.Default.Home, Color(0xFFE8F5E9), Color(0xFF2E7D32))
                "ADUAN" -> Triple(Icons.Default.Warning, Color(0xFFFFF3E0), Color(0xFFE65100))
                "PENGUMUMAN" -> Triple(Icons.Default.Campaign, Color(0xFFE3F2FD), Color(0xFF1565C0))
                else -> Triple(Icons.Default.Info, Color(0xFFECEFF1), Color(0xFF455A64))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.judul,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tintBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = item.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tintColor)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.deskripsi,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Oleh: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatWaktu(item.waktu),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

fun formatWaktu(waktuStr: String): String {
    if (waktuStr.isBlank()) return "-"
    return try {
        waktuStr.take(16).replace("T", " ")
    } catch (_: Exception) {
        waktuStr
    }
}