package com.risuncode.mybest.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.api.ApiClient
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.ActivityMainBinding
import com.risuncode.mybest.ui.dashboard.DashboardFragment
import com.risuncode.mybest.ui.profil.ProfilFragment
import com.risuncode.mybest.ui.jadwal.JadwalFragment
import com.risuncode.mybest.ui.notification.NotificationActivity
import com.risuncode.mybest.ui.login.LoginActivity
import com.risuncode.mybest.AboutActivity
import com.risuncode.mybest.AutoLoginActivity
import com.risuncode.mybest.NotificationSettingsActivity
import com.risuncode.mybest.ReplacementClassActivity
import com.risuncode.mybest.util.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize global User Agent from WebView (before any API calls)
        // Initialize global User Agent and Cookies
        ApiClient.init(this)
        
        prefManager = PreferenceManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = AppRepository(database)
        
        // Check if user can be logged in (any credentials available)
        val hasCredentials = prefManager.savedNim.isNotEmpty() && prefManager.savedPassword.isNotEmpty()
        if (!prefManager.isLoggedIn && !prefManager.autoLoginEnabled && !hasCredentials) {
            // No session and no credentials - go to login
            redirectToLogin()
            return
        }
        
        // Check session validity in background
        checkSessionAndRedirect()
        
        setupBackPressHandler()
        setupBottomNavigation()
        setupAppBar()
        setupDrawer()
        
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            updateAppBarTitle(R.string.nav_dashboard)
            binding.navigationView.setCheckedItem(R.id.nav_drawer_dashboard)
        }
    }
    
    /**
     * Check if session is still valid.
     * If expired, try auto-relogin. Only redirect to login if auto-relogin fails.
     */
    private fun checkSessionAndRedirect() {
        lifecycleScope.launch {
            val sessionValid = repository.checkSession()
            
            if (!sessionValid) {
                // Session expired - try auto-relogin if we have credentials
                val nim = prefManager.savedNim
                val password = prefManager.savedPassword
                
                if (nim.isNotEmpty() && password.isNotEmpty()) {
                    // Try auto-relogin silently
                    val loginResult = repository.performLogin(nim, password)
                    
                    loginResult.onSuccess {
                        // Auto-relogin succeeded - continue using app
                        prefManager.isLoggedIn = true
                        return@launch
                    }.onFailure { error ->
                        // Auto-relogin failed - show message and redirect
                        Toast.makeText(
                            this@MainActivity,
                            "Sesi berakhir: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        redirectToLogin()
                    }
                } else {
                    // No saved credentials - redirect to login
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.session_expired),
                        Toast.LENGTH_SHORT
                    ).show()
                    redirectToLogin()
                }
            }
        }
    }
    
    private fun redirectToLogin() {
        startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    private fun setupAppBar() {
        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        
        binding.ivNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }
    
    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_drawer_dashboard -> {
                    loadFragment(DashboardFragment())
                    updateAppBarTitle(R.string.nav_dashboard)
                    binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
                    binding.navigationView.setCheckedItem(R.id.nav_drawer_dashboard)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_drawer_schedule -> {
                    loadFragment(JadwalFragment())
                    updateAppBarTitle(R.string.nav_schedule)
                    binding.bottomNavigation.selectedItemId = R.id.nav_schedule
                    binding.navigationView.setCheckedItem(R.id.nav_drawer_schedule)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_replacement_class -> {
                    startActivity(Intent(this, ReplacementClassActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfilFragment())
                    updateAppBarTitle(R.string.nav_profile)
                    binding.bottomNavigation.selectedItemId = R.id.nav_profile
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_auto_login -> {
                    startActivity(Intent(this, AutoLoginActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    false
                }
                R.id.nav_notification_settings -> {
                    openNotificationSettings()
                    false
                }
                R.id.nav_refresh_data -> {
                    refreshData()
                    false
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    false
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                    false
                }
                else -> false
            }
        }
        
        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: android.view.View) {}
            override fun onDrawerClosed(drawerView: android.view.View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }
    
    private fun openNotificationSettings() {
        startActivity(Intent(this, NotificationSettingsActivity::class.java))
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    private fun refreshData() {
        Toast.makeText(this, getString(R.string.refreshing_data), Toast.LENGTH_SHORT).show()
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is DashboardFragment) {
            loadFragment(DashboardFragment())
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    private fun logout() {
        // Clear auth data immediately for instant response
        prefManager.clearAuthData()
        prefManager.isSetupCompleted = false
        prefManager.autoLoginEnabled = false
        prefManager.isLoggedIn = false
        
        // Navigate to login immediately (don't wait for network)
        startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
        
        // Clear database and API session in background (fire and forget)
        lifecycleScope.launch {
            try {
                repository.performLogout()
            } catch (e: Exception) {
                // Ignore logout errors - user already navigated away
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    updateAppBarTitle(R.string.nav_dashboard)
                    binding.navigationView.setCheckedItem(R.id.nav_drawer_dashboard)
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(JadwalFragment())
                    updateAppBarTitle(R.string.nav_schedule)
                    binding.navigationView.setCheckedItem(R.id.nav_drawer_schedule)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfilFragment())
                    updateAppBarTitle(R.string.nav_profile)
                    binding.navigationView.setCheckedItem(R.id.nav_profile)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun updateAppBarTitle(titleRes: Int) {
        binding.tvAppBarTitle.setText(titleRes)
    }
}
