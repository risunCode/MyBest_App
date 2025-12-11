package com.risuncode.mybest.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.risuncode.mybest.R
import com.risuncode.mybest.data.DataInitializer
import com.risuncode.mybest.databinding.ActivityLoginBinding
import com.risuncode.mybest.ui.main.MainActivity
import com.risuncode.mybest.util.PreferenceManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefManager = PreferenceManager(this)
        
        setupViews()
        loadSavedCredentials()
        checkAutoLogin()
    }

    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordInfo()
        }

        binding.btnSkipLogin.setOnClickListener {
            startGuestMode()
        }
    }

    private fun loadSavedCredentials() {
        if (prefManager.rememberMe) {
            binding.etNim.setText(prefManager.savedNim)
            binding.etPassword.setText(prefManager.savedPassword)
            binding.cbRemember.isChecked = true
        }
    }

    private fun checkAutoLogin() {
        // Check if auto-login is enabled and credentials exist
        if (prefManager.autoLoginEnabled && 
            prefManager.savedNim.isNotEmpty() && 
            prefManager.savedPassword.isNotEmpty()) {
            
            // Show toast and start auto-login
            Toast.makeText(this, getString(R.string.auto_login_wait), Toast.LENGTH_SHORT).show()
            performAutoLogin()
        }
    }

    private fun performAutoLogin() {
        // Update button to show processing state
        showLoading(true)
        binding.btnLogin.text = getString(R.string.processing_login)
        
        // Disable all inputs during auto-login
        setInputsEnabled(false)
        
        // Simulate login delay (replace with actual API call later)
        binding.root.postDelayed({
            showLoading(false)
            prefManager.isGuestMode = false
            prefManager.isLoggedIn = true
            
            // Show success toast
            Toast.makeText(
                this, 
                getString(R.string.login_success_welcome, prefManager.savedNim), 
                Toast.LENGTH_SHORT
            ).show()
            
            navigateToMain()
        }, 2000)
    }

    private fun attemptLogin() {
        val nim = binding.etNim.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validation
        if (nim.isEmpty()) {
            binding.tilNim.error = getString(R.string.error_nim_empty)
            return
        } else {
            binding.tilNim.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_empty)
            return
        } else {
            binding.tilPassword.error = null
        }

        // Save credentials and enable auto-login if "remember me" is checked
        if (binding.cbRemember.isChecked) {
            prefManager.rememberMe = true
            prefManager.savedNim = nim
            prefManager.savedPassword = password
            prefManager.autoLoginEnabled = true
        } else {
            prefManager.rememberMe = false
            prefManager.savedNim = ""
            prefManager.savedPassword = ""
            prefManager.autoLoginEnabled = false
        }

        // TODO: Implement actual login API call
        // For now, just navigate to MainActivity
        showLoading(true)
        
        // Simulate login delay
        binding.root.postDelayed({
            showLoading(false)
            prefManager.isGuestMode = false
            prefManager.isLoggedIn = true
            
            // Show success toast
            Toast.makeText(
                this, 
                getString(R.string.login_success_welcome, nim), 
                Toast.LENGTH_SHORT
            ).show()
            
            navigateToMain()
        }, 1500)
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etNim.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.cbRemember.isEnabled = enabled
        binding.btnSkipLogin.isEnabled = enabled
        binding.tvForgotPassword.isEnabled = enabled
    }

    private fun showLoading(show: Boolean) {
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.text = if (show) getString(R.string.processing) else getString(R.string.login_button)
    }

    private fun showForgotPasswordInfo() {
        Snackbar.make(
            binding.root,
            getString(R.string.contact_admin_reset),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun startGuestMode() {
        showLoading(true)
        
        prefManager.isGuestMode = true
        DataInitializer.initializeGuestData(this)
        
        // Small delay to ensure data is initialized
        binding.root.postDelayed({
            showLoading(false)
            navigateToMain()
        }, 500)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
