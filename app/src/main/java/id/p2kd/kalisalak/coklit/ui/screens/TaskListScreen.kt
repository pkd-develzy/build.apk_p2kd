package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.RumahItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onBack: () -> Unit,
    onSelectRumah: (rumahId: String) -> Unit,
    onNavigateToScan: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var taskList by remember { mutableStateOf<List<RumahItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadTasks() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val res = ApiClient.api.getTasks()
                if (res.isSuccessful && res.body()?.success == true) {
                    taskList = res.body()?.rumahList ?: emptyList()
                } else {
                    errorMessage = res.body()?.message ?: "Gagal memuat daftar tugas"
                }
            } catch (e: Exception) {
                errorMessage = "Gagal memuat: "
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTasks()
    }

    val filteredList = remember(taskList, searchQuery) {
        if (searchQuery.isBlank()) taskList
        else {
            taskList.filter {
                it.alamat.contains(searchQuery, ignoreCase = true) ||
                it.rt.contains(searchQuery) ||
                it.rw.contains(searchQuery) ||
                it.qrToken.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Tugas Rumah Coklit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { loadTasks() }) {
                        Text("Coba Lagi")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari alamat, RT, token QR...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total Rumah Terdata: ",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Belum ada rumah yang terdaftar.", color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onNavigateToScan) {
                                    Text("Pindai QR Rumah Baru")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredList) { house ->
                                val statusBg = when (house.statusPendataan) {
                                    "COMPLETED" -> Color(0xFFE8F5E9)
                                    "VISITED" -> Color(0xFFE3F2FD)
                                    "FOLLOW_UP_REQUIRED" -> Color(0xFFFFEBEE)
                                    else -> Color(0xFFFFF3E0)
                                }
                                val statusText = when (house.statusPendataan) {
                                    "COMPLETED" -> Color(0xFF2E7D32)
                                    "VISITED" -> Color(0xFF1565C0)
                                    "FOLLOW_UP_REQUIRED" -> Color(0xFFC62828)
                                    else -> Color(0xFFE65100)
                                }

                                Card(
                                    onClick = { onSelectRumah(house.id) },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = house.alamat.ifEmpty { "Tanpa Alamat Fisik" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "RT  / RW  • TPS: ",
                                                fontSize = 13.sp,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = "Token QR: ",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Surface(
                                            color = statusBg,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = house.statusPendataan,
                                                color = statusText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
