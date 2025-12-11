package com.risuncode.mybest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.risuncode.mybest.databinding.ActivityReplacementClassBinding

/**
 * Activity untuk menampilkan jadwal kuliah pengganti.
 * Kuliah pengganti adalah jadwal kuliah yang menggantikan jadwal reguler
 * karena libur atau alasan lainnya.
 */
class ReplacementClassActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReplacementClassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReplacementClassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupAppBar()
    }
    
    private fun setupAppBar() {
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
}
