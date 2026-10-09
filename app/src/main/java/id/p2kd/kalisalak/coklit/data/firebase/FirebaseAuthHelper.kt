package id.p2kd.kalisalak.coklit.data.firebase

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object FirebaseAuthHelper {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = currentUser != null

    /**
     * 1. Email and Password Authentication
     */
    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return auth.signInWithEmailAndPassword(email.trim(), pass).await()
    }

    suspend fun registerWithEmail(email: String, pass: String): AuthResult {
        return auth.createUserWithEmailAndPassword(email.trim(), pass).await()
    }

    /**
     * 2. Google Sign-In via ID Token
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await()
    }

    /**
     * 3. Phone Number Verification (SMS OTP)
     */
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult {
        return auth.signInWithCredential(credential).await()
    }

    suspend fun signInWithSmsCode(verificationId: String, smsCode: String): AuthResult {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        return auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        auth.signOut()
    }
}