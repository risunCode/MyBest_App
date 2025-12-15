package com.risuncode.mybest.ui.tugas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.risuncode.mybest.databinding.ActivityPdfViewerBinding
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding

    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        private const val EXTRA_TITLE = "title"

        fun start(context: Context, filePath: String, title: String = "PDF Viewer") {
            val intent = Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "PDF Viewer"

        setupToolbar(title)
        loadPdf(filePath)
    }

    private fun setupToolbar(title: String) {
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadPdf(filePath: String) {
        val file = File(filePath)
        
        if (!file.exists()) {
            showError("File tidak ditemukan")
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        try {
            binding.pdfView.fromFile(file)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(false)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(0)
                .onLoad { _ ->
                    binding.progressBar.visibility = View.GONE
                }
                .onError { t ->
                    binding.progressBar.visibility = View.GONE
                    showError("Gagal memuat PDF: ${t.message}")
                }
                .onPageError { _, t ->
                    showError("Error pada halaman: ${t.message}")
                }
                .load()
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            showError("Error: ${e.message}")
        }
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
    }
}
