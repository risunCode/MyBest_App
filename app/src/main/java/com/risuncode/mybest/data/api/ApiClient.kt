package com.risuncode.mybest.data.api

import android.content.Context
import android.webkit.WebSettings
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Singleton OkHttp client untuk API calls ke BSI E-Learning
 * Toleran terhadap perubahan HTTP/HTTPS dari server BSI
 */
object ApiClient {
    
    // Base URLs - BSI kadang pakai HTTP kadang HTTPS
    const val BASE_URL_HTTPS = "https://elearning.bsi.ac.id"
    const val BASE_URL_HTTP = "http://elearning.bsi.ac.id"
    
    // Default pakai HTTPS dulu
    var BASE_URL = BASE_URL_HTTPS
        private set
    
    // Fallback User Agent (Chrome on Android)
    private const val FALLBACK_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    
    // Global User Agent - set from WebView on first run
    var userAgent: String = FALLBACK_USER_AGENT
        private set
    
    /**
     * Initialize ApiClient (Cookie Persistence & User Agent)
     * Call this once on app start (in Application class)
     */
    fun init(context: Context) {
        // Init Cookies
        cookieJar.init(context)
        
        // Init User Agent
        try {
            userAgent = WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            userAgent = FALLBACK_USER_AGENT
        }
    }
    
    // Cookie jar untuk session management
    val cookieJar = SessionCookieJar()
    
    // Logging interceptor untuk debug
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS // Kurangi verbosity
    }
    
    // Trust manager yang menerima semua sertifikat (untuk BSI yang sertifikatnya bermasalah)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    
    // SSL context dengan trust manager custom
    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, SecureRandom())
    }
    
    // OkHttp client dengan cookie jar dan SSL bypass
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Trust semua hostname
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false) // Handle redirects manually untuk detect login redirect
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * Build full URL from path
     */
    fun buildUrl(path: String): String {
        return if (path.startsWith("http")) path else "$BASE_URL$path"
    }
    
    /**
     * Switch to HTTP if HTTPS fails
     */
    fun switchToHttp() {
        BASE_URL = BASE_URL_HTTP
    }
    
    /**
     * Switch to HTTPS
     */
    fun switchToHttps() {
        BASE_URL = BASE_URL_HTTPS
    }
    
    /**
     * Clear session (for logout)
     */
    fun clearSession() {
        cookieJar.clearCookies()
    }
}
