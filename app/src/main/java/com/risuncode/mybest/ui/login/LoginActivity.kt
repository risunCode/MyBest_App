package com.risuncode.mybest.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.ActivityLoginBinding
import com.risuncode.mybest.ui.main.MainActivity
import com.risuncode.mybest.util.PreferenceManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefManager = PreferenceManager(this)
        repository = AppRepository(AppDatabase.getDatabase(this))
        
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

        // Hide skip login button - login is now required
        binding.btnSkipLogin.visibility = android.view.View.GONE
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
        
        val nim = prefManager.savedNim
        val password = prefManager.savedPassword
        
        lifecycleScope.launch {
            val result = repository.performLogin(nim, password)
            
            result.onSuccess {
                prefManager.isLoggedIn = true
                prefManager.userName = nim
                
                // Sync schedule in background
                repository.syncScheduleFromServer()
                
                Toast.makeText(
                    this@LoginActivity, 
                    getString(R.string.login_success_welcome, nim), 
                    Toast.LENGTH_SHORT
                ).show()
                
                navigateToMain()
            }.onFailure { error ->
                showLoading(false)
                setInputsEnabled(true)
                binding.btnLogin.text = getString(R.string.login_button)
                
                Snackbar.make(
                    binding.root,
                    error.message ?: "Login gagal",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
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

        // Show loading
        showLoading(true)
        setInputsEnabled(false)
        
        lifecycleScope.launch {
            val result = repository.performLogin(nim, password)
            
            result.onSuccess {
                // Save credentials if "remember me" is checked
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
                
                prefManager.isLoggedIn = true
                prefManager.userName = nim
                
                // Navigate immediately, sync in background (non-blocking)
                navigateToMain()
                
                Toast.makeText(
                    this@LoginActivity, 
                    getString(R.string.login_success_welcome, nim), 
                    Toast.LENGTH_SHORT
                ).show()
                
                // Fire-and-forget background sync
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    repository.syncScheduleFromServer()
                    repository.syncUserFromServer()
                }
            }.onFailure { error ->
                showLoading(false)
                setInputsEnabled(true)
                
                Snackbar.make(
                    binding.root,
                    error.message ?: "Login gagal",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etNim.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.cbRemember.isEnabled = enabled
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

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
