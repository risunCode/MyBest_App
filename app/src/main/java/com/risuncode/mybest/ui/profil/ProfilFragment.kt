package com.risuncode.mybest.ui.profil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.FragmentProfilBinding
import com.risuncode.mybest.util.PreferenceManager
import com.risuncode.mybest.util.StringUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefManager: PreferenceManager
    private lateinit var repository: AppRepository
    
    private var isEditMode = false
    private var currentName = ""
    private var currentEmail = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PreferenceManager(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        repository = AppRepository(database)
        
        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        // NIM always from login credentials - no need to fetch
        val nim = prefManager.savedNim.ifEmpty { getString(R.string.default_nim) }
        binding.tvNim.text = StringUtils.formatNim(nim)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val user = repository.getCurrentUser().firstOrNull()
            
            if (user != null) {
                // Use data from database
                currentName = user.name
                currentEmail = user.email
                
                val initials = StringUtils.getInitials(user.name)
                
                binding.tvAvatar.text = initials
                binding.tvName.text = user.name
                binding.tvNameInfo.text = user.name
                binding.tvEmail.text = user.email
            } else {
                // Fallback to PreferenceManager
                currentName = prefManager.userName.ifEmpty { getString(R.string.guest_user) }
                currentEmail = prefManager.userEmail.ifEmpty { getString(R.string.default_email) }
                
                val initials = StringUtils.getInitials(currentName)
                
                binding.tvAvatar.text = initials
                binding.tvName.text = currentName
                binding.tvNameInfo.text = currentName
                binding.tvEmail.text = currentEmail
            }
        }
    }

    private fun setupListeners() {
        // Edit Profile button
        binding.btnEdit.setOnClickListener {
            toggleEditMode(true)
        }
        
        // Cancel Edit button
        binding.btnCancelEdit.setOnClickListener {
            toggleEditMode(false)
        }
        
        // Save Profile button
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Change Password button
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }
    
    private fun toggleEditMode(edit: Boolean) {
        isEditMode = edit
        
        // Toggle visibility
        binding.layoutInfoDisplay.visibility = if (edit) View.GONE else View.VISIBLE
        binding.layoutInfoEdit.visibility = if (edit) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (edit) View.GONE else View.VISIBLE
        
        if (edit) {
            // Pre-fill edit fields with current values
            binding.etEditName.setText(currentName)
            binding.etEditEmail.setText(currentEmail)
        }
    }
    
    private fun saveProfile() {
        val name = binding.etEditName.text?.toString()?.trim() ?: ""
        val email = binding.etEditEmail.text?.toString()?.trim() ?: ""
        
        // Validation
        if (name.isEmpty()) {
            binding.etEditName.error = getString(R.string.error_name_empty)
            return
        }
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEditEmail.error = getString(R.string.error_email_invalid)
            return
        }
        
        // Disable button during save
        binding.btnSaveProfile.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.updateProfile(name, email)
            
            result.onSuccess {
                // Update local display
                currentName = name
                currentEmail = email
                
                binding.tvName.text = name
                binding.tvNameInfo.text = name
                binding.tvEmail.text = email
                binding.tvAvatar.text = StringUtils.getInitials(name)
                
                Toast.makeText(requireContext(), getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
            
            binding.btnSaveProfile.isEnabled = true
        }
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        when {
            currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                Toast.makeText(requireContext(), getString(R.string.error_all_fields_required), Toast.LENGTH_SHORT).show()
            }
            newPassword != confirmPassword -> {
                Toast.makeText(requireContext(), getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
            }
            newPassword.length < 6 -> {
                Toast.makeText(requireContext(), getString(R.string.error_password_min_length), Toast.LENGTH_SHORT).show()
            }
            else -> {
                binding.btnChangePassword.isEnabled = false
                
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = repository.changePassword(currentPassword, newPassword)
                    
                    result.onSuccess {
                        binding.etCurrentPassword.text?.clear()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()
                        Toast.makeText(requireContext(), getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    
                    binding.btnChangePassword.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

