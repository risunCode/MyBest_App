package com.risuncode.mybest.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Custom CookieJar untuk menyimpan session cookies dari BSI E-Learning
 */
class SessionCookieJar : CookieJar {
    
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    
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
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
    
    /**
     * Get XSRF token from cookies
     */
    fun getXsrfToken(): String? {
        return cookieStore.values.flatten()
            .find { it.name == "XSRF-TOKEN" }
            ?.value
    }
    
    /**
     * Get session cookie value
     */
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
    fun clearCookies() {
        cookieStore.clear()
    }
    
    /**
     * Get all cookies as string for debugging
     */
    fun getAllCookiesAsString(): String {
        return cookieStore.values.flatten()
            .joinToString("; ") { "${it.name}=${it.value}" }
    }
}
