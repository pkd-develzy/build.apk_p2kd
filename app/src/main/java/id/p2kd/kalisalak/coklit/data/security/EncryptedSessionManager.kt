package id.p2kd.kalisalak.coklit.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import id.p2kd.kalisalak.coklit.data.models.UserProfile

class EncryptedSessionManager(context: Context) {

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
        private const val KEY_SERVER_URL = "server_base_url"
        const val DEFAULT_SERVER_URL = "https://p2kdkalisalak.my.id"
    }

    fun saveSession(token: String, profile: UserProfile) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_PROFILE, gson.toJson(profile))
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

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank() && getUserProfile() != null
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_PROFILE)
            .apply()
    }

    fun getServerUrl(): String {
        return DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        // Locked to official production domain
    }
}
