package id.p2kd.kalisalak.coklit.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.MemberVerificationPayload
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { EncryptedSessionManager(context) }
    val offlineQueue = remember { OfflineQueueManager(context) }
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val startDestination = if (sessionManager.getToken() != null) "main" else "login"

    // In-memory state for current visit verification list
    var currentVerifikasiList by remember { mutableStateOf<List<MemberVerificationPayload>>(emptyList()) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainContainerScreen(
                sessionManager = sessionManager,
                offlineQueue = offlineQueue,
                onNavigateToScan = { navController.navigate("scan") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToSync = { navController.navigate("sync") },
                onNavigateToTasks = { navController.navigate("tasks") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("tasks") {
            TaskListScreen(
                onBack = { navController.popBackStack() },
                onSelectRumah = { rumahId -> navController.navigate("kk_list/" + rumahId) },
                onNavigateToScan = { navController.navigate("scan") }
            )
        }

        composable("notifications") {
            NotificationCenterScreen(
                onBack = { navController.popBackStack() },
                onNotificationClick = { notif ->
                    if (notif.deepLink == "activity/kunjungan" || notif.deepLink == "activity/aduan" || notif.deepLink == "activity/pengumuman") {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable("scan") {
            CameraScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onManualInputClick = { navController.navigate("tasks") },
                onQrScanned = { rawToken ->
                    coroutineScope.launch {
                        try {
                            val res = ApiClient.api.lookupQr(rawToken.trim())
                            if (res.isSuccessful && res.body()?.valid == true) {
                                val lookup = res.body()!!
                                if (lookup.rumah != null) {
                                    navController.navigate("kk_list/" + lookup.rumah.id) {
                                        popUpTo("scan") { inclusive = true }
                                    }
                                } else {
                                    val qrId = lookup.qr?.id ?: ""
                                    val token = lookup.qr?.qrToken ?: rawToken
                                    navController.navigate("house_form/" + qrId + "/" + token) {
                                        popUpTo("scan") { inclusive = true }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        composable(
            route = "house_form/{qrId}/{token}",
            arguments = listOf(
                navArgument("qrId") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val qrId = backStackEntry.arguments?.getString("qrId") ?: ""
            val token = backStackEntry.arguments?.getString("token") ?: ""

            HouseFormScreen(
                qrId = qrId,
                qrToken = token,
                onBack = { navController.popBackStack() },
                onSuccess = { createdRumah ->
                    navController.navigate("kk_list/" + createdRumah.id) {
                        popUpTo("house_form/{qrId}/{token}") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "kk_list/{rumahId}",
            arguments = listOf(
                navArgument("rumahId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rumahId = backStackEntry.arguments?.getString("rumahId") ?: ""
            KKListScreen(
                rumahId = rumahId,
                onBack = { navController.popBackStack() },
                onSelectKk = { kkId, noKk ->
                    navController.navigate("family_members/" + rumahId + "/" + kkId + "/" + noKk)
                },
                onProceedToVisitConfirmation = {
                    navController.navigate("visit_confirm/" + rumahId)
                }
            )
        }

        composable(
            route = "family_members/{rumahId}/{kkId}/{noKk}",
            arguments = listOf(
                navArgument("rumahId") { type = NavType.StringType },
                navArgument("kkId") { type = NavType.StringType },
                navArgument("noKk") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rumahId = backStackEntry.arguments?.getString("rumahId") ?: ""
            val kkId = backStackEntry.arguments?.getString("kkId") ?: ""
            val noKk = backStackEntry.arguments?.getString("noKk") ?: ""

            FamilyMemberListScreen(
                rumahId = rumahId,
                kkId = kkId,
                noKk = noKk,
                onBack = { navController.popBackStack() },
                onVerificationChanged = { updatedList ->
                    val existingMap = currentVerifikasiList.associateBy { it.pemilih_id }.toMutableMap()
                    updatedList.forEach { item -> existingMap[item.pemilih_id] = item }
                    currentVerifikasiList = existingMap.values.toList()
                }
            )
        }

        composable(
            route = "visit_confirm/{rumahId}",
            arguments = listOf(
                navArgument("rumahId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rumahId = backStackEntry.arguments?.getString("rumahId") ?: ""
            VisitConfirmationScreen(
                rumahId = rumahId,
                verifikasiList = currentVerifikasiList,
                onBack = { navController.popBackStack() },
                onSuccess = { _ ->
                    currentVerifikasiList = emptyList()
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("sync") {
            SyncHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Modern 5-Position Bottom Navigation Scaffold
 * 1. Beranda
 * 2. Data Pemilih
 * 3. Kamera (Floating Center Action Button)
 * 4. Aktivitas
 * 5. Lainnya
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    sessionManager: EncryptedSessionManager,
    offlineQueue: OfflineQueueManager,
    onNavigateToScan: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Beranda, 1: Data Pemilih, 3: Aktivitas, 4: Lainnya
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch unread notification count
    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.api.getNotifications()
            if (res.isSuccessful && res.body()?.success == true) {
                unreadNotificationCount = res.body()!!.unreadCount
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val (title, subtitle) = when (selectedTab) {
                            0 -> "P2KD Kalisalak" to "Coklit Pilkades 2026"
                            1 -> "Data Pemilih" to "7 Tahapan Administrasi"
                            3 -> "Pusat Aktivitas" to "Laporan, Kunjungan & Info"
                            else -> "Menu Lainnya" to "Profil & Pengaturan"
                        }
                        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    // Notification Center Bell Icon with Badge
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(text = if (unreadNotificationCount > 9) "9+" else "")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Pusat Notifikasi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Bottom Navigation Bar with 5 slots
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    // Posisi 1: Beranda
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda", fontSize = 11.sp) }
                    )

                    // Posisi 2: Data Pemilih
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.People, contentDescription = "Data Pemilih") },
                        label = { Text("Pemilih", fontSize = 11.sp) }
                    )

                    // Posisi 3: Placeholder tengah untuk FAB Kamera
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToScan,
                        icon = { Spacer(modifier = Modifier.size(24.dp)) },
                        label = { Text("Kamera", fontSize = 11.sp, color = Color.Transparent) },
                        enabled = true
                    )

                    // Posisi 4: Aktivitas
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.FactCheck, contentDescription = "Aktivitas") },
                        label = { Text("Aktivitas", fontSize = 11.sp) }
                    )

                    // Posisi 5: Lainnya
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Lainnya") },
                        label = { Text("Lainnya", fontSize = 11.sp) }
                    )
                }

                // Posisi 3: Tombol Kamera Mengambang di Tengah (Floating Action Button)
                FloatingActionButton(
                    onClick = onNavigateToScan,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .size(58.dp)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pindai Kamera",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    sessionManager = sessionManager,
                    offlineQueue = offlineQueue,
                    onNavigateToScan = onNavigateToScan,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToSync = onNavigateToSync,
                    onNavigateToProfile = { selectedTab = 4 }
                )
                1 -> VoterDataScreen()
                3 -> ActivityHubScreen(
                    onNavigateToTasks = onNavigateToTasks
                )
                4 -> MoreScreen(
                    sessionManager = sessionManager,
                    offlineQueue = offlineQueue,
                    onNavigateToSync = onNavigateToSync,
                    onLogout = onLogout
                )
            }
        }
    }
}