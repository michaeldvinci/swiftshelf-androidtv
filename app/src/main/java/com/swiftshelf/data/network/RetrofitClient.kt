package com.swiftshelf.data.network

import com.swiftshelf.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var apiToken: String? = null

    /**
     * Create an unsafe OkHttpClient that trusts all certificates.
     * WARNING: ONLY USE IN DEBUG BUILDS FOR TESTING!
     * This is needed when emulator date is wrong and can't validate cert dates.
     */
    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
    }

    fun initialize(baseUrl: String, token: String) {
        apiToken = token

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer $token")
                .method(original.method, original.body)

            val request = requestBuilder.build()
            chain.proceed(request)
        }

        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.HEADERS
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = loggingLevel
            // Debug logging - safe for unit tests
            try {
                android.util.Log.d("RetrofitClient", "Initializing with token: ${token.take(20)}...")
            } catch (e: RuntimeException) {
                // Running in unit test environment where android.util.Log is not available
                println("RetrofitClient: Initializing with token: ${token.take(20)}...")
            }
        }

        // Use unsafe SSL in DEBUG builds only (for emulator date issues)
        val clientBuilder = if (BuildConfig.DEBUG) {
            android.util.Log.w("RetrofitClient", "DEBUG: Using unsafe SSL client (trusts all certificates)")
            getUnsafeOkHttpClient()
        } else {
            OkHttpClient.Builder()
        }

        val client = clientBuilder
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create an unauthenticated API client for login requests
     */
    fun createUnauthenticatedApi(baseUrl: String): AudiobookshelfApi {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = loggingLevel
        }

        // Use unsafe SSL in DEBUG builds only (for emulator date issues)
        val clientBuilder = if (BuildConfig.DEBUG) {
            android.util.Log.w("RetrofitClient", "DEBUG: Using unsafe SSL client for login (trusts all certificates)")
            getUnsafeOkHttpClient()
        } else {
            OkHttpClient.Builder()
        }

        val client = clientBuilder
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiobookshelfApi::class.java)
    }

    fun getApi(): AudiobookshelfApi {
        return retrofit?.create(AudiobookshelfApi::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean = retrofit != null
}
