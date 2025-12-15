package com.risuncode.mybest.ui.tugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.api.ParsedAssignment
import com.risuncode.mybest.data.api.SessionExpiredException
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.util.PreferenceManager
import kotlinx.coroutines.launch

class TugasListFragment : Fragment() {

    private lateinit var repository: AppRepository
    private lateinit var prefManager: PreferenceManager
    private var tugasLink: String = ""
    
    private var tugasContainer: LinearLayout? = null
    private var tvSummary: TextView? = null
    private var layoutEmptyTugas: LinearLayout? = null
    private var tvEmptyMessage: TextView? = null

    companion object {
        private const val ARG_TUGAS_LINK = "tugas_link"
        
        fun newInstance(tugasLink: String = "") = TugasListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TUGAS_LINK, tugasLink)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tugasLink = arguments?.getString(ARG_TUGAS_LINK) ?: ""
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_tugas_list, container, false)
        
        repository = AppRepository(AppDatabase.getDatabase(requireContext()))
        prefManager = PreferenceManager(requireContext())
        
        tugasContainer = rootView.findViewById(R.id.tugasContainer)
        tvSummary = rootView.findViewById(R.id.tvSummary)
        layoutEmptyTugas = rootView.findViewById(R.id.layoutEmptyTugas)
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage)
        
        loadAssignments()
        
        return rootView
    }
    
    private fun loadAssignments() {
        // Debug: Show what we're fetching
        android.util.Log.d("TugasListFragment", "Loading tugas with link: '$tugasLink'")
        
        if (tugasLink.isEmpty()) {
            showEmptyState("Tidak ada tugas (link kosong)")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getAssignments(tugasLink)
            
            result.onSuccess { assignments ->
                if (assignments.isNotEmpty()) {
                    hideEmptyState()
                    populateAssignments(assignments)
                } else {
                    showEmptyState("Tidak ada tugas untuk mata kuliah ini")
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    Toast.makeText(requireContext(), R.string.session_expired_reload, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat tugas: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                showEmptyState("Gagal memuat tugas")
            }
        }
    }
    
    private fun showEmptyState(message: String) {
        tugasContainer?.removeAllViews()
        tugasContainer?.visibility = View.GONE
        layoutEmptyTugas?.visibility = View.VISIBLE
        tvEmptyMessage?.text = message
        tvSummary?.text = ""
    }
    
    private fun hideEmptyState() {
        tugasContainer?.visibility = View.VISIBLE
        layoutEmptyTugas?.visibility = View.GONE
    }
    
    private fun populateAssignments(assignments: List<ParsedAssignment>) {
        val container = tugasContainer ?: return
        container.removeAllViews()
        
        val aktif = assignments.count { isActive(it.selesai) }
        val berakhir = assignments.count { !isActive(it.selesai) }
        tvSummary?.text = getString(R.string.tugas_summary, assignments.size, aktif, berakhir)
        
        for (assignment in assignments) {
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_tugas_card, container, false)

            cardView.findViewById<TextView>(R.id.tvPertemuan).text = 
                getString(R.string.pertemuan_n, assignment.pertemuan.toIntOrNull() ?: 0)
            cardView.findViewById<TextView>(R.id.tvTitle).text = assignment.judul
            cardView.findViewById<TextView>(R.id.tvDescription).text = assignment.deskripsi
            cardView.findViewById<TextView>(R.id.tvDeadline).text = 
                getString(R.string.deadline_format, assignment.selesai)

            val tvStatus = cardView.findViewById<TextView>(R.id.tvStatus)
            if (isActive(assignment.selesai)) {
                tvStatus.text = getString(R.string.aktif)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.status_success)
                )
            } else {
                tvStatus.text = getString(R.string.berakhir)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.text_secondary)
                )
            }

            cardView.findViewById<MaterialButton>(R.id.btnLihatTugas).setOnClickListener {
                showAssignmentDetailDialog(assignment)
            }

            container.addView(cardView)
        }
    }
    
    private fun isActive(deadline: String): Boolean {
        // Simple check - could be improved with proper date parsing
        return try {
            val now = System.currentTimeMillis()
            // For simplicity, assume active if not empty
            deadline.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAssignmentFile(filename: String): java.io.File {
        // Use app-specific Downloads directory which is persistent but doesn't require extra permissions
        val dir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        return java.io.File(dir, filename)
    }

    private fun showAssignmentDetailDialog(assignment: ParsedAssignment) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_tugas_detail, null)

        dialogView.findViewById<TextView>(R.id.tvPertemuan).text = 
            getString(R.string.pertemuan_n, assignment.pertemuan.toIntOrNull() ?: 0)
        dialogView.findViewById<TextView>(R.id.tvTitle).text = assignment.judul
        dialogView.findViewById<TextView>(R.id.tvDeadline).text = 
            getString(R.string.deadline_format, assignment.selesai)

        val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)
        if (isActive(assignment.selesai)) {
            tvStatus.text = getString(R.string.aktif)
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.status_success)
            )
        } else {
            tvStatus.text = getString(R.string.berakhir)
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.text_secondary)
            )
        }

        // Handle File Logic
        val parts = assignment.downloadLink.removePrefix("FORM:").split("|")
        val filename = if (parts.size == 3) parts[2] else "document.pdf"
        val file = getAssignmentFile(filename)
        val isFileExists = file.exists()

        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
        val btnUnduh = dialogView.findViewById<MaterialButton>(R.id.btnUnduh)
        val tvFileStatus = dialogView.findViewById<TextView>(R.id.tvFileStatus)
        val ivCheckBadge = dialogView.findViewById<android.widget.ImageView>(R.id.ivCheckBadge)

        if (assignment.downloadLink.isNotEmpty()) {
            tvFileName.text = filename
            
            if (isFileExists) {
                btnUnduh.text = "Buka File"
                btnUnduh.setIconResource(R.drawable.ic_file_pdf)
                
                tvFileStatus?.text = getString(R.string.downloaded)
                tvFileStatus?.visibility = View.VISIBLE
                ivCheckBadge?.visibility = View.VISIBLE
                
                btnUnduh.setOnClickListener {
                    dialog.dismiss()
                    openFile(file)
                }
            } else {
                btnUnduh.text = "Unduh Soal"
                btnUnduh.setIconResource(R.drawable.ic_download)
                tvFileStatus?.visibility = View.GONE
                ivCheckBadge?.visibility = View.GONE
                
                btnUnduh.setOnClickListener {
                    downloadAssignment(assignment.downloadLink, filename, dialogView, btnUnduh, tvFileStatus, ivCheckBadge)
                }
            }
        } else {
            tvFileName.text = "Tidak ada file"
            btnUnduh.visibility = View.GONE
            tvFileStatus?.visibility = View.GONE
            ivCheckBadge?.visibility = View.GONE
        }

        // Batal button
        dialogView.findViewById<MaterialButton>(R.id.btnBatal).setOnClickListener {
            dialog.dismiss()
        }

        // Kirim button
        dialogView.findViewById<MaterialButton>(R.id.btnKirim).setOnClickListener {
            if (assignment.submitLink.isNotEmpty()) {
                Toast.makeText(requireContext(), R.string.tugas_submitted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Link submit tidak tersedia", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        
        dialog.behavior.peekHeight = 800
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        
        dialog.show()
    }
    
    private fun downloadAssignment(downloadLink: String, filename: String, dialogView: View, btnUnduh: MaterialButton, tvFileStatus: TextView?, ivCheckBadge: android.widget.ImageView?) {
        val file = getAssignmentFile(filename)
        
        viewLifecycleOwner.lifecycleScope.launch {
            btnUnduh.isEnabled = false
            btnUnduh.text = "Mengunduh..."
            
            val result = repository.downloadAssignmentFile(downloadLink, file)
            
            result.onSuccess { savedFile ->
                Toast.makeText(requireContext(), "File tersimpan", Toast.LENGTH_SHORT).show()
                
                btnUnduh.isEnabled = true
                btnUnduh.text = "Buka File"
                btnUnduh.setIconResource(R.drawable.ic_file_pdf)
                btnUnduh.setOnClickListener {
                    openFile(savedFile)
                }
                
                tvFileStatus?.text = getString(R.string.downloaded)
                tvFileStatus?.visibility = View.VISIBLE
                ivCheckBadge?.visibility = View.VISIBLE
                
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Gagal mengunduh: ${error.message}", Toast.LENGTH_SHORT).show()
                btnUnduh.isEnabled = true
                btnUnduh.text = "Unduh Soal"
            }
        }
    }
    
    private fun openFile(file: java.io.File) {
        try {
            PdfViewerActivity.start(requireContext(), file.absolutePath, file.name)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

