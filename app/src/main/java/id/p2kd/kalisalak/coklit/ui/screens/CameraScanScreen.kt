package id.p2kd.kalisalak.coklit.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import id.p2kd.kalisalak.coklit.ui.theme.*
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScanScreen(
    onQrScanned: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onManualInputClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isProcessingScan by remember { mutableStateOf(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // Scanner laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun parseQrToken(rawText: String): String {
        val trimmed = rawText.trim()
        // If scanned full URL e.g. https://.../stiker-coklit?qr=KLK-HM-01-A1B2
        if (trimmed.contains("qr=")) {
            val queryParam = trimmed.substringAfter("qr=").substringBefore("&")
            if (queryParam.isNotBlank()) return queryParam.uppercase()
        }
        if (trimmed.contains("token=")) {
            val queryParam = trimmed.substringAfter("token=").substringBefore("&")
            if (queryParam.isNotBlank()) return queryParam.uppercase()
        }
        return trimmed.uppercase()
    }

    Box(modifier = Modifier.fillMaxSize().background(Navy950)) {
        // 1. CameraX Native Back Camera View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val cameraExecutor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                    )

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isProcessingScan) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            val now = System.currentTimeMillis()
                                            val token = parseQrToken(rawValue)

                                            // Debounce duplicate prevention (min 2.5s between duplicate scans)
                                            if (token != lastScannedCode || now - lastScannedTime > 2500) {
                                                lastScannedCode = token
                                                lastScannedTime = now
                                                isProcessingScan = true
                                                vibrateSuccess()
                                                onQrScanned(token)
                                                break
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    // Non-blocking analysis error
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        scanErrorMessage = "Gagal menginisialisasi kamera belakang: ${e.message}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. Translucent Cutout Overlay & Scanning Frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanBoxSize = size.width * 0.72f
            val left = (size.width - scanBoxSize) / 2f
            val top = (size.height - scanBoxSize) / 2.2f

            // Dark border around scan area
            drawRect(
                color = Color(0xAA020617),
                size = size
            )

            // Clear cutout window in center
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(scanBoxSize, scanBoxSize),
                cornerRadius = CornerRadius(24f, 24f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Red/Gold Scanning laser line
            val laserY = top + (scanBoxSize * laserYRatio)
            drawLine(
                color = Color(0xFFFBBF24),
                start = Offset(left + 16f, laserY),
                end = Offset(left + scanBoxSize - 16f, laserY),
                strokeWidth = 6f
            )
        }

        // Center Box Decorative Corner Markers
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-30).dp)
                .size(270.dp)
                .border(2.dp, Blue400.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        )

        // 3. Top Action Bar: Back, Title, Torch Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Navy900.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pindai QR Rumah C6",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kamera Belakang • P2KD Kalisalak",
                    style = MaterialTheme.typography.labelSmall,
                    color = Blue400
                )
            }

            IconButton(
                onClick = {
                    val nextState = !isTorchEnabled
                    cameraControl?.enableTorch(nextState)
                    isTorchEnabled = nextState
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Navy900.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = if (isTorchEnabled) Amber400 else Slate400
                )
            }
        }

        // 4. Bottom Instructions & Manual Input Action
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Navy900.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1 QR CODE = 1 RUMAH",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Amber400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Arahkan kamera ke QR Code pada stiker fisik rumah. QR tidak memuat nama; nama ditulis petugas secara manual.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Fallback: Tombol Input Manual Token QR
            OutlinedButton(
                onClick = onManualInputClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue400)
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ketik Kode QR Rumah Secara Manual", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Error Banner if camera failed
        if (scanErrorMessage != null) {
            Surface(
                color = Rose600,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = scanErrorMessage!!,
                    color = White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
