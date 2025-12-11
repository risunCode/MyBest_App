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
import com.risuncode.mybest.data.DataInitializer
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
        
        prefManager = PreferenceManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = AppRepository(database)
        
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
        // Clear all data properly
        lifecycleScope.launch {
            // Clear database if in guest mode
            if (prefManager.isGuestMode) {
                repository.clearAllData()
            }
            
            // Clear auth data from preferences
            prefManager.clearAuthData()
            prefManager.isGuestMode = false
            prefManager.isSetupCompleted = false
            
            // Disable auto-login temporarily (but keep saved credentials for autofill)
            prefManager.autoLoginEnabled = false
            
            // Navigate to login
            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
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
