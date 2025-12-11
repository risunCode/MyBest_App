package com.risuncode.mybest.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "mybest_prefs"
        
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_NIM = "saved_nim"
        private const val KEY_SAVED_PASSWORD = "saved_password"
        
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CSRF_TOKEN = "csrf_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST_MODE = "is_guest_mode"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        
        private const val KEY_NOTIF_SCHEDULE_ENABLED = "notif_schedule_enabled"
        private const val KEY_NOTIF_GRADE_ENABLED = "notif_grade_enabled"
        private const val KEY_ALARM_ENABLED = "alarm_enabled"
        private const val KEY_ALARM_MINUTES = "alarm_minutes"
        
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled"
    }
    
    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()
    
    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()
    
    var savedNim: String
        get() = prefs.getString(KEY_SAVED_NIM, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_NIM, value).apply()
    
    var savedPassword: String
        get() = prefs.getString(KEY_SAVED_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_PASSWORD, value).apply()
    
    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
    
    var csrfToken: String
        get() = prefs.getString(KEY_CSRF_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CSRF_TOKEN, value).apply()
    
    var refreshToken: String
        get() = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
    
    var tokenExpiry: Long
        get() = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        set(value) = prefs.edit().putLong(KEY_TOKEN_EXPIRY, value).apply()
    
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()
    
    var isGuestMode: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST_MODE, value).apply()
    
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()
    
    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()
    
    var notifScheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SCHEDULE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SCHEDULE_ENABLED, value).apply()
    
    var notifGradeEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_GRADE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_GRADE_ENABLED, value).apply()
    
    var alarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ALARM_ENABLED, value).apply()
    
    var alarmMinutes: Int
        get() = prefs.getInt(KEY_ALARM_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_ALARM_MINUTES, value).apply()
    
    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var autoLoginEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOGIN_ENABLED, value).apply()
    
    fun clearAuthData() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_CSRF_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }
    
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
