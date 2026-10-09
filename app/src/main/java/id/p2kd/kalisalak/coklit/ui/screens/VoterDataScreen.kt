package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import id.p2kd.kalisalak.coklit.data.models.VoterItem
import id.p2kd.kalisalak.coklit.data.models.VoterStageSummary
import kotlinx.coroutines.launch

enum class VoterGroup(val label: String, val stages: List<Pair<String, String>>) {
    SUMBER("Sumber & Persiapan", listOf("DP4" to "DP4", "BAHAN_COKLIT" to "Bahan Coklit")),
    PENDATAAN("Pendataan & Perbaikan", listOf("DPS" to "DPS", "DPS_TAMBAHAN" to "DPS Tambahan", "DPSHP" to "DPSHP")),
    FINALISASI("Finalisasi", listOf("DPSHP_AKHIR" to "DPSHP Akhir", "DPT" to "DPT"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterDataScreen(
    onVoterClick: (VoterItem) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedGroup by remember { mutableStateOf(VoterGroup.PENDATAAN) }
    var selectedStageKey by remember { mutableStateOf("DPS") }
    var selectedStatus by remember { mutableStateOf("SEMUA") } // SEMUA | AKTIF | TMS
    var searchQuery by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var voterList by remember { mutableStateOf<List<VoterItem>>(emptyList()) }
    var summary by remember { mutableStateOf(VoterStageSummary()) }
    var totalCount by remember { mutableStateOf(0) }
    var selectedVoterDetail by remember { mutableStateOf<VoterItem?>(null) }

    // Fetch data function
    fun loadVoters() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = ApiClient.api.getVoters(
                    tahap = selectedStageKey,
                    status = if (selectedStatus == "SEMUA") null else selectedStatus,
                    search = if (searchQuery.isBlank()) null else searchQuery.trim(),
                    page = 1,
                    limit = 100
                )
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()!!
                    voterList = body.data
                    summary = body.summary
                    totalCount = body.total
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat daftar pemilih."
                }
            } catch (e: Exception) {
                errorMessage = "Koneksi bermasalah: "
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStageKey, selectedStatus) {
        loadVoters()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Group Selector Tabs (3 Visual Groups)
        TabRow(
            selectedTabIndex = selectedGroup.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            VoterGroup.values().forEach { group ->
                Tab(
                    selected = selectedGroup == group,
                    onClick = {
                        selectedGroup = group
                        selectedStageKey = group.stages.first().first
                    },
                    text = {
                        Text(
                            text = group.label,
                            fontWeight = if (selectedGroup == group) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // 2. Sub-Stage Chips Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedGroup.stages.forEach { (key, label) ->
                val isSelected = selectedStageKey == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStageKey = key },
                    label = { Text(label, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // 3. Search and Status Filter Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari NIK / Nama Pemilih...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; loadVoters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = { loadVoters() }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Cari")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("SEMUA", "AKTIF", "TMS").forEach { st ->
                    val isSel = selectedStatus == st
                    AssistChip(
                        onClick = { selectedStatus = st },
                        label = { Text(st, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            labelColor = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = " Jiwa",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

        // 4. Content Area
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
                        Text("Memuat data pemilih...", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
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
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "Terjadi kesalahan", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadVoters() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                voterList.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada data pemilih pada filter ini", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(voterList, key = { it.id }) { voter ->
                            VoterCard(
                                voter = voter,
                                onClick = {
                                    selectedVoterDetail = voter
                                    onVoterClick(voter)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Detail Pemilih
    selectedVoterDetail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { selectedVoterDetail = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.nama,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    BadgeTahap(tahap = detail.tahap)
                }

                Spacer(modifier = Modifier.height(12.dp))

                DetailItemRow(label = "NIK (Masked)", value = maskNik(detail.nik))
                DetailItemRow(label = "Jenis Kelamin", value = if (detail.jenisKelamin == "L") "Laki-laki" else "Perempuan")
                DetailItemRow(label = "Usia", value = " Tahun")
                DetailItemRow(label = "Wilayah", value = "RT  / RW  (TPS )")
                DetailItemRow(label = "Alamat", value = detail.alamat ?: "-")
                DetailItemRow(label = "Status Data", value = detail.status)
                if (!detail.keterangan.isNullOrBlank()) {
                    DetailItemRow(label = "Keterangan", value = detail.keterangan)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { selectedVoterDetail = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun VoterCard(
    voter: VoterItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (voter.status == "AKTIF") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voter.jenisKelamin == "L") Icons.Default.Person else Icons.Default.Face,
                    contentDescription = null,
                    tint = if (voter.status == "AKTIF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voter.nama,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NIK: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RT /RW  â€¢ TPS ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                BadgeTahap(tahap = voter.tahap)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = voter.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (voter.status == "AKTIF") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun BadgeTahap(tahap: String) {
    val (bgColor, textColor) = when (tahap.uppercase()) {
        "DPT" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "DPSHP_AKHIR", "DPSHP" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "DPS_TAMBAHAN" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "DPS" -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        "BAHAN_COKLIT" -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        else -> Color(0xFFECEFF1) to Color(0xFF37474F)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = tahap.replace("_", " "),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun maskNik(nik: String): String {
    if (nik.length < 8) return nik
    return nik.take(4) + "********" + nik.takeLast(4)
}