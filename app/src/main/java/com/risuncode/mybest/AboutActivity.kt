package com.risuncode.mybest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.risuncode.mybest.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAboutBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupAppBar()
    }
    
    private fun setupAppBar() {
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
}
