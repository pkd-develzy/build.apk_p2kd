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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
                        AppNavigation()
                    }
                }
            }
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
                                // Coba minta lagi
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
                            // Buka pengaturan aplikasi sistem jika pengguna memilih 'Jangan tanya lagi'
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
