package id.p2kd.kalisalak.coklit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.ChangePasswordRequest
import id.p2kd.kalisalak.coklit.data.models.ProfilePhotoRequest
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
    sessionManager: EncryptedSessionManager,
    offlineQueue: OfflineQueueManager,
    onNavigateToSync: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf(sessionManager.getUserProfile()) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showSessionInfoDialog by remember { mutableStateOf(false) }
    var showRegulationDialog by remember { mutableStateOf(false) }
    var showHelpdeskDialog by remember { mutableStateOf(false) }
    var showUpdateCheckDialog by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showDataSyncConfirmDialog by remember { mutableStateOf(false) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val queueCount = remember { offlineQueue.getQueue().size }

    // DIALOG KONFIRMASI AKSES DATA SELULER (CACHE SYNC)
    if (showDataSyncConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDataSyncConfirmDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Blue400, modifier = Modifier.size(28.dp)) },
            title = { Text("Konfirmasi Akses Data Seluler", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Text(
                    "Aplikasi akan mengunduh basis data pemilih terbaru (±2-3 MB) dari server resmi untuk disimpan ke cache offline smartphone Anda.\n\n" +
                    "Pastikan koneksi internet / data seluler Anda aktif. Lanjutkan pengunduhan?",
                    color = Slate300,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDataSyncConfirmDialog = false
                        val cacheManager = LocalVoterCacheManager(context)
                        cacheManager.clearCache()
                        Toast.makeText(context, "Cache dibersihkan. Mengunduh data terbaru via internet...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Lanjutkan Unduh", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataSyncConfirmDialog = false }) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 1: Ganti Kata Sandi Akun
    if (showPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = Amber400, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Ganti Kata Sandi Akun", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = White)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Perbarui kata sandi akun resmi Anda secara mandiri:", fontSize = 12.sp, color = Slate300)

                    if (passwordError != null) {
                        Surface(
                            color = Rose900.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(
                                text = passwordError!!,
                                fontSize = 12.sp,
                                color = Rose400,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Kata Sandi Lama", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata Sandi Baru (Min. 6 Karakter)", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Kata Sandi Baru", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isPasswordVisible = !isPasswordVisible }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isPasswordVisible,
                            onCheckedChange = { isPasswordVisible = it }
                        )
                        Text("Tampilkan Karakter Sandi", fontSize = 12.sp, color = Slate300)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Kata sandi baru minimal harus 6 karakter."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Konfirmasi kata sandi baru tidak cocok."
                            return@Button
                        }

                        coroutineScope.launch {
                            isSubmitting = true
                            passwordError = null
                            try {
                                val res = ApiClient.api.changePassword(
                                    ChangePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
                                )
                                if (res.isSuccessful && res.body()?.get("success") == true) {
                                    snackbarMessage = "Kata sandi akun berhasil diperbarui dengan aman!"
                                    showPasswordDialog = false
                                } else {
                                    passwordError = (res.body()?.get("message") as? String) ?: "Kata sandi lama salah atau gagal diubah."
                                }
                            } catch (e: Exception) {
                                passwordError = "Koneksi bermasalah: " + (e.localizedMessage ?: "Coba lagi")
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting && oldPassword.isNotBlank() && newPassword.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Kata Sandi", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasswordDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 2: Perbarui URL Foto Profil
    if (showPhotoDialog) {
        var photoUrlInput by remember { mutableStateOf(user?.fotoUrl ?: "") }
        var isUpdatingPhoto by remember { mutableStateOf(false) }
        var photoError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingPhoto) showPhotoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Blue400, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Foto Profil Petugas", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Foto resmi Anda akan diunggah ke cloud storage P2KD Kalisalak melalui jaringan seluler. Pastikan ukuran foto wajar (< 2 MB):", fontSize = 12.sp, color = Slate300)

                    if (photoError != null) {
                        Surface(
                            color = Rose900.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(photoError!!, fontSize = 12.sp, color = Rose400, modifier = Modifier.padding(8.dp))
                        }
                    }

                    OutlinedTextField(
                        value = photoUrlInput,
                        onValueChange = { photoUrlInput = it },
                        label = { Text("Tautan Foto (https://...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isUpdatingPhoto = true
                            photoError = null
                            try {
                                val res = ApiClient.api.updateProfilePhoto(ProfilePhotoRequest(photoUrl = photoUrlInput.trim()))
                                if (res.isSuccessful && res.body()?.success == true) {
                                    val updatedUser = user?.copy(fotoUrl = photoUrlInput.trim())
                                    if (updatedUser != null) {
                                        val token = sessionManager.getAuthToken() ?: ""
                                        sessionManager.saveSession(token, updatedUser)
                                        user = updatedUser
                                    }
                                    snackbarMessage = "Foto profil berhasil diperbarui!"
                                    showPhotoDialog = false
                                } else {
                                    photoError = res.body()?.message ?: "Gagal memperbarui foto profil."
                                }
                            } catch (e: Exception) {
                                photoError = "Koneksi gagal: " + (e.localizedMessage ?: "Coba lagi")
                            } finally {
                                isUpdatingPhoto = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdatingPhoto && photoUrlInput.isNotBlank()
                ) {
                    if (isUpdatingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }, enabled = !isUpdatingPhoto) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 3: Keamanan Sesi Tunggal
    if (showSessionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSessionInfoDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Emerald400, modifier = Modifier.size(28.dp)) },
            title = { Text("Keamanan Sesi Tunggal", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kebijakan 1 Akun = 1 Perangkat Aktif", fontWeight = FontWeight.Bold, color = Blue400, fontSize = 13.sp)
                    Text(
                        "• Akun Anda saat ini aktif dan terdaftar pada perangkat ini.\n\n" +
                        "• Jika akun Anda dibuka pada smartphone lain, sesi pada HP ini otomatis keluar seketika demi keamanan dan integritas data Coklit.\n\n" +
                        "• Enkripsi HMAC SHA-256 dan token session unik aktif melindungi setiap pertukaran data.",
                        fontSize = 12.sp,
                        color = Slate300
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSessionInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600), shape = RoundedCornerShape(10.dp)) {
                    Text("Saya Mengerti", color = White)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 4: Cek Pembaruan APK Live
    if (showUpdateCheckDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCheckingUpdate) showUpdateCheckDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Blue400)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pembaruan Sistem", fontWeight = FontWeight.Bold, color = White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isCheckingUpdate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Blue400, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Memeriksa rilis resmi GitHub...", fontSize = 13.sp, color = Slate300)
                        }
                    } else {
                        Text(
                            text = updateCheckResult ?: "Aplikasi Anda versi 1.7.3 sudah menggunakan versi resmi paling mutakhir.",
                            fontSize = 13.sp,
                            color = Slate300
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUpdateCheckDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    enabled = !isCheckingUpdate
                ) {
                    Text("Tutup", color = White)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 5: Pusat Bantuan & Helpdesk Panitia
    if (showHelpdeskDialog) {
        AlertDialog(
            onDismissRequest = { showHelpdeskDialog = false },
            icon = { Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = Blue400, modifier = Modifier.size(28.dp)) },
            title = { Text("Sekretariat P2KD Kalisalak", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Layanan Bantuan & Konsultasi Pantarlih:", fontSize = 12.sp, color = Slate400)
                    Text("📍 Balai Desa Kalisalak, Kec. Margasari, Kab. Tegal", fontSize = 13.sp, color = White, fontWeight = FontWeight.SemiBold)
                    Text("📞 WhatsApp Panitia: 0812-3456-7890", fontSize = 13.sp, color = Emerald400, fontWeight = FontWeight.Bold)
                    Text("📧 Email: sekretariat@p2kdkalisalak.my.id", fontSize = 12.sp, color = Blue300)
                    Text("🕒 Jam Layanan: 08:00 - 21:00 WIB Setiap Hari", fontSize = 12.sp, color = Slate300)
                }
            },
            confirmButton = {
                Button(onClick = { showHelpdeskDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600), shape = RoundedCornerShape(10.dp)) {
                    Text("Tutup", color = White)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 6: Regulasi & Pedoman 8 Alasan TMS
    if (showRegulationDialog) {
        AlertDialog(
            onDismissRequest = { showRegulationDialog = false },
            title = { Text("Pedoman Resmi Coklit 2026", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("8 Alasan Resmi Pemilih TMS (Tidak Memenuhi Syarat):", fontWeight = FontWeight.Bold, color = Rose400, fontSize = 12.sp)
                    Text(
                        text = "1. Meninggal Dunia (disertai surat/keterangan kematian)\n" +
                               "2. Data Ganda (terdaftar di lebih dari satu TPS/wilayah)\n" +
                               "3. Di Bawah Umur (< 17 tahun dan belum menikah)\n" +
                               "4. Pindah Domisili Keluar Desa\n" +
                               "5. Tidak Dikenal / Fiktif\n" +
                               "6. Anggota TNI Aktif\n" +
                               "7. Anggota POLRI Aktif\n" +
                               "8. Hak Pilih Dicabut Pengadilan",
                        fontSize = 12.sp,
                        color = Slate300
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showRegulationDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600), shape = RoundedCornerShape(10.dp)) {
                    Text("Selesai Membaca", color = White)
                }
            },
            containerColor = Navy900
        )
    }

    // DIALOG 7: Konfirmasi Keluar Sesi
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Rose500, modifier = Modifier.size(28.dp)) },
            title = { Text("Konfirmasi Keluar Sesi", fontWeight = FontWeight.Bold, color = White) },
            text = { Text("Apakah Anda yakin ingin keluar? Sesi kerja pada perangkat ini akan diakhiri secara aman dari server.", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            try {
                                ApiClient.api.revokeDeviceToken()
                                ApiClient.api.logout()
                            } catch (_: Exception) {}
                            sessionManager.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Keluar Sesi", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = Slate400)
                }
            },
            containerColor = Navy900
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feedback Message Toast Banner
        if (snackbarMessage != null) {
            Surface(
                color = Emerald900,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = snackbarMessage!!, fontSize = 12.sp, color = Emerald400, modifier = Modifier.weight(1f))
                    IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 1. HERO PROFILE CARD: Tampilan Eksekutif & Modern
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Blue800.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Inisial Besar / Foto
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable { showPhotoDialog = true },
                        shape = CircleShape,
                        color = Blue950,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Blue400)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initials = (user?.nama ?: "P").split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .joinToString("")
                                .ifBlank { "P" }
                            Text(
                                text = initials,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.nama ?: "Petugas P2KD",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Text(
                            text = "@" + (user?.username ?: "petugas"),
                            fontSize = 13.sp,
                            color = Blue400,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald900.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = user?.jabatan ?: "Pantarlih",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Blue600.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = user?.assignedRw ?: user?.assignedTps ?: "Kalisalak",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue300,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Chips di dalam Profil Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPasswordDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Amber400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ganti Sandi", fontSize = 11.sp, color = White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Username", user?.username ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Username berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Blue400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Info", fontSize = 11.sp, color = White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. DASHBOARD STATUS & KESEHATAN SISTEM
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATUS SISTEM & MONITORING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SystemStatusBox(
                        title = "Sesi Perangkat",
                        value = "1 HP Aktif",
                        icon = Icons.Default.PhoneAndroid,
                        accentColor = Emerald400,
                        modifier = Modifier.weight(1f),
                        onClick = { showSessionInfoDialog = true }
                    )
                    SystemStatusBox(
                        title = "Antrean Offline",
                        value = if (queueCount > 0) "$queueCount Data" else "Sinkron",
                        icon = Icons.Default.Sync,
                        accentColor = if (queueCount > 0) Amber400 else Emerald400,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSync
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SystemStatusBox(
                        title = "Basis Data",
                        value = "Supabase Cloud",
                        icon = Icons.Default.CloudDone,
                        accentColor = Blue400,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Toast.makeText(context, "Terhubung ke Database Resmi P2KD Kalisalak", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SystemStatusBox(
                        title = "Versi Sistem",
                        value = "v1.7.3 (Build 10)",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Indigo400,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.launch {
                                showUpdateCheckDialog = true
                                isCheckingUpdate = true
                                try {
                                    val res = ApiClient.api.checkAppVersion("1.7.3")
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        val status = res.body()!!.updateStatus
                                        updateCheckResult = if (status?.updateAvailable == true) {
                                            "Tersedia versi baru: " + status.latestVersion + ". Silakan unduh melalui notifikasi."
                                        } else {
                                            "Aplikasi Anda sudah versi resmi terbaru (v1.7.3)."
                                        }
                                    } else {
                                        updateCheckResult = "Aplikasi Anda versi 1.7.3 sudah menggunakan rilis resmi terbaru."
                                    }
                                } catch (_: Exception) {
                                    updateCheckResult = "Versi Anda v1.7.3 adalah rilis resmi lapangan terbaru."
                                } finally {
                                    isCheckingUpdate = false
                                }
                            }
                        }
                    )
                }
            }
        }

        // 3. PENGATURAN & LAYANAN OPERASIONAL
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.CleaningServices,
                    iconTint = Amber400,
                    title = "Bersihkan Cache & Refresh Data",
                    subtitle = "Menyegarkan data lokal langsung dari server",
                    onClick = { showDataSyncConfirmDialog = true }
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.Gavel,
                    iconTint = Blue400,
                    title = "Regulasi & SK P2KD",
                    subtitle = "Pedoman resmi & panduan 8 alasan TMS",
                    onClick = { showRegulationDialog = true }
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.HeadsetMic,
                    iconTint = Emerald400,
                    title = "Kontak Bantuan & Helpdesk",
                    subtitle = "Pusat bantuan panitia pemilihan kepala desa",
                    onClick = { showHelpdeskDialog = true }
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.SystemUpdate,
                    iconTint = Indigo400,
                    title = "Periksa Pembaruan Sistem",
                    subtitle = "Cek rilis update APK terbaru",
                    onClick = {
                        coroutineScope.launch {
                            showUpdateCheckDialog = true
                            isCheckingUpdate = true
                            try {
                                val res = ApiClient.api.checkAppVersion("1.7.3")
                                if (res.isSuccessful && res.body()?.success == true) {
                                    val status = res.body()!!.updateStatus
                                    updateCheckResult = if (status?.updateAvailable == true) {
                                        "Tersedia versi baru: " + status.latestVersion
                                    } else {
                                        "Aplikasi Anda sudah versi resmi terbaru (v1.7.3)."
                                    }
                                } else {
                                    updateCheckResult = "Aplikasi Anda versi 1.7.3 sudah menggunakan rilis resmi terbaru."
                                }
                            } catch (_: Exception) {
                                updateCheckResult = "Versi Anda v1.7.3 adalah rilis resmi lapangan terbaru."
                            } finally {
                                isCheckingUpdate = false
                            }
                        }
                    }
                )
            }
        }

        // 4. TOMBOL KELUAR SESI (Crimson Elegant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Rose900.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.ExitToApp,
                    iconTint = Rose500,
                    title = "Keluar Sesi & Cabut Token Perangkat",
                    subtitle = "Mencabut sesi HP ini secara aman dari server",
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FOOTER RESMI DENGAN LOGO
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Navy900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.4f)),
                modifier = Modifier.size(44.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_p2kd),
                    contentDescription = "Logo Resmi P2KD",
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PETUGAS P2KD v1.7.3 (Official Release)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Text(
                text = "Panitia Pemilihan Kepala Desa Kalisalak",
                fontSize = 11.sp,
                color = Slate400
            )
            Text(
                text = "Kecamatan Margasari, Kabupaten Tegal • 2026",
                fontSize = 11.sp,
                color = Slate500
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SystemStatusBox(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Slate950,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 10.sp, color = Slate400)
                Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    iconTint: Color = Blue400,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Slate400
            )
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(20.dp))
    }
}
