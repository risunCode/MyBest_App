package com.risuncode.mybest.ui.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.risuncode.mybest.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
    }

    private fun setupAppBar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}
