package id.p2kd.kalisalak.coklit.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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

    val startDestination = if (sessionManager.getToken() != null) "home" else "login"

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
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                sessionManager = sessionManager,
                offlineQueue = offlineQueue,
                onNavigateToScan = { navController.navigate("scan") },
                onNavigateToTasks = { navController.navigate("tasks") },
                onNavigateToSync = { navController.navigate("sync") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("tasks") {
            TaskListScreen(
                onBack = { navController.popBackStack() },
                onSelectRumah = { rumahId -> navController.navigate("kk_list/" + rumahId) },
                onNavigateToScan = { navController.navigate("scan") }
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
                onSuccess = { isOffline ->
                    currentVerifikasiList = emptyList()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("sync") {
            SyncHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
