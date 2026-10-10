package id.p2kd.kalisalak.coklit.ui.screens

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var isReadyToExit by remember { mutableStateOf(false) }

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
        delay(2800L)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
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

        // 2. Indikator Loading Bawah & Tombol Lewati
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
