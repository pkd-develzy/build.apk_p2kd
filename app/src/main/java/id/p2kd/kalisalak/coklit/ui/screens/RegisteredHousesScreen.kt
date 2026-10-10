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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.RumahItem
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RegisteredHousesScreen(
    sessionManager: EncryptedSessionManager,
    onNavigateToScan: () -> Unit,
    onSelectHouse: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile = sessionManager.getUserProfile()

    var houses by remember { mutableStateOf<List<RumahItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRt by remember { mutableStateOf("SEMUA") }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadHouses() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = ApiClient.api.getTasks(userProfile?.assignedRw, userProfile?.assignedTps)
                if (res.isSuccessful && res.body()?.success == true) {
                    houses = res.body()!!.rumahList
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat daftar rumah tercatat."
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Kesalahan koneksi jaringan."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHouses()
    }

    // Extract unique RTs
    val rtList = remember(houses) {
        val rts = houses.mapNotNull { it.rt.takeIf { rt -> rt.isNotBlank() } }.distinct().sorted()
        listOf("SEMUA") + rts
    }

    val filteredHouses = remember(houses, selectedRt, searchQuery) {
        houses.filter { house ->
            val matchRt = selectedRt == "SEMUA" || house.rt.replace("\\D".toRegex(), "").padStart(2, '0') == selectedRt.padStart(2, '0')
            val matchSearch = searchQuery.isBlank() ||
                    house.alamat.contains(searchQuery, ignoreCase = true) ||
                    house.qrToken.contains(searchQuery, ignoreCase = true) ||
                    (house.nomorRumah ?: "").contains(searchQuery, ignoreCase = true)
            matchRt && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari alamat, nomor rumah, atau token QR...", fontSize = 13.sp, color = Slate400) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Blue400) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Slate400)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Navy900,
                unfocusedContainerColor = Navy900,
                focusedBorderColor = Blue500,
                unfocusedBorderColor = Slate800,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // RT Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(rtList) { rt ->
                val isSelected = selectedRt == rt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedRt = rt },
                    label = {
                        Text(
                            text = if (rt == "SEMUA") "Semua RT (" + houses.size + ")" else "RT " + rt,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Status header & reload button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tercatat: " + filteredHouses.size + " Rumah",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate300
            )
            IconButton(
                onClick = { loadHouses() },
                modifier = Modifier.size(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Blue400, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Blue400, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // House List / Empty State
        if (filteredHouses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        color = Navy900,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue800.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Blue400, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (houses.isEmpty()) "Belum Ada Rumah Tercatat" else "Tidak Ada Rumah Ditemukan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (houses.isEmpty())
                            "Data rumah akan terisi otomatis saat Anda menempel stiker QR di rumah warga dan memindainya."
                        else "Coba ubah kata kunci pencarian atau pilih RT lainnya.",
                        fontSize = 12.sp,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (houses.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToScan,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pindai QR Rumah Pertama", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredHouses, key = { it.id }) { house ->
                    HouseCardItem(house = house, onClick = { onSelectHouse(house.id) })
                }
            }
        }
    }
}

@Composable
fun HouseCardItem(
    house: RumahItem,
    onClick: () -> Unit
) {
    Surface(
        color = Navy900,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: QR Token & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Blue950,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue800.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Blue400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (house.qrToken.isNotBlank()) house.qrToken else "STIKER QR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue300
                        )
                    }
                }

                Surface(
                    color = if (house.statusPendataan == "SELESAI") Emerald900.copy(alpha = 0.3f) else Amber900.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (house.statusPendataan == "SELESAI") Emerald500.copy(alpha = 0.5f) else Amber500.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (house.statusPendataan == "SELESAI") "TERVERIFIKASI" else "TERDAFTAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (house.statusPendataan == "SELESAI") Emerald400 else Amber400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alamat & RT/RW
            Text(
                text = if (house.alamat.isNotBlank()) house.alamat else "Rumah Warga",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Blue400, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RT " + house.rt.padStart(2, '0') + " / RW " + house.rw.padStart(2, '0') + " • Desa Kalisalak",
                    fontSize = 12.sp,
                    color = Slate300
                )
            }

            if (!house.keteranganLokasi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ket: " + house.keteranganLokasi,
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sentuh untuk lihat Kartu Keluarga & Jiwa",
                    fontSize = 11.sp,
                    color = Slate400
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Blue400)
            }
        }
    }
}
