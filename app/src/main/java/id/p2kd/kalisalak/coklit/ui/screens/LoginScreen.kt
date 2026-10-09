package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.LoginRequest
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    sessionManager: EncryptedSessionManager? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actualSessionManager = sessionManager ?: remember { ApiClient.getSessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showServerSettings by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(actualSessionManager.getServerUrl()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Institution
            Surface(
                color = Blue900.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.4f)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = "Logo",
                        tint = Amber400,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Amber500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "P2KD DESA KALISALAK 2026/2027",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aplikasi Coklit Petugas Lapangan",
                style = MaterialTheme.typography.headlineMedium,
                color = White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Pencocokan & Penelitian Pemilih Berbasis 1 QR 1 Rumah",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Masuk Akun Petugas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )

                    // Error Message Banner
                    if (errorMessage != null) {
                        Surface(
                            color = Rose600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose500)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Rose500,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Username / NIK Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Username atau NIK") },
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
                            unfocusedTextColor = White
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
                            unfocusedTextColor = White
                        )
                    )

                    // Submit Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Username dan kata sandi wajib diisi."
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
                                        errorMessage = response.body()?.message
                                            ?: "Gagal login. Periksa username dan kata sandi."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Gagal terhubung ke server: ${e.message ?: "Periksa koneksi internet."}"
                                } finally {
                                    isLoading = false
                                }
                            }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            disabledContainerColor = Blue800.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Memverifikasi...", color = White, fontWeight = FontWeight.Bold)
                        } else {
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

            // Server Settings Toggle
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { showServerSettings = !showServerSettings }) {
                Text(
                    text = if (showServerSettings) "Sembunyikan Pengaturan Server" else "Pengaturan Server Backend",
                    color = Slate400,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showServerSettings) {
                Surface(
                    color = Navy900.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Alamat Server Backend:", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                        Button(
                            onClick = {
                                actualSessionManager.setServerUrl(serverUrlInput.trim())
                                showServerSettings = false
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700)
                        ) {
                            Text("Simpan URL", color = White)
                        }
                    }
                }
            }
        }
    }
}



