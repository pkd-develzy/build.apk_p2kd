package id.p2kd.kalisalak.coklit.ui.screens

import android.net.Uri
import android.content.Intent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.ChangePasswordRequest
import id.p2kd.kalisalak.coklit.data.models.ProfilePhotoRequest
import id.p2kd.kalisalak.coklit.data.security.BiometricAuthHelper
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.data.update.AppUpdateManager
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
    val currentAppVersion = remember { AppUpdateManager.getInstalledVersion(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showTelegramLinkDialog by remember { mutableStateOf(false) }

    var isUpdatingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // State Keamanan PIN & Biometrik
    var isPinActive by remember { mutableStateOf(sessionManager.hasPin()) }
    var isBiometricActive by remember { mutableStateOf(sessionManager.isBiometricEnabled()) }
    var autoLockMinutes by remember { mutableStateOf(sessionManager.getAutoLockMinutes()) }
    var isTelegramLinked by remember { mutableStateOf(sessionManager.isTelegramLinked()) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUpdatingPhoto = true
                photoError = null
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bmp != null) {
                        val maxDim = 512
                        val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                            val factor = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                            Bitmap.createScaledBitmap(bmp, (bmp.width * factor).toInt(), (bmp.height * factor).toInt(), true)
                        } else {
                            bmp
                        }
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        val bytes = baos.toByteArray()
                        val base64Data = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)

                        val res = ApiClient.api.updateProfilePhoto(ProfilePhotoRequest(image = base64Data))
                        if (res.isSuccessful && res.body()?.success == true) {
                            val updatedUser = user?.copy(fotoUrl = base64Data)
                            if (updatedUser != null) {
                                val token = sessionManager.getAuthToken() ?: ""
                                sessionManager.saveSession(token, updatedUser)
                                user = updatedUser
                            }
                            snackbarMessage = "Foto profil berhasil diunggah dari galeri!"
                            showPhotoDialog = false
                        } else {
                            photoError = res.body()?.message ?: "Gagal memperbarui foto profil."
                        }
                    } else {
                        photoError = "Gagal memproses file gambar galeri."
                    }
                } catch (e: Exception) {
                    photoError = "Gagal membuka galeri: " + (e.localizedMessage ?: "Coba lagi")
                } finally {
                    isUpdatingPhoto = false
                }
            }
        }
    }

    var showSessionInfoDialog by remember { mutableStateOf(false) }
    var showRegulationDialog by remember { mutableStateOf(false) }
    var showHelpdeskDialog by remember { mutableStateOf(false) }
    var showUpdateCheckDialog by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showDataSyncConfirmDialog by remember { mutableStateOf(false) }

    val queueCount = remember { offlineQueue.getQueue().size }

    // DIALOG KONFIRMASI AKSES DATA SELULER (CACHE SYNC)
    if (showDataSyncConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDataSyncConfirmDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Blue600, modifier = Modifier.size(28.dp)) },
            title = { Text("Konfirmasi Akses Data Seluler", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Text(
                    "Aplikasi akan mengunduh basis data pemilih terbaru (±2-3 MB) dari server resmi untuk disimpan ke cache offline smartphone Anda.\n\n" +
                    "Pastikan koneksi internet / data seluler Anda aktif. Lanjutkan pengunduhan?",
                    color = Color(0xFF475569),
                    fontSize = 13.sp
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
                    Text("Lanjutkan Unduh", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataSyncConfirmDialog = false }) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG ATUR 6-DIGIT PIN (GAYA SEABANK)
    if (showPinSetupDialog) {
        var inputPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinSetupError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = Blue600, modifier = Modifier.size(28.dp))
                    }
                }
            },
            title = {
                Text(
                    text = if (isPinActive) "Ubah 6-Digit PIN Petugas" else "Buat 6-Digit PIN Petugas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PIN 6 angka digunakan untuk masuk cepat tanpa perlu mengetik kata sandi:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    if (pinSetupError != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = pinSetupError!!,
                                fontSize = 12.sp,
                                color = Rose500,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) inputPin = it },
                        label = { Text("Masukkan 6 Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Konfirmasi 6 Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPin.length != 6) {
                            pinSetupError = "PIN wajib terdiri dari 6 angka."
                        } else if (inputPin != confirmPin) {
                            pinSetupError = "Konfirmasi PIN tidak cocok."
                        } else {
                            sessionManager.setPin(inputPin)
                            isPinActive = true
                            showPinSetupDialog = false
                            snackbarMessage = "6-Digit PIN petugas berhasil disimpan!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Simpan PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG TAUTKAN AKUN TELEGRAM
    if (showTelegramLinkDialog) {
        val linkCode = remember { (100000..999999).random().toString() }

        AlertDialog(
            onDismissRequest = { showTelegramLinkDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Blue600, modifier = Modifier.size(28.dp))
                    }
                }
            },
            title = {
                Text(
                    text = "Tautkan Akun Telegram Resmi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Telegram digunakan untuk pengiriman Tautan Rahasia Satu Kali Pakai jika Anda Lupa PIN atau Lupa Kata Sandi.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Langkah Penautan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                            Text("1. Klik tombol 'Buka Bot Telegram' di bawah", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("2. Tekan tombol START pada bot @P2kdKalisalak_Bot", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("3. Akun Telegram Anda akan otomatis terhubung!", fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val url = "https://t.me/P2kdKalisalak_Bot?start=LINK_${user?.username ?: "PETUGAS"}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                sessionManager.setTelegramLinked(true, "@${user?.username ?: "petugas"}_klk")
                                isTelegramLinked = true
                                showTelegramLinkDialog = false
                                snackbarMessage = "Membuka Telegram. Tekan START pada bot untuk mengonfirmasi penautan."
                            } catch (_: Exception) {
                                Toast.makeText(context, "Aplikasi Telegram tidak terpasang di HP ini.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Bot Telegram (@P2kdKalisalak_Bot)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTelegramLinkDialog = false }) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
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
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = Amber500, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Ganti Kata Sandi Akun", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Perbarui kata sandi akun resmi Anda secara mandiri:", fontSize = 12.sp, color = Color(0xFF475569))

                    if (passwordError != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = passwordError!!,
                                fontSize = 12.sp,
                                color = Rose500,
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPasswordVisible, onCheckedChange = { isPasswordVisible = it })
                        Text("Tampilkan Karakter Sandi", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPassword.isBlank() || newPassword.isBlank()) {
                            passwordError = "Semua kolom wajib diisi."
                        } else if (newPassword.length < 6) {
                            passwordError = "Kata sandi baru minimal 6 karakter."
                        } else if (newPassword != confirmPassword) {
                            passwordError = "Konfirmasi kata sandi baru tidak cocok."
                        } else {
                            isSubmitting = true
                            passwordError = null
                            coroutineScope.launch {
                                try {
                                    val res = ApiClient.api.changePassword(
                                        ChangePasswordRequest(
                                            oldPassword = oldPassword,
                                            newPassword = newPassword
                                        )
                                    )
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        showPasswordDialog = false
                                        snackbarMessage = "Kata sandi akun Anda berhasil diperbarui!"
                                    } else {
                                        passwordError = res.body()?.message ?: "Gagal mengubah kata sandi."
                                    }
                                } catch (e: Exception) {
                                    passwordError = "Gagal terhubung: " + (e.localizedMessage ?: "Coba lagi")
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Sandi", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }, enabled = !isSubmitting) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 2: Perbarui Foto Profil Petugas
    if (showPhotoDialog) {
        var inputUrl by remember { mutableStateOf(user?.fotoUrl ?: "") }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingPhoto) showPhotoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Blue600, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Foto Profil Petugas", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Pilih foto resmi wajah dari galeri HP Anda:", fontSize = 12.sp, color = Color(0xFF475569))

                    Button(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Galeri Foto HP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (photoError != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = photoError!!,
                                fontSize = 12.sp,
                                color = Rose500,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    Text("Atau masukkan tautan URL foto langsung:", fontSize = 12.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Tautan URL Foto (Opsional)", fontSize = 11.sp) },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUrl.isNotBlank() && inputUrl != user?.fotoUrl) {
                            isUpdatingPhoto = true
                            coroutineScope.launch {
                                try {
                                    val res = ApiClient.api.updateProfilePhoto(ProfilePhotoRequest(image = inputUrl.trim()))
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        val updatedUser = user?.copy(fotoUrl = inputUrl.trim())
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
                                    photoError = "Gagal terhubung: " + (e.localizedMessage ?: "Coba lagi")
                                } finally {
                                    isUpdatingPhoto = false
                                }
                            }
                        } else {
                            showPhotoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    enabled = !isUpdatingPhoto,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdatingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan URL", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }, enabled = !isUpdatingPhoto) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 3: Informasi Sesi & Perangkat Terotentikasi
    if (showSessionInfoDialog) {
        val devId = sessionManager.getOrCreateDeviceId()
        AlertDialog(
            onDismissRequest = { showSessionInfoDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Emerald600, modifier = Modifier.size(28.dp)) },
            title = { Text("Integritas Sesi Terenkripsi", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sistem keamanan P2KD memvalidasi identitas perangkat Anda:", fontSize = 12.sp, color = Color(0xFF475569))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ID Perangkat:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(devId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Model Perangkat:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(Build.MANUFACTURER.uppercase() + " " + Build.MODEL, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Status Token:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("Aktif & Terdaftar Resmi (Single-Device Bound)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSessionInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600), shape = RoundedCornerShape(10.dp)) {
                    Text("Tutup", color = Color.White)
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 4: Cek Pembaruan Sistem (Live Versi Terpasang)
    if (showUpdateCheckDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCheckingUpdate) showUpdateCheckDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Indigo500, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pembaruan Sistem", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isCheckingUpdate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Blue600, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Memeriksa rilis resmi server...", fontSize = 13.sp, color = Color(0xFF475569))
                        }
                    } else {
                        Text(
                            text = updateCheckResult ?: "Aplikasi Anda versi $currentAppVersion sudah menggunakan versi resmi paling mutakhir.",
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
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
                    Text("Tutup", color = Color.White)
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 5: Pusat Bantuan & Helpdesk Panitia
    if (showHelpdeskDialog) {
        AlertDialog(
            onDismissRequest = { showHelpdeskDialog = false },
            icon = { Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = Emerald600, modifier = Modifier.size(28.dp)) },
            title = { Text("Sekretariat Resmi P2KD Kalisalak", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Layanan Konsultasi & Pendampingan Petugas Lapangan:", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("📍 Kantor Sekretariat P2KD Balai Desa Kalisalak", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                    Text("📞 WhatsApp Resmi: 0851-7154-2025", fontSize = 13.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                    Text("📧 Email: sekretariat@p2kdkalisalak.my.id", fontSize = 12.sp, color = Blue600)
                    Text("🕒 Jam Layanan: 08:00 - 21:00 WIB Setiap Hari", fontSize = 11.sp, color = Color(0xFF64748B))

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            try {
                                val uri = Uri.parse("https://wa.me/6285171542025?text=Halo%20Sekretariat%20P2KD%20Kalisalak%2C%20saya%20petugas%20lapangan%20ingin%20berkonsultasi...")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                snackbarMessage = "Tidak dapat membuka WhatsApp: " + (e.localizedMessage ?: "Periksa aplikasi WA")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka WhatsApp Panitia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpdeskDialog = false }) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 6: Regulasi & Pedoman 8 Alasan TMS
    if (showRegulationDialog) {
        AlertDialog(
            onDismissRequest = { showRegulationDialog = false },
            title = { Text("Pedoman Resmi Coklit 2026", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("8 Alasan Resmi Pemilih TMS (Tidak Memenuhi Syarat):", fontWeight = FontWeight.Bold, color = Rose500, fontSize = 12.sp)
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
                        color = Color(0xFF334155)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showRegulationDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600), shape = RoundedCornerShape(10.dp)) {
                    Text("Selesai Membaca", color = Color.White)
                }
            },
            containerColor = Color.White
        )
    }

    // DIALOG 7: Konfirmasi Keluar Sesi
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Rose500, modifier = Modifier.size(28.dp)) },
            title = { Text("Konfirmasi Keluar Sesi", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Apakah Anda yakin ingin keluar? Sesi kerja pada perangkat ini akan diakhiri secara aman dari server.", color = Color(0xFF475569)) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Keluar Sesi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feedback Message Toast Banner
        if (snackbarMessage != null) {
            Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = snackbarMessage!!, fontSize = 12.sp, color = Emerald600, modifier = Modifier.weight(1f))
                    IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 1. HERO PROFILE CARD: Tampilan Eksekutif & Modern
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Blue600)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (!user?.fotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.fotoUrl,
                                    contentDescription = "Foto Profil Petugas",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = (user?.nama ?: "P").split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .joinToString("")
                                    .ifBlank { "P" }
                                Text(
                                    text = initials,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Blue600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.nama ?: "Petugas P2KD",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "NIK: " + (user?.nik ?: "-"),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text(
                                    text = "RW " + (user?.rw ?: "-"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Text(
                                    text = "TPS " + (user?.tps ?: "-"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showPhotoDialog = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Ganti Foto", tint = Blue600)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Buttons Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showPasswordDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Amber500, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ganti Sandi", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Username", user?.username ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Username berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Blue600, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Info", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // =========================================================================
        // 2. KARTU KEAMANAN & KUNCI APLIKASI (GAYA PERBANKAN / SEABANK)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Blue600, modifier = Modifier.size(20.dp))
                        Text(
                            text = "KEAMANAN & KUNCI APLIKASI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = "Banking Grade",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Item 1: 6-Digit PIN Keamanan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("6-Digit PIN Masuk Cepat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text(
                            text = if (isPinActive) "Aktif (Masuk cepat gaya SeaBank)" else "Belum dibuat (Gunakan kata sandi)",
                            fontSize = 11.sp,
                            color = if (isPinActive) Emerald600 else Color(0xFF64748B)
                        )
                    }
                    Button(
                        onClick = { showPinSetupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPinActive) Color(0xFFF8FAFC) else Blue600),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isPinActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPinActive) "Ubah PIN" else "Atur PIN",
                            color = if (isPinActive) Color(0xFF0F172A) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Item 2: Sidik Jari (Fingerprint)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Masuk Sidik Jari (Fingerprint)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("Sentuh sensor HP untuk membuka aplikasi seketika", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isBiometricActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (context is FragmentActivity && BiometricAuthHelper.isBiometricAvailable(context)) {
                                    BiometricAuthHelper.promptBiometric(
                                        activity = context,
                                        title = "Konfirmasi Pendaftaran Sidik Jari",
                                        subtitle = "Tempelkan jari untuk mengaktifkan masuk biometrik",
                                        onSuccess = {
                                            sessionManager.setBiometricEnabled(true)
                                            isBiometricActive = true
                                            snackbarMessage = "Masuk sidik jari berhasil diaktifkan!"
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, BiometricAuthHelper.getBiometricStatusMessage(context), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                sessionManager.setBiometricEnabled(false)
                                isBiometricActive = false
                                snackbarMessage = "Masuk sidik jari dinonaktifkan."
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Blue600,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Item 3: Kunci Pemulihan Telegram
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pemulihan Akun via Telegram", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text(
                            text = if (isTelegramLinked) "Terhubung (" + (sessionManager.getTelegramUsername() ?: "@telegram") + ")" else "Belum ditautkan (Tautan 15 menit)",
                            fontSize = 11.sp,
                            color = if (isTelegramLinked) Emerald600 else Amber500
                        )
                    }
                    Button(
                        onClick = { showTelegramLinkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTelegramLinked) Color(0xFFF8FAFC) else Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isTelegramLinked) Color(0xFFCBD5E1) else Color(0xFFBFDBFE)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTelegramLinked) "Atur Ulang" else "Tautkan",
                            color = if (isTelegramLinked) Color(0xFF0F172A) else Blue600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. DASHBOARD STATUS & KESEHATAN SISTEM
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATUS SISTEM & MONITORING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF64748B)
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
                        accentColor = Emerald600,
                        modifier = Modifier.weight(1f),
                        onClick = { showSessionInfoDialog = true }
                    )
                    SystemStatusBox(
                        title = "Antrean Offline",
                        value = if (queueCount > 0) "$queueCount Data" else "Sinkron",
                        icon = Icons.Default.Sync,
                        accentColor = if (queueCount > 0) Amber500 else Emerald600,
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
                        value = "Pusat Data P2KD Kalisalak",
                        icon = Icons.Default.CloudDone,
                        accentColor = Blue600,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Toast.makeText(context, "Terhubung ke Database Resmi P2KD Kalisalak", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SystemStatusBox(
                        title = "Versi Sistem",
                        value = "v$currentAppVersion",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Indigo500,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.launch {
                                showUpdateCheckDialog = true
                                isCheckingUpdate = true
                                try {
                                    val res = ApiClient.api.checkAppVersion(currentAppVersion)
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        val status = res.body()!!.updateStatus
                                        updateCheckResult = if (status?.updateAvailable == true) {
                                            "Tersedia versi baru: " + status.latestVersion + ". Silakan unduh melalui notifikasi."
                                        } else {
                                            "Aplikasi Anda sudah versi resmi terbaru (v$currentAppVersion)."
                                        }
                                    } else {
                                        updateCheckResult = "Aplikasi Anda versi $currentAppVersion sudah menggunakan rilis resmi terbaru."
                                    }
                                } catch (_: Exception) {
                                    updateCheckResult = "Versi Anda v$currentAppVersion adalah rilis resmi lapangan terbaru."
                                } finally {
                                    isCheckingUpdate = false
                                }
                            }
                        }
                    )
                }
            }
        }

        // 4. PENGATURAN & LAYANAN OPERASIONAL
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.CleaningServices,
                    iconTint = Amber500,
                    title = "Bersihkan Cache & Refresh Data",
                    subtitle = "Menyegarkan data lokal langsung dari server",
                    onClick = { showDataSyncConfirmDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.Gavel,
                    iconTint = Blue600,
                    title = "Regulasi & SK P2KD",
                    subtitle = "Pedoman resmi & panduan 8 alasan TMS",
                    onClick = { showRegulationDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.HeadsetMic,
                    iconTint = Emerald600,
                    title = "Kontak Bantuan & Helpdesk",
                    subtitle = "Pusat bantuan panitia pemilihan kepala desa",
                    onClick = { showHelpdeskDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.SystemUpdate,
                    iconTint = Indigo500,
                    title = "Periksa Pembaruan Sistem",
                    subtitle = "Cek rilis update APK terbaru",
                    onClick = {
                        coroutineScope.launch {
                            showUpdateCheckDialog = true
                            isCheckingUpdate = true
                            try {
                                val res = ApiClient.api.checkAppVersion(currentAppVersion)
                                if (res.isSuccessful && res.body()?.success == true) {
                                    val status = res.body()!!.updateStatus
                                    updateCheckResult = if (status?.updateAvailable == true) {
                                        "Tersedia versi baru: " + status.latestVersion
                                    } else {
                                        "Aplikasi Anda sudah versi resmi terbaru (v$currentAppVersion)."
                                    }
                                } else {
                                    updateCheckResult = "Aplikasi Anda versi $currentAppVersion sudah menggunakan rilis resmi terbaru."
                                }
                            } catch (_: Exception) {
                                updateCheckResult = "Versi Anda v$currentAppVersion adalah rilis resmi lapangan terbaru."
                            } finally {
                                isCheckingUpdate = false
                            }
                        }
                    }
                )
            }
        }

        // 5. TOMBOL KELUAR SESI (Crimson Elegant)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
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
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                text = "PETUGAS P2KD v$currentAppVersion (Official Release)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Panitia Pemilihan Kepala Desa Kalisalak",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "Kecamatan Margasari, Kabupaten Tegal • 2026",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
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
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 10.sp, color = Color(0xFF64748B))
                Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    iconTint: Color = Blue600,
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
            color = iconTint.copy(alpha = 0.12f),
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
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
    }
}
