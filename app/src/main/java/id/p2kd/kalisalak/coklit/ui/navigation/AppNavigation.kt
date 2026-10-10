package id.p2kd.kalisalak.coklit.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.MemberVerificationPayload
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.screens.*
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { EncryptedSessionManager(context) }
    val offlineQueue = remember { OfflineQueueManager(context) }
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    var isBackgroundLocked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Background-to-Foreground Auto-Lock: Wajib Masukkan PIN / Sidik Jari / Password saat aplikasi kembali dari latar belakang
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Ketika aplikasi diminimize ke latar belakang
                if (sessionManager.isLoggedIn()) {
                    isBackgroundLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldLock = sessionManager.isAutoLockTriggered()
    val startDestination = if (sessionManager.getToken() != null && !shouldLock) "main" else "login"

    // In-memory state for current visit verification list
    var currentVerifikasiList by remember { mutableStateOf<List<MemberVerificationPayload>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                onSelectHouse = { rumahId -> navController.navigate("kk_list/" + rumahId) },
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
                onNotificationClick = { _ ->
                    navController.popBackStack()
                }
            )
        }

        composable("scan") {
            var isCheckingQr by remember { mutableStateOf(false) }
            var invalidQrDialog by remember { mutableStateOf<String?>(null) }

            if (invalidQrDialog != null) {
                AlertDialog(
                    onDismissRequest = { invalidQrDialog = null },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Amber400) },
                    title = { Text("QR Code Tidak Terdaftar", fontWeight = FontWeight.Bold, color = White) },
                    text = { Text(invalidQrDialog!!, color = Slate300, fontSize = 13.sp) },
                    confirmButton = {
                        Button(
                            onClick = { invalidQrDialog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                        ) {
                            Text("Pindai Ulang", color = White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            invalidQrDialog = null
                            navController.navigate("tasks")
                        }) {
                            Text("Input Manual", color = Slate400)
                        }
                    },
                    containerColor = Navy900
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CameraScanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onManualInputClick = { navController.navigate("tasks") },
                    onQrScanned = { rawToken ->
                        coroutineScope.launch {
                            isCheckingQr = true
                            try {
                                val token = rawToken.trim()
                                val res = ApiClient.api.lookupQr(token)
                                if (res.isSuccessful && res.body()?.valid == true) {
                                    val lookup = res.body()!!
                                    if (lookup.rumah != null) {
                                        navController.navigate("kk_list/" + lookup.rumah.id) {
                                            popUpTo("scan") { inclusive = true }
                                        }
                                    } else {
                                        val qrId = lookup.qr?.id ?: ""
                                        val qrToken = lookup.qr?.qrToken ?: token
                                        navController.navigate("house_form/" + qrId + "/" + qrToken) {
                                            popUpTo("scan") { inclusive = true }
                                        }
                                    }
                                } else {
                                    val errorMsg = res.body()?.message ?: "QR Code '$token' tidak terdaftar dalam database resmi P2KD Kalisalak. Pastikan menggunakan stiker resmi panitia."
                                    invalidQrDialog = errorMsg
                                }
                            } catch (e: Exception) {
                                invalidQrDialog = "Koneksi terganggu saat memverifikasi stiker: " + (e.localizedMessage ?: "Silakan periksa jaringan internet Anda.")
                            } finally {
                                isCheckingQr = false
                            }
                        }
                    }
                )

                if (isCheckingQr) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Slate950.copy(alpha = 0.75f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Blue400)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Memverifikasi Stiker QR Resmi...", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
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

    // OVERLAY KUNCI LATAR BELAKANG: Wajib PIN / Sidik Jari / Password saat kembali dari background
    if (isBackgroundLocked && sessionManager.isLoggedIn()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            LoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = {
                    isBackgroundLocked = false
                }
            )
        }
    }
    }
}

/**
 * Modern 5-Position Bottom Navigation Scaffold
 * 1. Beranda
 * 2. Data Pemilih
 * 3. Kamera (Floating Center Action Button)
 * 4. Rumah (Rumah Tercatat)
 * 5. Akun (Profil & Pengaturan)
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
    onSelectHouse: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Beranda, 1: Data Pemilih, 3: Rumah, 4: Akun
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Back Button Handler: Cegah langsung keluar 1 kali, konfirmasi dulu!
    BackHandler(enabled = true) {
        if (selectedTab != 0) {
            selectedTab = 0 // Kembali ke tab Beranda dulu
        } else {
            showExitConfirmDialog = true // Minta konfirmasi sebelum keluar aplikasi
        }
    }

    // Dialog Konfirmasi Keluar Aplikasi Resmi
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Rose500,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Konfirmasi Keluar Aplikasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menutup aplikasi PETUGAS P2KD? Sesi Anda akan tetap aman dan terkunci.",
                    color = Color(0xFF475569),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        (context as? android.app.Activity)?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Keluar Aplikasi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Navy800,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.5f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_p2kd),
                                contentDescription = "Logo Resmi P2KD",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PETUGAS P2KD",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Blue600.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "v" + sessionManager.getAppVersion(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blue300,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Pilkades 2026 • Petugas Coklit"
                                    1 -> "Data Pemilih Warga (DPT)"
                                    3 -> "Rumah Tercatat Lapangan"
                                    else -> "Profil & Pengaturan Akun"
                                },
                                fontSize = 11.sp,
                                color = Blue300
                            )
                        }
                    }
                },
                actions = {
                    // Icon Lonceng Notifikasi Interaktif Premium dengan Efek Ripple dan Red Badge Dot
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (unreadNotificationCount > 0) Rose500 else Color.White.copy(alpha = 0.35f)
                        ),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToNotifications() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = Rose600,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unreadNotificationCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Pusat Notifikasi dan Pembaruan",
                                    tint = if (unreadNotificationCount > 0) Amber400 else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
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
                    containerColor = Slate950,
                    tonalElevation = 8.dp
                ) {
                    // Posisi 1: Beranda
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue400,
                            selectedTextColor = Blue400,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = Navy800
                        )
                    )

                    // Posisi 2: Data Pemilih
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.People, contentDescription = "Data Pemilih") },
                        label = { Text("Pemilih", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue400,
                            selectedTextColor = Blue400,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = Navy800
                        )
                    )

                    // Posisi 3: Placeholder tengah untuk FAB Kamera
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToScan,
                        icon = { Spacer(modifier = Modifier.size(24.dp)) },
                        label = { Text("Kamera", fontSize = 11.sp, color = Color.Transparent) },
                        enabled = true
                    )

                    // Posisi 4: Rumah Tercatat
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.HomeWork, contentDescription = "Rumah") },
                        label = { Text("Rumah", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue400,
                            selectedTextColor = Blue400,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = Navy800
                        )
                    )

                    // Posisi 5: Akun
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Akun") },
                        label = { Text("Akun", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue400,
                            selectedTextColor = Blue400,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = Navy800
                        )
                    )
                }

                // Posisi 3: Tombol Kamera Mengambang di Tengah (Floating Action Button)
                FloatingActionButton(
                    onClick = onNavigateToScan,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .size(58.dp)
                        .shadow(10.dp, CircleShape),
                    shape = CircleShape,
                    containerColor = Blue600,
                    contentColor = White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pindai Kamera",
                        modifier = Modifier.size(28.dp),
                        tint = White
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
                3 -> RegisteredHousesScreen(
                    sessionManager = sessionManager,
                    onNavigateToScan = onNavigateToScan,
                    onSelectHouse = onSelectHouse
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
