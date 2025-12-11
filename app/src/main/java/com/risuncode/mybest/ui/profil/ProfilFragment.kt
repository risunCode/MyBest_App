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
        viewLifecycleOwner.lifecycleScope.launch {
            val user = repository.getCurrentUser().firstOrNull()
            
            if (user != null) {
                // Use data from database
                val initials = StringUtils.getInitials(user.name)
                
                binding.tvAvatar.text = initials
                binding.tvName.text = user.name
                binding.tvNim.text = StringUtils.formatNim(user.nim)
                binding.tvNameInfo.text = user.name
                binding.tvEmail.text = user.email
            } else {
                // Fallback to PreferenceManager
                val name = prefManager.userName.ifEmpty { getString(R.string.guest_user) }
                val nim = prefManager.savedNim.ifEmpty { getString(R.string.default_nim) }
                val email = prefManager.userEmail.ifEmpty { getString(R.string.default_email) }
                
                val initials = StringUtils.getInitials(name)
                
                binding.tvAvatar.text = initials
                binding.tvName.text = name
                binding.tvNim.text = StringUtils.formatNim(nim)
                binding.tvNameInfo.text = name
                binding.tvEmail.text = email
            }
        }
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.feature_not_available), Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            changePassword()
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
