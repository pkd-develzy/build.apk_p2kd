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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val totalTerverifikasi = remember(houses) {
        houses.count { it.statusPendataan == "SELESAI" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPearl)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // 1. KOTAK PENCARIAN (EXECUTIVE WHITE)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari alamat, nomor rumah, atau kode C6...", fontSize = 13.sp, color = TextSubtle) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalSapphireBright, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TextSubtle, modifier = Modifier.size(18.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = RoyalSapphireBright,
                unfocusedBorderColor = CardBorderSubtle,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. TIGA KARTU RINGKASAN METRIK LAPANGAN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Rumah
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalSapphireBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Rumah Terdata", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${houses.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RoyalSapphireDark)
                }
            }

            // Card 2: Terverifikasi
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Terverifikasi", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$totalTerverifikasi", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EmeraldVibrant)
                }
            }

            // Card 3: Wilayah RW
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Wilayah Tugas", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("RW ${userProfile?.assignedRw ?: '-'}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. RT FILTER CHIPS
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
                        selectedContainerColor = RoyalSapphireBright,
                        selectedLabelColor = White,
                        containerColor = PureWhite,
                        labelColor = TextMuted
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = RoyalSapphireBright,
                        borderColor = CardBorderSubtle
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. HEADER STATUS & RELOAD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Menampilkan " + filteredHouses.size + " Rumah Terdata",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            IconButton(
                onClick = { loadHouses() },
                modifier = Modifier.size(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RoyalSapphireBright, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = RoyalSapphireBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. HOUSE LIST / EMPTY STATE
        if (filteredHouses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSubtle),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            color = RoyalSapphireSoft,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = RoyalSapphireBright, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (houses.isEmpty()) "Belum Ada Rumah Tercatat" else "Tidak Ada Rumah Ditemukan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (houses.isEmpty())
                                "Data rumah akan otomatis terdaftar saat Anda menempel stiker C6 dan memindainya dengan kamera."
                            else "Coba ubah kata kunci pencarian atau pilih RT lainnya.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        if (houses.isEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onNavigateToScan,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalSapphireBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pindai QR Rumah Pertama", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalSapphireBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: QR Token & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RoyalSapphireSoft,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalSapphireBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = RoyalSapphireBright, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (house.qrToken.isNotBlank()) "STIKER: #" + house.qrToken.takeLast(8).uppercase() else "STIKER QR C6",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalSapphireDark
                        )
                    }
                }

                val isSelesai = house.statusPendataan == "SELESAI"
                Surface(
                    color = if (isSelesai) EmeraldSoft else AmberSoft,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelesai) EmeraldBorder else AmberBorder
                    )
                ) {
                    Text(
                        text = if (isSelesai) "TERVERIFIKASI" else "TERDAFTAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelesai) EmeraldDark else AmberAccent,
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
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalSapphireBright, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RT " + house.rt.padStart(2, '0') + " / RW " + house.rw.padStart(2, '0') + " • Desa Kalisalak",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            if (!house.keteranganLokasi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Ket: " + house.keteranganLokasi,
                    fontSize = 11.sp,
                    color = TextSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RoyalSapphireSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalSapphireBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buka Daftar Kartu Keluarga (KK)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalSapphireDark
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = RoyalSapphireBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
