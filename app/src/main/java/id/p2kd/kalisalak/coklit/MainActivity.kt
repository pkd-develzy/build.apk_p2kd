package id.p2kd.kalisalak.coklit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import id.p2kd.kalisalak.coklit.data.update.AppUpdateManager
import id.p2kd.kalisalak.coklit.data.update.UpdateInfo
import id.p2kd.kalisalak.coklit.ui.navigation.AppNavigation
import id.p2kd.kalisalak.coklit.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            P2kdTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
                containerColor = Navy900,
                shape = RoundedCornerShape(26.dp),
                icon = {
                    Surface(
                        color = Blue600.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Pembaruan",
                                tint = Blue400,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Pembaruan Tersedia!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Amber500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Versi Terbaru: v${info.newVersion}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Versi terbaru PETUGAS P2KD siap diunduh. Harap perbarui aplikasi Anda untuk kelancaran tugas Coklit di lapangan:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300
                        )

                        if (info.releaseNotes.isNotEmpty()) {
                            Surface(
                                color = Navy800,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Catatan Pembaruan:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Blue400,
                                        fontWeight = FontWeight.Bold
                                    )
                                    info.releaseNotes.forEach { note ->
                                        Text(
                                            text = "• $note",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate200,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(downloadIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unduh Pembaruan Sekarang", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    if (!info.isMandatory) {
                        TextButton(
                            onClick = { isDialogDismissed = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nanti Saja", color = Slate400, fontSize = 13.sp)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Professional Permission Guard:
 * Langsung meminta izin Kamera, Lokasi, dan Notifikasi saat aplikasi pertama kali dibuka.
 */
@Composable
fun PermissionGuardWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    fun checkAllGranted(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    var allGranted by remember { mutableStateOf(checkAllGranted()) }
    var hasAttemptedRequest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAttemptedRequest = true
        allGranted = checkAllGranted()
    }

    // Otomatis meminta semua izin runtime langsung saat aplikasi dijalankan
    LaunchedEffect(Unit) {
        if (!allGranted) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Pop-up dialog penjelasan profesional jika izin ditolak / belum lengkap
        AnimatedVisibility(
            visible = !allGranted && hasAttemptedRequest,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            AlertDialog(
                onDismissRequest = { /* Modal wajib konfirmasi untuk kelancaran tugas lapangan */ },
                containerColor = Navy900,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Surface(
                        color = Amber500.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Izin Perangkat Diperlukan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Aplikasi PETUGAS P2KD memerlukan 3 izin utama agar seluruh modul operasional lapangan berfungsi normal:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300
                        )

                        PermissionRequirementRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Kamera Belakang (CameraX)",
                            description = "Pindai stiker QR 1 Rumah & dokumentasi fisik di lapangan."
                        )

                        PermissionRequirementRow(
                            icon = Icons.Default.LocationOn,
                            title = "Lokasi Presisi (GPS)",
                            description = "Penandaan titik koordinat geografis rumah pemilih secara akurat."
                        )

                        PermissionRequirementRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifikasi Real-time",
                            description = "Pemberitahuan instruksi panitia & pembaruan tahapan Pilkades."
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val stillMissing = requiredPermissions.any {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (stillMissing) {
                                permissionLauncher.launch(requiredPermissions.toTypedArray())
                            } else {
                                allGranted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Berikan Semua Izin", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Buka Pengaturan HP", color = Slate400, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionRequirementRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy800, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = Blue600.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Blue400, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = White, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Slate400, fontSize = 11.sp)
        }
    }
}
