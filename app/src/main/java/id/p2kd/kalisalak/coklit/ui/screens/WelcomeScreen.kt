package id.p2kd.kalisalak.coklit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    // Teks lengkap yang akan ditulis otomatis berjalan (Typewriter effect)
    val fullTitle = "Selamat Datang Petugas"
    val fullSubtitle = "Pilkades Kalisalak 2026"

    var displayedTitle by remember { mutableStateOf("") }
    var displayedSubtitle by remember { mutableStateOf("") }
    var isLogoVisible by remember { mutableStateOf(false) }
    var isCursorVisible by remember { mutableStateOf(true) }

    // Efek kursor berkedip
    LaunchedEffect(Unit) {
        while (true) {
            delay(450L)
            isCursorVisible = !isCursorVisible
        }
    }

    // Efek animasi menulis huruf demi huruf (Typewriter Animation)
    LaunchedEffect(Unit) {
        delay(200L)
        isLogoVisible = true
        delay(400L)

        // Tulis baris pertama
        for (i in 1..fullTitle.length) {
            displayedTitle = fullTitle.substring(0, i)
            delay(55L)
        }

        delay(150L)

        // Tulis baris kedua
        for (j in 1..fullSubtitle.length) {
            displayedSubtitle = fullSubtitle.substring(0, j)
            delay(45L)
        }

        // Tahan sejenak setelah selesai menulis lalu otomatis masuk
        delay(1200L)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFEEF2F6)
                    )
                )
            )
            .clickable { onFinish() }, // Bisa disentuh di mana saja untuk skip langsung
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // 1. LOGO RESMI PANTARLIH DI TENGAH LAYAR
            AnimatedVisibility(
                visible = isLogoVisible,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.7f, animationSpec = tween(600))
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier.size(125.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_p2kd),
                            contentDescription = "Logo Resmi Pantarlih P2KD",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. TEKS BERJALAN MENULIS (TYPEWRITER EFFECT)
            Text(
                text = displayedTitle + if (displayedSubtitle.isEmpty() && isCursorVisible) "|" else "",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F2042), // Deep Executive Navy
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayedSubtitle + if (displayedSubtitle.isNotEmpty() && isCursorVisible) "|" else "",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB), // Blue Accent
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. LENCANA RESMI SEKRETARIAT
            Surface(
                color = Color(0xFF0F2042),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "KOMISI PEMILIHAN KEPALA DESA KALISALAK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCD34D), // Amber Gold
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 4. FOOTER BAWAH: INDIKATOR LOADING HALUS & TOMBOL LEWATI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF1E3A8A),
                strokeWidth = 2.dp
            )
            Text(
                text = "Ketuk layar untuk masuk langsung",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
