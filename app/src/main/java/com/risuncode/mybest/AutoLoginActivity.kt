package com.risuncode.mybest

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.risuncode.mybest.databinding.ActivityAutoLoginBinding
import com.risuncode.mybest.util.PreferenceManager

class AutoLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutoLoginBinding
    private lateinit var prefManager: PreferenceManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutoLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupAppBar()
        loadSavedData()
        setupListeners()
    }

    private fun setupAppBar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSavedData() {
        binding.switchAutoLogin.isChecked = prefManager.rememberMe
        binding.etNim.setText(prefManager.savedNim)
        
        if (prefManager.savedPassword.isNotEmpty()) {
            binding.etPassword.setText(prefManager.savedPassword)
        }
        
        updateFieldsState()
    }

    private fun setupListeners() {
        binding.switchAutoLogin.setOnCheckedChangeListener { _, _ ->
            updateFieldsState()
        }

        binding.btnSave.setOnClickListener {
            saveAutoLoginData()
        }

        binding.btnClear.setOnClickListener {
            showClearDataDialog()
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun updateFieldsState() {
        val isEnabled = binding.switchAutoLogin.isChecked
        binding.etNim.isEnabled = isEnabled
        binding.etPassword.isEnabled = isEnabled
        binding.btnSave.isEnabled = isEnabled
    }

    private fun saveAutoLoginData() {
        val nim = binding.etNim.text.toString()
        val password = binding.etPassword.text.toString()

        if (nim.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_nim_password_empty), Toast.LENGTH_SHORT).show()
            return
        }

        prefManager.rememberMe = binding.switchAutoLogin.isChecked
        prefManager.savedNim = nim
        prefManager.savedPassword = password

        Toast.makeText(this, getString(R.string.auto_login_saved), Toast.LENGTH_SHORT).show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.clear_saved_data_title))
            .setMessage(getString(R.string.clear_saved_data_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                clearSavedData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun clearSavedData() {
        prefManager.rememberMe = false
        prefManager.savedNim = ""
        prefManager.savedPassword = ""
        
        binding.switchAutoLogin.isChecked = false
        binding.etNim.setText("")
        binding.etPassword.setText("")
        
        Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        binding.ivTogglePassword.setImageResource(
            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
        
        // Keep cursor at end
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }
}
