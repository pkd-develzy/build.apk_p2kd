package id.p2kd.kalisalak.coklit.ui.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.firebase.FirebaseAuthHelper
import id.p2kd.kalisalak.coklit.data.models.LoginRequest
import id.p2kd.kalisalak.coklit.data.models.UserProfile
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    sessionManager: EncryptedSessionManager? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val actualSessionManager = sessionManager ?: remember { ApiClient.getSessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Phone Auth states
    var showPhoneAuthDialog by remember { mutableStateOf(false) }
    var phoneNumberInput by remember { mutableStateOf("+62") }
    var otpCodeInput by remember { mutableStateOf("") }
    var verificationIdReceived by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var phoneAuthError by remember { mutableStateOf<String?>(null) }



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

                    // Username / Email Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Username / Email / NIK") },
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

                    // Submit Button (Email/Password or Backend Login)
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Username/Email dan kata sandi wajib diisi."
                            } else {
                                isLoading = true
                                errorMessage = null

                                coroutineScope.launch {
                                    try {
                                        // 1. Coba Firebase Auth jika format email
                                        if (username.contains("@")) {
                                            try {
                                                val firebaseAuthResult = FirebaseAuthHelper.signInWithEmail(username, password)
                                                val firebaseUser = firebaseAuthResult.user
                                                if (firebaseUser != null) {
                                                    val dummyProfile = UserProfile(
                                                        id = firebaseUser.uid,
                                                        username = firebaseUser.email ?: username,
                                                        nama = firebaseUser.displayName ?: username.substringBefore("@"),
                                                        jabatan = "Petugas Pantarlih",
                                                        assignedRw = "RW 01",
                                                        assignedTps = "TPS 01"
                                                    )
                                                    actualSessionManager.saveSession("firebase-token-", dummyProfile)
                                                    onLoginSuccess()
                                                    return@launch
                                                }
                                            } catch (fe: Exception) {
                                                // Lanjut ke backend web login
                                            }
                                        }

                                        // 2. Login standar via backend API P2KD
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
                                        errorMessage = "Gagal terhubung: "
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

                    // Divider Opsi Firebase Authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Navy700)
                        Text(
                            text = "  atau via Firebase  ",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Navy700)
                    }

                    // Phone Auth Button (SMS OTP)
                    OutlinedButton(
                        onClick = {
                            phoneAuthError = null
                            showPhoneAuthDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber400),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Masuk via Nomor HP (SMS OTP)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }


        }
    }

    // Modal Dialog Phone Auth (SMS OTP)
    if (showPhoneAuthDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneAuthDialog = false },
            title = { Text("Login Nomor HP (Firebase OTP)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (phoneAuthError != null) {
                        Text(phoneAuthError!!, color = Rose500, fontSize = 12.sp)
                    }

                    if (verificationIdReceived == null) {
                        Text("Masukkan nomor HP terdaftar (format +62...):", fontSize = 13.sp)
                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("+6281234567890") }
                        )
                    } else {
                        Text("Masukkan 6 digit kode OTP yang dikirimkan via SMS:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { otpCodeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("123456") }
                        )
                    }
                }
            },
            confirmButton = {
                if (verificationIdReceived == null) {
                    Button(
                        onClick = {
                            if (activity != null && phoneNumberInput.isNotBlank()) {
                                isSendingOtp = true
                                phoneAuthError = null
                                FirebaseAuthHelper.startPhoneVerification(
                                    phoneNumber = phoneNumberInput.trim(),
                                    activity = activity,
                                    callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                            isSendingOtp = false
                                            coroutineScope.launch {
                                                try {
                                                    val res = FirebaseAuthHelper.signInWithPhoneCredential(credential)
                                                    val u = res.user
                                                    if (u != null) {
                                                        actualSessionManager.saveSession(
                                                            "firebase-phone-",
                                                            UserProfile(id = u.uid, username = u.phoneNumber ?: "pantarlih", nama = "Petugas Pantarlih ()")
                                                        )
                                                        showPhoneAuthDialog = false
                                                        onLoginSuccess()
                                                    }
                                                } catch (e: Exception) {
                                                    phoneAuthError = "Verifikasi gagal: "
                                                }
                                            }
                                        }

                                        override fun onVerificationFailed(e: FirebaseException) {
                                            isSendingOtp = false
                                            phoneAuthError = "Pengiriman SMS gagal: "
                                        }

                                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                            isSendingOtp = false
                                            verificationIdReceived = verificationId
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isSendingOtp
                    ) {
                        Text(if (isSendingOtp) "Mengirim OTP..." else "Kirim Kode SMS")
                    }
                } else {
                    Button(
                        onClick = {
                            if (verificationIdReceived != null && otpCodeInput.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        val res = FirebaseAuthHelper.signInWithSmsCode(verificationIdReceived!!, otpCodeInput.trim())
                                        val u = res.user
                                        if (u != null) {
                                            actualSessionManager.saveSession(
                                                "firebase-phone-",
                                                UserProfile(id = u.uid, username = u.phoneNumber ?: "pantarlih", nama = "Petugas Pantarlih ()")
                                            )
                                            showPhoneAuthDialog = false
                                            onLoginSuccess()
                                        }
                                    } catch (e: Exception) {
                                        phoneAuthError = "Kode OTP tidak valid: "
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Verifikasi & Masuk")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneAuthDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}