package id.p2kd.kalisalak.coklit.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.local.OfflineQueueManager
import id.p2kd.kalisalak.coklit.data.models.ChangePasswordRequest
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
    val coroutineScope = rememberCoroutineScope()
    val user = remember { sessionManager.getUserProfile() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val queueCount = remember { offlineQueue.getQueue().size }

    // Dialog Ganti Kata Sandi
    if (showPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPasswordDialog = false },
            title = { Text("Ganti Kata Sandi Akun", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Perbarui kata sandi akun resmi Anda:", fontSize = 12.sp, color = Slate400)

                    if (passwordError != null) {
                        Surface(
                            color = Rose900.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(
                                text = passwordError!!,
                                fontSize = 11.sp,
                                color = Rose400,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Kata Sandi Lama", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata Sandi Baru (Min. 6 Karakter)", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Kata Sandi Baru", fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isPasswordVisible = !isPasswordVisible }
                    ) {
                        Checkbox(
                            checked = isPasswordVisible,
                            onCheckedChange = { isPasswordVisible = it }
                        )
                        Text("Tampilkan Kata Sandi", fontSize = 12.sp, color = Slate300)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Kata sandi baru minimal 6 karakter."
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
                                    snackbarMessage = "Kata sandi akun berhasil diubah!"
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
                    enabled = !isSubmitting && oldPassword.isNotBlank() && newPassword.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Kata Sandi", color = White)
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

    // Dialog Info Regulasi
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Regulasi & Tahapan Pilkades", fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("SK Panitia Pemilihan Kepala Desa (P2KD) Kalisalak 2026", fontWeight = FontWeight.Bold, color = Blue400, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. Pencocokan dan Penelitian (Coklit) DPT/DPS dilakukan langsung dari rumah ke rumah oleh Petugas Pantarlih.

" +
                        "2. Pemilih Tidak Memenuhi Syarat (TMS) wajib didasarkan pada 8 alasan resmi perundang-undangan.

" +
                        "3. Stiker fisik ber-QR ditempelkan pada bagian rumah warga yang mudah dilihat setelah pendataan selesai.

" +
                        "4. Setiap akun petugas dilindungi sistem Single Active Device (1 HP = 1 Akun Aktif).",
                        fontSize = 12.sp,
                        color = Slate300
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Blue600)) {
                    Text("Tutup", color = White)
                }
            },
            containerColor = Navy900
        )
    }

    // Dialog Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Rose500) },
            title = { Text("Konfirmasi Keluar Sesi", fontWeight = FontWeight.Bold, color = White) },
            text = { Text("Apakah Anda yakin ingin keluar? Token sesi dan sesi aktif pada perangkat ini akan diakhiri secara aman.", color = Slate300) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Keluar", color = White)
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
        // Feedback message
        if (snackbarMessage != null) {
            Surface(
                color = Emerald950,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = snackbarMessage!!, fontSize = 12.sp, color = Emerald300, modifier = Modifier.weight(1f))
                    IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 1. Officer Profile Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Blue900.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user?.nama ?: "Petugas P2KD",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Text(
                        text = "@" + (user?.username ?: "petugas"),
                        fontSize = 13.sp,
                        color = Blue400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Blue600.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Wilayah: " + (user?.assignedRw ?: user?.assignedTps ?: "Kalisalak"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Blue300,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 2. Akun & Keamanan Group
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Navy900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.LockReset,
                    iconTint = Amber400,
                    title = "Ganti Kata Sandi Akun",
                    subtitle = "Perbarui sandi login mandiri petugas",
                    onClick = { showPasswordDialog = true }
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.Security,
                    iconTint = Emerald400,
                    title = "Status Keamanan Sesi",
                    subtitle = "1 Akun = 1 Perangkat Aktif (Single Device)",
                    onClick = {
                        snackbarMessage = "Akun Anda terlindungi dengan sistem sesi tunggal."
                    }
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.Sync,
                    iconTint = Blue400,
                    title = "Riwayat Sinkronisasi & Offline",
                    subtitle = "$queueCount antrean tersimpan di memori HP",
                    onClick = onNavigateToSync
                )
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                MoreMenuItem(
                    icon = Icons.Default.Gavel,
                    iconTint = Indigo400,
                    title = "Regulasi & SK P2KD",
                    subtitle = "Panduan & aturan Coklit Pilkades 2026",
                    onClick = { showInfoDialog = true }
                )
            }
        }

        // 3. Security & Logout Action
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
                    title = "Keluar Sesi & Cabut Token",
                    subtitle = "Mencabut sesi perangkat secara aman dari server",
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // App Version Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PETUGAS P2KD v1.6.0 (Official Field Release)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Blue400
            )
            Text(
                text = "Panitia Pemilihan Kepala Desa Kalisalak",
                fontSize = 11.sp,
                color = Slate400
            )
            Text(
                text = "Kecamatan Margasari, Kabupaten Tegal",
                fontSize = 11.sp,
                color = Slate500
            )
        }

        Spacer(modifier = Modifier.height(70.dp))
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
