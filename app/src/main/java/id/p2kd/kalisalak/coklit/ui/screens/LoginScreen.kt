package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.LoginRequest
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    sessionManager: EncryptedSessionManager? = null,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val actualSessionManager = remember { sessionManager ?: ApiClient.getSessionManager(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Navy950,
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
                        listOf(Navy950, Navy900, Navy950)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. LOGO RESMI P2KD KALISALAK (High Resolution Raster)
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Amber400.copy(alpha = 0.6f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(92.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_p2kd),
                        contentDescription = "Logo Resmi P2KD Kalisalak",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Badge Institusi Resmi
                Surface(
                    color = Amber500.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "PANITIA PEMILIHAN KEPALA DESA",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PETUGAS P2KD",
                    style = MaterialTheme.typography.headlineLarge,
                    color = White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Aplikasi Operasional Coklit Pemilih Pilkades Kalisalak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate300,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 2. KARTU LOGIN KREDENSIAL RESMI
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Blue600.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LockPerson,
                                        contentDescription = null,
                                        tint = Blue400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Masuk Kredensial Resmi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                                Text(
                                    text = "Gunakan akun petugas yang telah terdaftar di database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Error Message Banner
                        if (errorMessage != null) {
                            Surface(
                                color = Rose600.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Username / NIK Field
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = null
                            },
                            label = { Text("Username / NIK Petugas") },
                            placeholder = { Text("Contoh: marufah atau 332801...") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Blue400)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedLabelColor = Blue400,
                                unfocusedLabelColor = Slate400
                            )
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Kata Sandi") },
                            placeholder = { Text("Masukkan kata sandi akun") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Blue400)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Slate400
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedLabelColor = Blue400,
                                unfocusedLabelColor = Slate400
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button (Official Backend Authentication)
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
                                                    "Gagal masuk (Kode HTTP ${response.code()}). Silakan coba lagi."
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
                                    color = White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Memverifikasi Kredensial...", color = White, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Masuk Sebagai Petugas",
                                    color = White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
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
                        tint = Slate500,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Sistem Terotentikasi & Terenkripsi AES-256 P2KD Kalisalak",
                        color = Slate500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
