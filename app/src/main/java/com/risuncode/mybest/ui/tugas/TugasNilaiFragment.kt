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
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.api.ParsedAssignmentGrade
import com.risuncode.mybest.data.api.SessionExpiredException
import com.risuncode.mybest.data.repository.AppRepository
import kotlinx.coroutines.launch

class TugasNilaiFragment : Fragment() {

    private lateinit var repository: AppRepository
    private var tugasLink: String = ""
    
    private var gradesContainer: LinearLayout? = null
    private var layoutEmpty: LinearLayout? = null
    private var tvEmptyMessage: TextView? = null

    companion object {
        private const val ARG_TUGAS_LINK = "tugas_link"
        
        fun newInstance(tugasLink: String = "") = TugasNilaiFragment().apply {
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
        val rootView = inflater.inflate(R.layout.fragment_tugas_nilai, container, false)
        
        repository = AppRepository(AppDatabase.getDatabase(requireContext()))
        
        gradesContainer = rootView.findViewById(R.id.gradesContainer)
        layoutEmpty = rootView.findViewById(R.id.layoutEmpty)
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage)
        
        loadGrades()
        
        return rootView
    }
    
    private fun loadGrades() {
        if (tugasLink.isEmpty()) {
            showEmptyState("Tidak ada data nilai")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getAssignmentGrades(tugasLink)
            
            result.onSuccess { grades ->
                if (grades.isNotEmpty()) {
                    hideEmptyState()
                    populateGrades(grades)
                } else {
                    showEmptyState("Belum ada nilai untuk mata kuliah ini")
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    Toast.makeText(requireContext(), R.string.session_expired_reload, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat nilai: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                showEmptyState("Gagal memuat data nilai")
            }
        }
    }
    
    private fun showEmptyState(message: String) {
        gradesContainer?.removeAllViews()
        gradesContainer?.visibility = View.GONE
        layoutEmpty?.visibility = View.VISIBLE
        tvEmptyMessage?.text = message
    }
    
    private fun hideEmptyState() {
        gradesContainer?.visibility = View.VISIBLE
        layoutEmpty?.visibility = View.GONE
    }
    
    private fun populateGrades(grades: List<ParsedAssignmentGrade>) {
        val container = gradesContainer ?: return
        container.removeAllViews()
        
        for (grade in grades) {
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_nilai_card, container, false)

            cardView.findViewById<TextView>(R.id.tvNo).text = grade.no.toString()
            cardView.findViewById<TextView>(R.id.tvJudul).text = grade.judul
            cardView.findViewById<TextView>(R.id.tvKodeMtk).text = grade.kodeMtk
            cardView.findViewById<TextView>(R.id.tvNilai).text = grade.nilai
            cardView.findViewById<TextView>(R.id.tvKomentar).text = 
                grade.komentarDosen.ifEmpty { "Tidak ada komentar" }
            
            // Set nilai badge color based on score
            val nilaiValue = grade.nilai.toIntOrNull() ?: 0
            val tvNilai = cardView.findViewById<TextView>(R.id.tvNilai)
            val colorRes = when {
                nilaiValue >= 80 -> R.color.status_success
                nilaiValue >= 60 -> R.color.status_warning
                else -> R.color.status_error
            }
            tvNilai.setTextColor(requireContext().getColor(colorRes))

            container.addView(cardView)
        }
    }
}
