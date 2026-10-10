package id.p2kd.kalisalak.coklit.ui.screens

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var showWelcomeText by remember { mutableStateOf(false) }

    // Inisialisasi Coil ImageLoader dengan GIF Decoder
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // Durasi putar animasi welcome screen (~3 detik) lalu otomatis masuk
    LaunchedEffect(Unit) {
        delay(200L)
        showWelcomeText = true
        delay(3200L)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy950, Color(0xFF070D18), Navy950)
                )
            )
            .clickable { onFinish() }, // Bisa di-tap untuk skip instan
        contentAlignment = Alignment.Center
    ) {
        // 1. Tampilan Utama Animasi GIF Welcome Screen
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.raw.welcome_screen)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Welcome Screen Pantarlih",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // 2. Banner Teks Berjalan Kaligrafi Elegan (Gaya Apple / iOS Welcome)
        AnimatedVisibility(
            visible = showWelcomeText,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 20.dp, end = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Kaligrafi iOS-style Cursive Title
                Text(
                    text = "Selamat Datang Petugas",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            delayMillis = 800,
                            velocity = 45.dp
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle Badge Elegan
                Surface(
                    color = Blue950.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "PILKADES KALISALAK 2026",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue300,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 3. Indikator Loading Bawah & Tombol Lewati
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Blue400,
                strokeWidth = 2.5.dp
            )
            Text(
                text = "Memuat Sistem Coklit Pilkades Kalisalak...",
                fontSize = 12.sp,
                color = Slate400,
                fontWeight = FontWeight.Medium
            )
            Surface(
                color = Navy900.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.clickable { onFinish() }
            ) {
                Text(
                    text = "Ketuk untuk Masuk Langsung",
                    fontSize = 11.sp,
                    color = Blue300,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
