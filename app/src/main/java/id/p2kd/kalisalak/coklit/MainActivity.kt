package id.p2kd.kalisalak.coklit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.data.update.AppUpdateManager
import id.p2kd.kalisalak.coklit.data.update.UpdateInfo
import id.p2kd.kalisalak.coklit.ui.navigation.AppNavigation
import id.p2kd.kalisalak.coklit.ui.screens.WelcomeScreen
import id.p2kd.kalisalak.coklit.ui.theme.*

class MainActivity : FragmentActivity() {

    private lateinit var sessionManager: EncryptedSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = EncryptedSessionManager(this)
        sessionManager.updateLastActiveTime()
        enableEdgeToEdge()

        setContent {
            P2kdTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showWelcomeScreen by remember { mutableStateOf(true) }

                    AnimatedVisibility(
                        visible = showWelcomeScreen,
                        exit = fadeOut()
                    ) {
                        WelcomeScreen(
                            onFinish = { showWelcomeScreen = false }
                        )
                    }

                    AnimatedVisibility(
                        visible = !showWelcomeScreen,
                        enter = fadeIn()
                    ) {
                        PermissionGuardWrapper {
                            AppUpdateGuardWrapper {
                                AppNavigation()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::sessionManager.isInitialized) {
            sessionManager.updateLastActiveTime()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::sessionManager.isInitialized) {
            sessionManager.updateLastActiveTime()
        }
    }
}

/**
 * In-App Auto-Update Guard:
 * Otomatis mendeteksi rilis versi baru APK, mengirim notifikasi sistem,
 * dan memunculkan dialog pembaruan dengan direct link unduh ke browser / download manager.
 */
@Composable
fun AppUpdateGuardWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDialogDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val info = AppUpdateManager.checkForUpdate(context)
                if (info != null && info.hasUpdate) {
                    updateInfo = info
                    // Otomatis dan realtime kirim notifikasi pada bilah notifikasi Android secara resmi
                    AppUpdateManager.postSystemNotification(context, info)
                }
            } catch (_: Exception) {
                // Abaikan kesalahan koneksi berkala
            }
            // Realtime polling setiap 3 menit saat aplikasi aktif di latar depan
            kotlinx.coroutines.delay(3 * 60 * 1000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Modal Dialog Pembaruan Aplikasi Otomatis
        if (updateInfo != null && !isDialogDismissed) {
            val info = updateInfo!!
            AlertDialog(
                onDismissRequest = {
                    if (!info.isMandatory) {
                        isDialogDismissed = true
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(26.dp),
                icon = {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Blue600)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Rilis Resmi v${info.newVersion}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pembaruan Lapangan P2KD Kalisalak",
                            fontSize = 12.sp,
                            color = Blue600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pembaruan versi ${info.newVersion} tersedia untuk menunjang kelancaran dan akurasi data.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )

                        if (info.releaseNotes.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Catatan Pembaruan:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    info.releaseNotes.forEach { note ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("•", color = Blue600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(note, color = Color(0xFF334155), fontSize = 12.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unduh & Perbarui APK Sekarang", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    if (!info.isMandatory) {
                        TextButton(
                            onClick = { isDialogDismissed = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nanti Saja", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Permission Guard Wrapper:
 * Memastikan izin Kamera, Notifikasi (Android 13+), dan Lokasi diaktifkan secara interaktif.
 */
@Composable
fun PermissionGuardWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        val list = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var isGranted by remember { mutableStateOf(hasPermissions()) }
    var showDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val cameraGranted = map[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            isGranted = true
            showDialog = false
        } else {
            isGranted = false
            showDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions()) {
            launcher.launch(requiredPermissions)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = !isGranted && showDialog,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Blue600,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Izin Perangkat Diperlukan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Untuk menggunakan fitur Pindai QR Stiker dan menerima pembaruan data pemilih, berikan izin Kamera dan Notifikasi.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Pengaturan HP", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
