package com.risuncode.mybest

import android.app.Application
import com.risuncode.mybest.data.api.ApiClient

class MyBestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize API Client (Cookies & User Agent) globally
        ApiClient.init(this)
    }
}
