package com.risuncode.mybest.data.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Custom CookieJar untuk menyimpan session cookies dari BSI E-Learning
 * Mendukung persistensi menggunakan SharedPreferences agar tidak login ulang tiap restart app
 */
class SessionCookieJar(context: Context? = null) : CookieJar {
    
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    init {
        if (context != null) {
            init(context)
        }
    }
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("cookie_store", Context.MODE_PRIVATE)
        loadCookies()
    }
    
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        if (cookieStore[host] == null) {
            cookieStore[host] = mutableListOf()
        }
        
        // Update or add cookies
        cookies.forEach { newCookie ->
            cookieStore[host]?.removeAll { it.name == newCookie.name }
            cookieStore[host]?.add(newCookie)
        }
        
        // Persist changes
        saveCookies()
    }
    
    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = cookieStore[url.host] ?: mutableListOf()
        
        // Remove expired cookies
        val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }
        
        if (validCookies.size != cookies.size) {
            cookieStore[url.host] = validCookies.toMutableList()
            saveCookies()
        }
        
        return validCookies
    }
    
    /**
     * Save cookies to SharedPreferences
     */
    private fun saveCookies() {
        prefs?.let {
            // Flatten map to list for simpler storage (assuming simple host matching)
            // Or store the whole map
            // Since we only really care about elearning.bsi.ac.id, simple storage is fine.
            // But to be proper, let's store the map.
             // Issue: Cookie class is not easily direct serializable by GSON sometimes due to internal fields.
             // Better to store SerializableCookie wrapper or just manual fields.
             // For simplicity/speed: Let's assume standard serialization works or use a simple list of custom data class.
             
             // Simplest approach: Store just the key cookies we care about manually?
             // No, generic is better.
             
             val serializableMap = cookieStore.mapValues { entry ->
                 entry.value.map { SerializableCookie(it) }
             }
             
             val json = gson.toJson(serializableMap)
             it.edit().putString("cookies", json).apply()
        }
    }
    
    /**
     * Load cookies from SharedPreferences
     */
    private fun loadCookies() {
        prefs?.getString("cookies", null)?.let { json ->
            try {
                val type = object : TypeToken<Map<String, List<SerializableCookie>>>() {}.type
                val serializableMap: Map<String, List<SerializableCookie>> = gson.fromJson(json, type)
                
                cookieStore.clear()
                serializableMap.forEach { (host, list) ->
                    cookieStore[host] = list.map { it.toCookie() }.toMutableList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get XSRF token from cookies
     */
    @Synchronized
    fun getXsrfToken(): String? {
        return cookieStore.values.flatten()
            .find { it.name == "XSRF-TOKEN" }
            ?.value
    }
    
    /**
     * Get session cookie value
     */
    @Synchronized
    fun getSessionCookie(): String? {
        return cookieStore.values.flatten()
            .find { it.name == "mybest_session" }
            ?.value
    }
    
    /**
     * Check if has valid session
     */
    fun hasSession(): Boolean {
        return getSessionCookie() != null
    }
    
    /**
     * Clear all cookies (for logout)
     */
    @Synchronized
    fun clearCookies() {
        cookieStore.clear()
        saveCookies()
    }
    
    /**
     * Get all cookies as string for debugging
     */
    @Synchronized
    fun getAllCookiesAsString(): String {
        return cookieStore.values.flatten()
            .joinToString("; ") { "${it.name}=${it.value}" }
    }
    
    // Wrapper class for serialization
    data class SerializableCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean
    ) {
        constructor(cookie: Cookie) : this(
            cookie.name, cookie.value, cookie.expiresAt, cookie.domain, 
            cookie.path, cookie.secure, cookie.httpOnly, cookie.hostOnly
        )
        
        fun toCookie(): Cookie {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)
            
            if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            
            return builder.build()
        }
    }
}
