package com.risuncode.mybest.ui.tugas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.risuncode.mybest.R
import com.risuncode.mybest.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFileName: String = ""

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_FILE_NAME = "file_name"

        fun start(context: Context, title: String, content: String, fileName: String = "") {
            val intent = Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        pdfFileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "document.pdf"

        setupUI(title, content)
        showDownloadSuccess()
    }

    private fun setupUI(title: String, content: String) {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvTitle.text = title
        binding.tvPdfContent.text = content
        
        // Open in external PDF viewer
        binding.ivOpenExternal.setOnClickListener {
            openInExternalApp()
        }
    }

    private fun openInExternalApp() {
        try {
            // In a real app, this would open the actual PDF file
            // For demo, we'll show intent chooser for PDF apps
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(null, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val chooser = Intent.createChooser(intent, getString(R.string.open_in_external))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(this, getString(R.string.no_pdf_app), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_pdf_app), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadSuccess() {
        binding.cardDownloadSuccess.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            delay(3000)
            binding.cardDownloadSuccess.visibility = View.GONE
        }
    }
}
