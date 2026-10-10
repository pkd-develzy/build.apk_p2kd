package id.p2kd.kalisalak.coklit.data.api

import android.content.Context
import id.p2kd.kalisalak.coklit.data.security.EncryptedSessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var internalSessionManager: EncryptedSessionManager? = null

    fun initialize(context: Context) {
        internalSessionManager = EncryptedSessionManager(context)
    }

    fun getSessionManager(context: Context): EncryptedSessionManager {
        if (internalSessionManager == null) {
            internalSessionManager = EncryptedSessionManager(context)
        }
        return internalSessionManager!!
    }

    val api: ApiService
        get() {
            checkNotNull(internalSessionManager) { "ApiClient must be initialized with context in Application class" }
            return getService(internalSessionManager!!)
        }

    fun getService(sessionManager: EncryptedSessionManager): ApiService {
        val baseUrl = sessionManager.getServerUrl().trim().let {
            if (it.endsWith("/")) it else "$it/"
        }

        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val token = sessionManager.getAuthToken()

                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("X-App-Version", "1.8.1")

                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                    requestBuilder.header("X-Authorization", "Bearer $token")
                    requestBuilder.header("X-App-Token", token)
                }

                chain.proceed(requestBuilder.build())
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}


