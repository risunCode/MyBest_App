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
    private var encryptedId: String = ""
    
    private var tugasContainer: LinearLayout? = null
    private var tvSummary: TextView? = null

    companion object {
        private const val ARG_ENCRYPTED_ID = "encrypted_id"
        
        fun newInstance(encryptedId: String = "") = TugasListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ENCRYPTED_ID, encryptedId)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        encryptedId = arguments?.getString(ARG_ENCRYPTED_ID) ?: ""
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
        
        loadAssignments()
        
        return rootView
    }
    
    private fun loadAssignments() {
        if (encryptedId.isEmpty()) {
            showEmptyState("Tidak ada tugas")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getAssignments(encryptedId)
            
            result.onSuccess { assignments ->
                if (assignments.isNotEmpty()) {
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
        val container = tugasContainer ?: return
        container.removeAllViews()
        tvSummary?.text = message
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

        dialogView.findViewById<TextView>(R.id.tvFileName).text = 
            if (assignment.downloadLink.isNotEmpty()) "File tersedia" else "Tidak ada file"

        // Lihat button - open PDF viewer
        dialogView.findViewById<MaterialButton>(R.id.btnLihat).setOnClickListener {
            dialog.dismiss()
            PdfViewerActivity.start(requireContext(), assignment.judul, assignment.deskripsi)
        }

        // Unduh button
        dialogView.findViewById<MaterialButton>(R.id.btnUnduh).setOnClickListener {
            if (assignment.downloadLink.isNotEmpty()) {
                downloadAssignment(assignment.downloadLink, dialogView)
            } else {
                Toast.makeText(requireContext(), "Tidak ada file untuk diunduh", Toast.LENGTH_SHORT).show()
            }
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
        dialog.show()
    }
    
    private fun downloadAssignment(downloadLink: String, dialogView: View) {
        val parts = downloadLink.removePrefix("FORM:").split("|")
        val filename = if (parts.size == 3) parts[2] else "document.pdf"
        val file = java.io.File(requireContext().cacheDir, "assignments/$filename")
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.downloadAssignmentFile(downloadLink, file)
            
            result.onSuccess { savedFile ->
                Toast.makeText(requireContext(), "File tersimpan: ${savedFile.name}", Toast.LENGTH_SHORT).show()
                dialogView.findViewById<TextView>(R.id.tvFileStatus)?.text = getString(R.string.downloaded)
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Gagal mengunduh: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
