package id.p2kd.kalisalak.coklit.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatusMessage(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Sensor sidik jari siap digunakan."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Perangkat ini tidak memiliki sensor sidik jari."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Sensor biometrik sedang tidak dapat diakses."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Sidik jari belum didaftarkan di setelan keamanan HP."
            else -> "Autentikasi biometrik tidak tersedia."
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Autentikasi Petugas P2KD",
        subtitle: String = "Tempelkan sidik jari pada sensor untuk masuk",
        negativeButtonText: String = "Gunakan PIN / Batal",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Sidik jari tidak dikenali. Silakan coba lagi.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
