package com.risuncode.mybest.ui.setup

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.risuncode.mybest.databinding.ActivitySetupBinding
import com.risuncode.mybest.ui.login.LoginActivity
import com.risuncode.mybest.ui.main.MainActivity
import com.risuncode.mybest.util.PreferenceManager

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefManager: PreferenceManager
    private var isAgreementExpanded = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateNotificationStatus(isGranted)
    }

    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateExactAlarmStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefManager = PreferenceManager(this)
        
        // Skip setup if already completed
        if (prefManager.isSetupCompleted) {
            // Check if has any login state or credentials -> go to Dashboard
            val hasCredentials = prefManager.savedNim.isNotEmpty() && prefManager.savedPassword.isNotEmpty()
            if (prefManager.isLoggedIn || prefManager.autoLoginEnabled || hasCredentials) {
                navigateToMain()
            } else {
                navigateToLogin()
            }
            return
        }
        
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        setupScrollListener()
        updatePermissionStatuses()
        updateContinueButtonState()
    }

    private fun setupClickListeners() {
        binding.layoutNotification.setOnClickListener {
            requestNotificationPermission()
        }

        binding.layoutBackground.setOnClickListener {
            requestExactAlarm()
        }

        binding.btnShowAll.setOnClickListener {
            toggleAgreementView()
        }

        binding.cbUserAgreement.setOnCheckedChangeListener { _, _ ->
            updateContinueButtonState()
        }

        binding.btnContinue.setOnClickListener {
            if (binding.cbUserAgreement.isChecked) {
                completeSetup()
            } else {
                Toast.makeText(
                    this,
                    getString(com.risuncode.mybest.R.string.user_agreement_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupScrollListener() {
        binding.scrollAgreement.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val scrollView = v as android.widget.ScrollView
            val child = scrollView.getChildAt(0)
            
            if (child != null) {
                val diff = (child.bottom - (scrollView.height + scrollY))
                
                if (diff <= 0 && !isAgreementExpanded) {
                    toggleAgreementView()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    updateNotificationStatus(true)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            updateNotificationStatus(true)
        }
    }

    private fun requestExactAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                exactAlarmLauncher.launch(intent)
            } else {
                updateExactAlarmStatus()
            }
        } else {
            updateExactAlarmStatus()
        }
    }

    private fun updatePermissionStatuses() {
        // Notification
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        updateNotificationStatus(hasNotificationPermission)

        // Exact Alarm
        updateExactAlarmStatus()
    }

    private fun updateNotificationStatus(granted: Boolean) {
        if (granted) {
            binding.tvNotificationStatus.text = getString(com.risuncode.mybest.R.string.permission_granted)
            binding.tvNotificationStatus.setTextColor(ContextCompat.getColor(this, com.risuncode.mybest.R.color.status_success))
            binding.ivNotificationCheck.visibility = View.VISIBLE
        } else {
            binding.tvNotificationStatus.text = getString(com.risuncode.mybest.R.string.tap_to_enable)
            binding.tvNotificationStatus.setTextColor(ContextCompat.getColor(this, com.risuncode.mybest.R.color.primary))
            binding.ivNotificationCheck.visibility = View.GONE
        }
    }

    private fun updateExactAlarmStatus() {
        val canScheduleAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canScheduleAlarms) {
            binding.tvBackgroundStatus.text = getString(com.risuncode.mybest.R.string.permission_granted)
            binding.tvBackgroundStatus.setTextColor(ContextCompat.getColor(this, com.risuncode.mybest.R.color.status_success))
            binding.ivBackgroundCheck.visibility = View.VISIBLE
        } else {
            binding.tvBackgroundStatus.text = getString(com.risuncode.mybest.R.string.tap_to_enable)
            binding.tvBackgroundStatus.setTextColor(ContextCompat.getColor(this, com.risuncode.mybest.R.color.primary))
            binding.ivBackgroundCheck.visibility = View.GONE
        }
    }

    private fun updateContinueButtonState() {
        val isAgreementChecked = binding.cbUserAgreement.isChecked
        binding.btnContinue.isEnabled = isAgreementChecked
        binding.btnContinue.alpha = if (isAgreementChecked) 1.0f else 0.5f
    }

    private fun completeSetup() {
        prefManager.isSetupCompleted = true
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toggleAgreementView() {
        isAgreementExpanded = !isAgreementExpanded
        
        val transition = AutoTransition().apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }
        TransitionManager.beginDelayedTransition(binding.root, transition)
        
        if (isAgreementExpanded) {
            binding.btnShowAll.text = "Perkecil"
            
            binding.cardPermissions.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.cardPermissions.visibility = View.GONE
                }
            
            binding.tvScrollHint.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.tvScrollHint.visibility = View.GONE
                }
            
            val params = binding.cardAgreement.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            params.topMargin = (200 + 16).dpToPx()
            params.height = 0
            binding.cardAgreement.layoutParams = params
            
            val contentParams = binding.cardAgreementContent.layoutParams as android.widget.LinearLayout.LayoutParams
            contentParams.height = 0
            contentParams.weight = 1f
            binding.cardAgreementContent.layoutParams = contentParams
        } else {
            binding.btnShowAll.text = "Perbesar"
            
            binding.cardPermissions.visibility = View.VISIBLE
            binding.cardPermissions.alpha = 0f
            binding.cardPermissions.animate()
                .alpha(1f)
                .setDuration(300)
            
            binding.tvScrollHint.visibility = View.VISIBLE
            binding.tvScrollHint.alpha = 0f
            binding.tvScrollHint.animate()
                .alpha(1f)
                .setDuration(300)
            
            val params = binding.cardAgreement.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            params.topToBottom = binding.cardPermissions.id
            params.topMargin = 16.dpToPx()
            params.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            binding.cardAgreement.layoutParams = params
            
            val contentParams = binding.cardAgreementContent.layoutParams as android.widget.LinearLayout.LayoutParams
            contentParams.height = 180.dpToPx()
            contentParams.weight = 0f
            binding.cardAgreementContent.layoutParams = contentParams
        }
    }
    
    
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}
