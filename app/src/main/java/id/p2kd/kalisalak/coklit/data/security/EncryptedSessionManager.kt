package id.p2kd.kalisalak.coklit.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import id.p2kd.kalisalak.coklit.data.models.UserProfile
import java.security.MessageDigest

class EncryptedSessionManager(val context: Context) {

    fun getAppVersion(): String {
        return try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.10.3"
        } catch (_: Exception) {
            "1.10.3"
        }
    }


    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "p2kd_secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_jwt_token"
        private const val KEY_USER_PROFILE = "user_profile_json"
        private const val KEY_LAST_LINKED_USER = "last_linked_user_json"
        private const val KEY_PIN_HASH = "security_pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "security_biometric_enabled"
        private const val KEY_AUTO_LOCK_MINUTES = "security_auto_lock_minutes"
        private const val KEY_LAST_ACTIVE_TIME = "security_last_active_time"
        private const val KEY_TELEGRAM_LINKED = "telegram_account_linked"
        private const val KEY_TELEGRAM_USERNAME = "telegram_account_username"
        private const val KEY_DEVICE_ID = "p2kd_device_unique_id"
        private const val KEY_SERVER_URL = "server_base_url"
        const val DEFAULT_SERVER_URL = "https://p2kdkalisalak.my.id"
    }

    fun saveSession(token: String, profile: UserProfile) {
        val previousUser = getUserProfile()
        val isDifferentUser = previousUser != null && !previousUser.username.equals(profile.username, ignoreCase = true)

        val editor = sharedPreferences.edit()
        if (isDifferentUser) {
            // Pengguna berganti! Bersihkan pengaturan akun lama (PIN, Biometrik, Telegram link)
            editor.remove(KEY_PIN_HASH)
            editor.remove(KEY_BIOMETRIC_ENABLED)
            editor.remove(KEY_TELEGRAM_LINKED)
            editor.remove(KEY_TELEGRAM_USERNAME)
            try {
                id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager(context).clearCache()
            } catch (_: Exception) {}
        }

        val userJson = gson.toJson(profile)
        editor.putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_PROFILE, userJson)
            .putString(KEY_LAST_LINKED_USER, userJson)
            .putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun getToken(): String? = getAuthToken()

    fun getUserProfile(): UserProfile? {
        val json = sharedPreferences.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, UserProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getUser(): UserProfile? = getUserProfile()

    fun getLastLinkedUser(): UserProfile? {
        val json = sharedPreferences.getString(KEY_LAST_LINKED_USER, null) 
            ?: sharedPreferences.getString(KEY_USER_PROFILE, null) 
            ?: return null
        return try {
            gson.fromJson(json, UserProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank() && getUserProfile() != null
    }

    fun clearSession() {
        // Hapus 100% seluruh preferensi, kredensial, kunci PIN, dan tautan akun lama
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_PROFILE)
            .remove(KEY_LAST_LINKED_USER)
            .remove(KEY_PIN_HASH)
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_AUTO_LOCK_MINUTES)
            .remove(KEY_LAST_ACTIVE_TIME)
            .remove(KEY_TELEGRAM_LINKED)
            .remove(KEY_TELEGRAM_USERNAME)
            .apply()

        // Hapus cache data pemilih lokal agar tidak terbawa ke akun lain
        try {
            id.p2kd.kalisalak.coklit.data.local.LocalVoterCacheManager(context).clearCache()
        } catch (_: Exception) {}
    }

    fun clearAllAndSwitchAccount() {
        clearSession()
    }

    fun getServerUrl(): String {
        return DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        // Locked to official production domain
    }

    fun getOrCreateDeviceId(): String {
        var devId = sharedPreferences.getString(KEY_DEVICE_ID, null)
        if (devId.isNullOrBlank()) {
            devId = "P2KD-DEV-" + java.util.UUID.randomUUID().toString().take(12).uppercase()
            sharedPreferences.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }

    // ==========================================
    // 6-DIGIT PIN SECURITY (Sistem Keamanan Terenkripsi P2KD)
    // ==========================================

    private fun hashPin(pin: String): String {
        val salt = "P2KD_KALISALAK_2026_SALT_" + (getUserProfile()?.username ?: "OFFICER")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((salt + pin).toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun setPin(pin: String) {
        if (pin.length == 6) {
            val hash = hashPin(pin)
            sharedPreferences.edit().putString(KEY_PIN_HASH, hash).apply()
        }
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashPin(pin)
    }

    fun hasPin(): Boolean {
        return !sharedPreferences.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun removePin() {
        sharedPreferences.edit().remove(KEY_PIN_HASH).apply()
    }

    // ==========================================
    // BIOMETRIC AUTHENTICATION
    // ==========================================

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    // ==========================================
    // AUTO-LOCK & BACKGROUND LIFECYCLE
    // ==========================================

    fun getAutoLockMinutes(): Int {
        return sharedPreferences.getInt(KEY_AUTO_LOCK_MINUTES, 0) // 0 = langsung saat keluar
    }

    fun setAutoLockMinutes(minutes: Int) {
        sharedPreferences.edit().putInt(KEY_AUTO_LOCK_MINUTES, minutes).apply()
    }

    fun updateLastActiveTime() {
        sharedPreferences.edit().putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply()
    }

    fun getLastActiveTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
    }

    fun isAutoLockTriggered(): Boolean {
        if (!hasPin() && !isBiometricEnabled()) return false
        val timeoutMinutes = getAutoLockMinutes()
        val lastActive = getLastActiveTime()
        val elapsedMillis = System.currentTimeMillis() - lastActive
        val allowedMillis = timeoutMinutes * 60 * 1000L
        return elapsedMillis >= allowedMillis
    }

    // ==========================================
    // TELEGRAM RECOVERY INTEGRATION
    // ==========================================

    fun isTelegramLinked(): Boolean {
        return sharedPreferences.getBoolean(KEY_TELEGRAM_LINKED, false)
    }

    fun getTelegramUsername(): String? {
        return sharedPreferences.getString(KEY_TELEGRAM_USERNAME, null)
    }

    fun setTelegramLinked(linked: Boolean, username: String? = null) {
        sharedPreferences.edit()
            .putBoolean(KEY_TELEGRAM_LINKED, linked)
            .putString(KEY_TELEGRAM_USERNAME, username)
            .apply()
    }
}
