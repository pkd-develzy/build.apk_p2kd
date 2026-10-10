package id.p2kd.kalisalak.coklit.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import id.p2kd.kalisalak.coklit.R
import id.p2kd.kalisalak.coklit.data.api.ApiClient
import id.p2kd.kalisalak.coklit.data.models.GitHubReleaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val newVersion: String,
    val downloadUrl: String,
    val releaseNotes: List<String>,
    val isMandatory: Boolean
)

object AppUpdateManager {

    private const val CHANNEL_ID = "p2kd_app_updates"
    private const val NOTIFICATION_ID = 20261010
    private const val DEFAULT_FALLBACK_DOWNLOAD_URL =
        "https://github.com/pkd-develzy/build.apk_p2kd/releases/latest/download/PETUGAS_P2KD.apk"

    /**
     * Membandingkan 2 semver string (misal: "1.10.5" vs "1.5.2")
     * Return: -1 jika v1 < v2, 0 jika sama, 1 jika v1 > v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.replace("^[vV]".toRegex(), "").trim()
        val clean2 = v2.replace("^[vV]".toRegex(), "").trim()

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 > p2) return 1
            if (p1 < p2) return -1
        }
        return 0
    }

    /**
     * Mengambil versi terpasang di perangkat
     */
    fun getInstalledVersion(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.10.5"
        } catch (_: Exception) {
            "1.10.5"
        }
    }

    /**
     * Melakukan pengecekan update baik melalui API Backend maupun GitHub Releases
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val installedVersion = getInstalledVersion(context)

        // 1. Coba melalui backend endpoint /api/app/version
        try {
            val response = ApiClient.api.checkAppVersion(installedVersion)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val status = body.updateStatus
                val identity = body.buildIdentity

                val latestVersion = status?.latestVersion ?: identity?.latestVersion ?: ""
                val isAvailable = compareVersions(latestVersion, installedVersion) > 0

                if (isAvailable && latestVersion.isNotBlank()) {
                    val downloadUrl = status?.apkDownloadUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }
                        ?: identity?.apkDownloadUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }
                        ?: DEFAULT_FALLBACK_DOWNLOAD_URL

                    val notes = identity?.releaseNotes ?: listOf(
                        "Pembaruan versi resmi PETUGAS P2KD.",
                        "Peningkatan stabilitas dan perbaikan fitur lapangan."
                    )

                    return@withContext UpdateInfo(
                        hasUpdate = true,
                        newVersion = latestVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = notes,
                        isMandatory = status?.updateRequired ?: false
                    )
                } else {
                    // Versi aplikasi di perangkat sudah sama atau lebih baru dari server: TIDAK ADA UPDATE
                    return@withContext null
                }
            }
        } catch (_: Exception) {
            // Lanjut ke fallback GitHub Releases
        }

        // 2. Fallback: Cek langsung ke GitHub Releases API
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.github.com/repos/pkd-develzy/build.apk_p2kd/releases/latest")
                .header("User-Agent", "PetugasP2KD-Updater")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val call = okHttpClient.newCall(request).execute()
            if (call.isSuccessful) {
                val responseBody = call.body?.string()
                if (!responseBody.isNullOrBlank()) {
                    val gh = Gson().fromJson(responseBody, GitHubReleaseResponse::class.java)
                    val remoteTag = gh.tagName.replace("^[vV]".toRegex(), "").trim()

                    if (compareVersions(remoteTag, installedVersion) > 0) {
                        val apkAsset = gh.assets.firstOrNull {
                            it.name.equals("PETUGAS_P2KD.apk", ignoreCase = true) ||
                            it.name.equals("PETUGAS P2KD.apk", ignoreCase = true)
                        }

                        val downloadUrl = apkAsset?.browserDownloadUrl?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_FALLBACK_DOWNLOAD_URL

                        val notes = gh.body.lines()
                            .map { it.trim().removePrefix("*").removePrefix("-").trim() }
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .take(5)

                        return@withContext UpdateInfo(
                            hasUpdate = true,
                            newVersion = remoteTag,
                            downloadUrl = downloadUrl,
                            releaseNotes = if (notes.isNotEmpty()) notes else listOf("Pembaruan performa dan fitur resmi."),
                            isMandatory = false
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Tidak ada koneksi atau gagal fetch
        }

        null
    }

    /**
     * Memunculkan notifikasi sistem di notification tray Android
     */
    fun postSystemNotification(context: Context, updateInfo: UpdateInfo) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Buat channel untuk Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Pembaruan Aplikasi P2KD",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi ketersediaan versi baru APK PETUGAS P2KD"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Intent membuka URL download saat notifikasi diklik
            val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                downloadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Verifikasi izin notifikasi di Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Pembaruan Resmi: PETUGAS P2KD v${updateInfo.newVersion}")
                .setContentText("Versi baru tersedia! Ketuk untuk mengunduh berkas APK.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Pembaruan Versi ${updateInfo.newVersion} Resmi P2KD Kalisalak telah dirilis. Ketuk di sini untuk langsung mengunduh dan memasang berkas APK pembaruan.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.stat_sys_download,
                    "UNDUH PEMBARUAN (APK)",
                    pendingIntent
                )
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
            // Abaikan jika izin notifikasi belum diberikan
        }
    }
}
