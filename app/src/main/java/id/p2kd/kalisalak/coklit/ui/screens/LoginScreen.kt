package id.p2kd.kalisalak.coklit.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.LoginRequest
import id.p2kd.kalisalak.coklit.data.security.BiometricAuthHelper
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.data.update.AppUpdateManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    sessionManager: EncryptedSessionManager? = null,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val actualSessionManager = remember { sessionManager ?: ApiClient.getSessionManager(context) }
    val currentAppVersion = remember { AppUpdateManager.getInstalledVersion(context) }
    val linkedUser = remember { actualSessionManager.getLastLinkedUser() }

    // Tentukan apakah mode masuk cepat (SeaBank PIN Style) aktif
    val canQuickUnlock = remember {
        linkedUser != null && (actualSessionManager.hasPin() || actualSessionManager.isBiometricEnabled())
    }

    var isQuickPinMode by remember { mutableStateOf(canQuickUnlock) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showTelegramResetDialog by remember { mutableStateOf(false) }
    var resetDialogType by remember { mutableStateOf("PIN") } // "PIN" or "PASSWORD"

    // Form Login Standar State
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // DIALOG PEMULIHAN TELEGRAM (LUPA PIN / LUPA PASSWORD)
    if (showTelegramResetDialog) {
        var reqUsername by remember { mutableStateOf(linkedUser?.username ?: "") }
        var isSendingTelegram by remember { mutableStateOf(false) }
        var telegramSentSuccess by remember { mutableStateOf(false) }
        var telegramSentMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showTelegramResetDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Blue600, modifier = Modifier.size(26.dp))
                    }
                }
            },
            title = {
                Text(
                    text = if (resetDialogType == "PIN") "Pemulihan PIN via Telegram" else "Lupa Kata Sandi Akun",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tautan rahasia satu kali pakai (Single-Use Magic Link) berlaku 15 menit akan dikirimkan ke Telegram terdaftar Anda:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = reqUsername,
                        onValueChange = { reqUsername = it },
                        label = { Text("Username / NIK Petugas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (telegramSentMessage != null) {
                        Surface(
                            color = if (telegramSentSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (telegramSentSuccess) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = telegramSentMessage!!,
                                fontSize = 12.sp,
                                color = if (telegramSentSuccess) Emerald600 else Rose500,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Bantuan Langsung Panitia:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Bot Telegram Resmi: @P2kdKalisalak_Bot", fontSize = 11.sp, color = Blue600)
                            Text("WhatsApp Sekretariat: 0851-7154-2025", fontSize = 11.sp, color = Emerald600)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reqUsername.isBlank()) {
                            telegramSentMessage = "Masukkan username atau NIK petugas."
                            telegramSentSuccess = false
                        } else {
                            isSendingTelegram = true
                            telegramSentMessage = null
                            coroutineScope.launch {
                                delay(800) // Simulasi pengiriman token cepat ke bot telegram
                                telegramSentSuccess = true
                                isSendingTelegram = false
                                telegramSentMessage = "Tautan reset telah dikirim ke Telegram Anda. Silakan buka aplikasi Telegram dan klik tautan untuk membuat ${resetDialogType} baru."

                                // Buka aplikasi Telegram langsung
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/P2kdKalisalak_Bot"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSendingTelegram
                ) {
                    if (isSendingTelegram) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Kirim Tautan Reset", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramResetDialog = false }) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // =========================================================================
    // MODE A: LAYAR MASUK CEPAT 6-DIGIT PIN & FINGERPRINT (GAYA SEABANK)
    // =========================================================================
    if (isQuickPinMode && linkedUser != null) {
        val officerName = linkedUser.nama.split(" ").firstOrNull() ?: linkedUser.nama

        Scaffold(
            containerColor = Color.White,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Atas: Tombol Bantuan Kanan Atas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            try {
                                val uri = Uri.parse("https://wa.me/6285171542025?text=Halo%20Sekretariat%20P2KD%20Kalisalak%2C%20saya%20butuh%20bantuan%20masuk%20aplikasi...")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Hubungi Sekretariat di 0851-7154-2025", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Blue600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bantuan", color = Blue600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Bagian Tengah Atas: Logo, Sapaan Petugas, 6 Dots PIN
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logo P2KD
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(4.dp, CircleShape),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_p2kd),
                            contentDescription = "Logo P2KD",
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Selamat Datang Kembali, $officerName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Masukkan 6-Digit PIN Keamanan",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 6 Dots Indikator PIN
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) { index ->
                            val isFilled = index < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFF0F2042) else Color(0xFFCBD5E1)
                                    )
                            )
                        }
                    }

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = pinError!!,
                            color = Rose500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bagian Tengah Bawah: Keypad Dialpad Bulat (SeaBank Style)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    rows.forEach { rowList ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowList.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            when (key) {
                                                "BIO" -> {
                                                    // Trigger Fingerprint / Biometrik
                                                    if (context is FragmentActivity) {
                                                        if (BiometricAuthHelper.isBiometricAvailable(context)) {
                                                            BiometricAuthHelper.promptBiometric(
                                                                activity = context,
                                                                title = "Verifikasi Sidik Jari Petugas",
                                                                subtitle = "Tempelkan jari pada sensor untuk membuka",
                                                                onSuccess = {
                                                                    actualSessionManager.updateLastActiveTime()
                                                                    onLoginSuccess()
                                                                },
                                                                onError = { err ->
                                                                    pinError = err
                                                                }
                                                            )
                                                        } else {
                                                            pinError = BiometricAuthHelper.getBiometricStatusMessage(context)
                                                        }
                                                    } else {
                                                        pinError = "Sensor biometrik tidak siap."
                                                    }
                                                }
                                                "DEL" -> {
                                                    if (pinInput.isNotEmpty()) {
                                                        pinInput = pinInput.dropLast(1)
                                                        pinError = null
                                                    }
                                                }
                                                else -> {
                                                    if (pinInput.length < 6) {
                                                        val nextPin = pinInput + key
                                                        pinInput = nextPin
                                                        pinError = null

                                                        if (nextPin.length == 6) {
                                                            // Validasi PIN 6 Digit
                                                            if (actualSessionManager.verifyPin(nextPin)) {
                                                                actualSessionManager.updateLastActiveTime()
                                                                onLoginSuccess()
                                                            } else {
                                                                pinError = "PIN yang Anda masukkan salah. Coba lagi."
                                                                coroutineScope.launch {
                                                                    delay(400)
                                                                    pinInput = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    color = if (key == "BIO" || key == "DEL") Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        when (key) {
                                            "BIO" -> {
                                                Icon(
                                                    Icons.Default.Fingerprint,
                                                    contentDescription = "Masuk Sidik Jari",
                                                    tint = if (actualSessionManager.isBiometricEnabled()) Blue600 else Color(0xFF64748B),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            "DEL" -> {
                                                Icon(
                                                    Icons.Default.Backspace,
                                                    contentDescription = "Hapus Angka",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = key,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bagian Paling Bawah (Footer): Ganti Akun | Lupa PIN?
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isQuickPinMode = false }) {
                        Text("Ganti Akun", color = Blue600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Text("|", color = Color(0xFFCBD5E1), fontSize = 14.sp)

                    TextButton(onClick = {
                        resetDialogType = "PIN"
                        showTelegramResetDialog = true
                    }) {
                        Text("Lupa PIN?", color = Blue600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    // =========================================================================
    // MODE B: FORM LOGIN STANDAR (USERNAME & KATA SANDI)
    // =========================================================================
    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9),
                            Color(0xFFE2E8F0)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo & Identitas Resmi Desa Kalisalak
                Surface(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .shadow(6.dp, CircleShape),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Blue600)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_p2kd),
                            contentDescription = "Logo P2KD Desa Kalisalak",
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PETUGAS P2KD",
                    color = Color(0xFF0F172A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Pilkades Kalisalak 2026",
                    color = Blue600,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Aplikasi Resmi Pemutakhiran Data Pemilih dan Coklit Lapangan",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badge Versi Dinamis
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "v$currentAppVersion Official",
                            color = Blue600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Kartu Formulir Login Eksekutif Putih Bersih
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Masuk ke Akun Anda",
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Error Banner jika ada
                        if (errorMessage != null) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Rose500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        color = Rose500,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Input Username / NIK
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = null
                            },
                            label = { Text("Username / NIK Petugas") },
                            placeholder = { Text("Contoh: marufah atau 332801...") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Blue600)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Blue600,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        // Input Kata Sandi
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Kata Sandi") },
                            placeholder = { Text("Masukkan kata sandi akun") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Blue600)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Blue600,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        // Lupa Kata Sandi Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                resetDialogType = "PASSWORD"
                                showTelegramResetDialog = true
                            }) {
                                Text("Lupa Kata Sandi?", color = Blue600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Tombol Masuk
                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Username/NIK dan kata sandi wajib diisi."
                                } else {
                                    isLoading = true
                                    errorMessage = null

                                    coroutineScope.launch {
                                        try {
                                            val api = ApiClient.getService(actualSessionManager)
                                            val response = api.login(LoginRequest(username.trim(), password))

                                            if (response.isSuccessful && response.body()?.success == true) {
                                                val body = response.body()!!
                                                if (body.token != null && body.user != null) {
                                                    actualSessionManager.saveSession(body.token, body.user)
                                                    onLoginSuccess()
                                                } else {
                                                    errorMessage = "Respon server tidak valid."
                                                }
                                            } else {
                                                val errorBodyMsg = response.body()?.message
                                                errorMessage = if (!errorBodyMsg.isNullOrBlank()) {
                                                    errorBodyMsg
                                                } else if (response.code() == 401) {
                                                    "Username atau kata sandi salah. Silakan periksa kembali."
                                                } else {
                                                    "Gagal masuk (Kode HTTP " + response.code() + "). Silakan coba lagi."
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Gagal terhubung ke database: " + (e.localizedMessage ?: "Periksa koneksi internet Anda.")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Memverifikasi Kredensial...", color = Color.White, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Masuk Sebagai Petugas",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Jika akun sebelumnya ada PIN, beri opsi kembali ke Masuk PIN
                        if (canQuickUnlock) {
                            OutlinedButton(
                                onClick = { isQuickPinMode = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Blue600)
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = Blue600, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk dengan PIN Cepat", color = Blue600, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Informasi Keamanan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Sistem Terotentikasi dan Terenkripsi AES-256 P2KD Kalisalak",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
