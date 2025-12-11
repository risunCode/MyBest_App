package com.risuncode.mybest

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.risuncode.mybest.databinding.ActivityNotificationSettingsBinding
import com.risuncode.mybest.util.PreferenceManager

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupAppBar()
        loadSavedSettings()
        setupListeners()
    }

    private fun setupAppBar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSavedSettings() {
        // Load saved settings from PreferenceManager
        binding.switchScheduleNotif.isChecked = prefManager.notifScheduleEnabled
        binding.switchGradeNotif.isChecked = prefManager.notifGradeEnabled
        binding.switchAlarm.isChecked = prefManager.alarmEnabled
        
        // Update alarm time UI based on saved minutes
        updateAlarmTimeDisplay()
        updateAlarmTimeState()
    }
    
    private fun updateAlarmTimeDisplay() {
        val minutes = prefManager.alarmMinutes
        binding.tvAlarmTime.text = "$minutes menit sebelum"
    }
    
    private fun updateAlarmTimeState() {
        val isAlarmEnabled = binding.switchAlarm.isChecked
        binding.layoutAlarmTime.alpha = if (isAlarmEnabled) 1.0f else 0.5f
        binding.tvAlarmTime.isEnabled = isAlarmEnabled
    }

    private fun setupListeners() {
        binding.switchScheduleNotif.setOnCheckedChangeListener { _, isChecked ->
            prefManager.notifScheduleEnabled = isChecked
            Toast.makeText(this, 
                if (isChecked) getString(R.string.schedule_notif_enabled) 
                else getString(R.string.schedule_notif_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchGradeNotif.setOnCheckedChangeListener { _, isChecked ->
            prefManager.notifGradeEnabled = isChecked
            Toast.makeText(this, 
                if (isChecked) getString(R.string.grade_notif_enabled) 
                else getString(R.string.grade_notif_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            prefManager.alarmEnabled = isChecked
            updateAlarmTimeState()
            Toast.makeText(this, 
                if (isChecked) getString(R.string.alarm_enabled) 
                else getString(R.string.alarm_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }
        
        binding.layoutAlarmTime.setOnClickListener {
            if (binding.switchAlarm.isChecked) {
                showAlarmTimeDialog()
            }
        }

        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestNotification.setOnClickListener {
            // TODO: Implement actual test notification
            Toast.makeText(this, getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show()
        }

        binding.btnTestAlarm.setOnClickListener {
            // TODO: Implement actual test alarm
            Toast.makeText(this, getString(R.string.test_alarm_sent), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAlarmTimeDialog() {
        val options = arrayOf("5 menit", "10 menit", "15 menit", "30 menit", "60 menit")
        val values = intArrayOf(5, 10, 15, 30, 60)
        val currentIndex = values.indexOf(prefManager.alarmMinutes).takeIf { it >= 0 } ?: 2
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Waktu Pengingat")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                prefManager.alarmMinutes = values[which]
                updateAlarmTimeDisplay()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveSettings() {
        // Settings are already saved on each toggle change
        // This button provides confirmation feedback
        Toast.makeText(this, getString(R.string.notification_settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}
