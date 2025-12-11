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

    // Dummy data for tugas
    private data class TugasItem(
        val pertemuan: Int,
        val title: String,
        val description: String,
        val deadline: String,
        val isActive: Boolean,
        val fileName: String,
        val fileContent: String,
        val downloadLink: String = "",
        val submitLink: String = ""
    )

    private val dummyTugas = listOf(
        TugasItem(1, "hasil quis pertemuan 1", "hasil quis pertemuan 1", 
            "22 Sep 2025, 11:00", false,
            "aeXWMzkj455HKXOAN7...D2K1nCYkfK0VnYiqa.pdf",
            "Hasil Quis Pertemuan 1\n\nSoal 1: Jelaskan konsep keamanan informasi\nJawaban: ..."),
        TugasItem(2, "tugas 1", "tugas 1",
            "01 Okt 2025, 23:00", false,
            "tugas_1_keamanan.pdf",
            "Tugas 1\n\nBuatlah makalah tentang keamanan jaringan..."),
        TugasItem(3, "Tugas Pertemuan 3", "Tugas Pertemuan 3",
            "06 Okt 2025, 23:59", false,
            "tugas_pertemuan_3.pdf",
            "Tugas Pertemuan 3\n\nAnalisis kasus keamanan siber...")
    )

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
        if (prefManager.isGuestMode || encryptedId.isEmpty()) {
            setupDummyUI()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getAssignments(encryptedId)
            
            result.onSuccess { assignments ->
                if (assignments.isNotEmpty()) {
                    populateAssignments(assignments)
                } else {
                    setupDummyUI()
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    Toast.makeText(requireContext(), R.string.session_expired_reload, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat tugas", Toast.LENGTH_SHORT).show()
                }
                setupDummyUI()
            }
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.downloadAssignmentFile(downloadLink)
            
            result.onSuccess { bytes ->
                Toast.makeText(requireContext(), R.string.file_downloaded, Toast.LENGTH_SHORT).show()
                dialogView.findViewById<TextView>(R.id.tvFileStatus)?.text = getString(R.string.downloaded)
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Gagal mengunduh: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDummyUI() {
        val container = tugasContainer ?: return
        
        // Update summary
        val aktif = dummyTugas.count { it.isActive }
        val berakhir = dummyTugas.count { !it.isActive }
        tvSummary?.text = getString(R.string.tugas_summary, dummyTugas.size, aktif, berakhir)

        // Generate tugas cards
        container.removeAllViews()
        for (tugas in dummyTugas) {
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_tugas_card, container, false)

            cardView.findViewById<TextView>(R.id.tvPertemuan).text = 
                getString(R.string.pertemuan_n, tugas.pertemuan)
            cardView.findViewById<TextView>(R.id.tvTitle).text = tugas.title
            cardView.findViewById<TextView>(R.id.tvDescription).text = tugas.description
            cardView.findViewById<TextView>(R.id.tvDeadline).text = 
                getString(R.string.deadline_format, tugas.deadline)

            val tvStatus = cardView.findViewById<TextView>(R.id.tvStatus)
            if (tugas.isActive) {
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
                showTugasDetailDialog(tugas)
            }

            container.addView(cardView)
        }
    }

    private fun showTugasDetailDialog(tugas: TugasItem) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_tugas_detail, null)

        dialogView.findViewById<TextView>(R.id.tvPertemuan).text = 
            getString(R.string.pertemuan_n, tugas.pertemuan)
        dialogView.findViewById<TextView>(R.id.tvTitle).text = tugas.title
        dialogView.findViewById<TextView>(R.id.tvDeadline).text = 
            getString(R.string.deadline_format, tugas.deadline)

        val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)
        if (tugas.isActive) {
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

        dialogView.findViewById<TextView>(R.id.tvFileName).text = tugas.fileName

        // Lihat button - open PDF viewer
        dialogView.findViewById<MaterialButton>(R.id.btnLihat).setOnClickListener {
            dialog.dismiss()
            PdfViewerActivity.start(requireContext(), tugas.title, tugas.fileContent)
        }

        // Unduh button
        dialogView.findViewById<MaterialButton>(R.id.btnUnduh).setOnClickListener {
            Toast.makeText(requireContext(), R.string.file_downloaded, Toast.LENGTH_SHORT).show()
            dialogView.findViewById<TextView>(R.id.tvFileStatus).text = getString(R.string.downloaded)
        }

        // Batal button
        dialogView.findViewById<MaterialButton>(R.id.btnBatal).setOnClickListener {
            dialog.dismiss()
        }

        // Kirim button
        dialogView.findViewById<MaterialButton>(R.id.btnKirim).setOnClickListener {
            Toast.makeText(requireContext(), R.string.tugas_submitted, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }
}
