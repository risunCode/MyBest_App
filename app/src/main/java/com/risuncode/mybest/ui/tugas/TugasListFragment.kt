package com.risuncode.mybest.ui.tugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.risuncode.mybest.R

class TugasListFragment : Fragment() {

    companion object {
        fun newInstance() = TugasListFragment()
    }

    // Dummy data for tugas
    private data class TugasItem(
        val pertemuan: Int,
        val title: String,
        val description: String,
        val deadline: String,
        val isActive: Boolean,
        val fileName: String,
        val fileContent: String
    )

    private val dummyTugas = listOf(
        TugasItem(1, "hasil quis pertemuan 1", "hasil quis pertemuan 1", 
            "22 Sep 2025, 11:00", false,
            "aeXWMzkj455HKXOAN7...D2K1nCYkfK0VnYiqa.pdf",
            "Hasil Quis Pertemuan 1\n\nSoal 1: Jelaskan konsep keamanan informasi\nJawaban: ...\n\nSoal 2: Sebutkan prinsip-prinsip keamanan\nJawaban: ..."),
        TugasItem(2, "tugas 1", "tugas 1",
            "01 Okt 2025, 23:00", false,
            "tugas_1_keamanan.pdf",
            "Tugas 1\n\nBuatlah makalah tentang keamanan jaringan..."),
        TugasItem(3, "Tugas Pertemuan 3", "Tugas Pertemuan 3",
            "06 Okt 2025, 23:59", false,
            "tugas_pertemuan_3.pdf",
            "Tugas Pertemuan 3\n\nAnalisis kasus keamanan siber..."),
        TugasItem(4, "Tugas Pertemuan 4", "Tugas Pertemuan 4",
            "19 Okt 2025, 23:59", false,
            "tugas_pertemuan_4.pdf",
            "Tugas Pertemuan 4\n\nImplementasi enkripsi sederhana..."),
        TugasItem(5, "Tugas Pertemuan 5", "Tugas Pertemuan 5",
            "26 Okt 2025, 23:59", false,
            "tugas_pertemuan_5.pdf",
            "Tugas Pertemuan 5\n\nPenerapan firewall..."),
        TugasItem(6, "Tugas Pertemuan 6", "Tugas Pertemuan 6",
            "02 Nov 2025, 23:59", false,
            "tugas_pertemuan_6.pdf",
            "Tugas Pertemuan 6\n\nAudit keamanan sistem..."),
        TugasItem(7, "Tugas Pertemuan 7", "Tugas Pertemuan 7",
            "09 Nov 2025, 23:59", false,
            "tugas_pertemuan_7.pdf",
            "Tugas Pertemuan 7\n\nIncident response plan...")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_tugas_list, container, false)
        setupUI(rootView)
        return rootView
    }

    private fun setupUI(rootView: View) {
        val tvSummary = rootView.findViewById<TextView>(R.id.tvSummary)
        val container = rootView.findViewById<LinearLayout>(R.id.tugasContainer)

        // Update summary
        val aktif = dummyTugas.count { it.isActive }
        val berakhir = dummyTugas.count { !it.isActive }
        tvSummary.text = getString(R.string.tugas_summary, dummyTugas.size, aktif, berakhir)

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
