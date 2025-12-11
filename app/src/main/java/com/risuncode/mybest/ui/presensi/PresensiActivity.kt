package com.risuncode.mybest.ui.presensi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.api.AttendanceRecord
import com.risuncode.mybest.data.api.SessionExpiredException
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.ActivityPresensiBinding
import com.risuncode.mybest.ui.login.LoginActivity
import com.risuncode.mybest.ui.tugas.TugasActivity
import com.risuncode.mybest.util.PreferenceManager
import kotlinx.coroutines.launch

class PresensiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPresensiBinding
    private lateinit var repository: AppRepository
    private lateinit var prefManager: PreferenceManager
    private var scheduleId: Int = -1
    private var schedule: ScheduleEntity? = null

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"

        fun start(context: Context, scheduleId: Int) {
            val intent = Intent(context, PresensiActivity::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPresensiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = AppRepository(database)
        prefManager = PreferenceManager(this)

        scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1)

        setupAppBar()
        setupListeners()
        loadScheduleData()
    }

    private fun setupAppBar() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnTugas.setOnClickListener {
            schedule?.let { s ->
                TugasActivity.start(this, s.id, s.subjectName, s.encryptedId)
            }
        }

        binding.btnPresensi.setOnClickListener {
            performPresensi()
        }
    }

    private fun loadScheduleData() {
        lifecycleScope.launch {
            schedule = repository.getScheduleById(scheduleId)

            schedule?.let { s ->
                // Update UI
                binding.tvSubjectName.text = s.subjectName
                binding.tvScheduleTime.text = "${s.day} - ${s.startTime}-${s.endTime}"
                binding.tvDosen.text = "Dosen: ${s.dosen}"
                binding.tvRoom.text = "Ruang: ${s.room}"
                binding.tvKode.text = "Kode: ${s.subjectCode}"
                binding.tvSks.text = "SKS: ${s.sks}"

                // Update button state
                if (s.isAttended) {
                    binding.btnPresensi.text = getString(R.string.presensi_already)
                    binding.btnPresensi.isEnabled = false
                }

                // Load attendance records from API or local
                loadAttendanceRecords(s.encryptedId)
            }
        }
    }

    private var hadirCount = 0
    private var tidakHadirCount = 0

    private fun loadAttendanceRecords(encryptedId: String) {
        if (encryptedId.isEmpty()) {
            // No encrypted ID - use dummy data
            generateDummyAttendanceList()
            return
        }
        
        lifecycleScope.launch {
            val result = repository.getAttendanceRecords(encryptedId)
            
            result.onSuccess { records ->
                if (records.isNotEmpty()) {
                    populateAttendanceList(records)
                } else {
                    generateDummyAttendanceList()
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    handleSessionExpired()
                } else {
                    Toast.makeText(
                        this@PresensiActivity,
                        "Gagal memuat data presensi",
                        Toast.LENGTH_SHORT
                    ).show()
                    generateDummyAttendanceList()
                }
            }
        }
    }
    
    private fun populateAttendanceList(records: List<AttendanceRecord>) {
        val container = binding.attendanceListContainer
        container.removeAllViews()
        
        hadirCount = records.count { it.status.contains("Hadir", ignoreCase = true) }
        tidakHadirCount = records.count { !it.status.contains("Hadir", ignoreCase = true) }
        
        for (record in records) {
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_attendance_expandable, container, false)

            val tvNumber = itemView.findViewById<TextView>(R.id.tvNumber)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            val tvPtm = itemView.findViewById<TextView>(R.id.tvPtm)
            val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpand)
            val rowMain = itemView.findViewById<LinearLayout>(R.id.rowMain)
            val expandableContent = itemView.findViewById<LinearLayout>(R.id.expandableContent)
            val tvRangkuman = itemView.findViewById<TextView>(R.id.tvRangkuman)
            val tvBeritaAcara = itemView.findViewById<TextView>(R.id.tvBeritaAcara)

            tvNumber.text = record.no.toString()
            tvDate.text = record.date
            tvPtm.text = record.pertemuan
            tvRangkuman.text = record.rangkuman.ifEmpty { "Tidak ada rangkuman" }
            tvBeritaAcara.text = record.beritaAcara.ifEmpty { "Tidak ada berita acara" }

            // Set status badge
            val isHadir = record.status.contains("Hadir", ignoreCase = true)
            if (isHadir) {
                tvStatus.text = getString(R.string.status_hadir)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_success)
                )
            } else {
                tvStatus.text = getString(R.string.status_tidak_hadir)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_error)
                )
            }

            // Toggle expand/collapse with smooth animation
            rowMain.setOnClickListener {
                toggleExpand(expandableContent, ivExpand)
            }

            container.addView(itemView)
        }
        
        updateAttendanceUI()
    }

    private fun generateDummyAttendanceList() {
        val container = binding.attendanceListContainer
        container.removeAllViews()

        data class DummyRecord(
            val ptm: Int,
            val date: String,
            val isHadir: Boolean,
            val rangkuman: String,
            val beritaAcara: String
        )

        val dummyRecords = listOf(
            DummyRecord(1, "2025-09-22", true, 
                "Pengantar Keamanan dan Penjaminan Informasi",
                "Membahas tentang Konsep dan prinsip dasar informasi"),
            DummyRecord(2, "2025-09-29", true,
                "Ancaman dan Serangan Keamanan Informasi",
                "Mempelajari berbagai jenis ancaman keamanan"),
            DummyRecord(3, "2025-10-06", true,
                "Kriptografi Dasar",
                "Pengenalan kriptografi dan algoritma enkripsi"),
            DummyRecord(4, "2025-10-13", true,
                "Keamanan Jaringan",
                "Arsitektur keamanan jaringan dan Firewall"),
            DummyRecord(5, "2025-10-20", false,
                "Manajemen Akses dan Identitas",
                "Autentikasi dan otorisasi"),
            DummyRecord(6, "2025-10-27", true,
                "Keamanan Aplikasi Web",
                "OWASP Top 10 dan SQL Injection"),
            DummyRecord(7, "2025-11-03", true,
                "Audit dan Compliance",
                "Standar keamanan ISO 27001"),
            DummyRecord(8, "2025-11-10", true,
                "Incident Response",
                "Proses penanganan insiden")
        )

        hadirCount = dummyRecords.count { it.isHadir }
        tidakHadirCount = dummyRecords.count { !it.isHadir }

        for (record in dummyRecords) {
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_attendance_expandable, container, false)

            val tvNumber = itemView.findViewById<TextView>(R.id.tvNumber)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            val tvPtm = itemView.findViewById<TextView>(R.id.tvPtm)
            val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpand)
            val rowMain = itemView.findViewById<LinearLayout>(R.id.rowMain)
            val expandableContent = itemView.findViewById<LinearLayout>(R.id.expandableContent)
            val tvRangkuman = itemView.findViewById<TextView>(R.id.tvRangkuman)
            val tvBeritaAcara = itemView.findViewById<TextView>(R.id.tvBeritaAcara)

            tvNumber.text = record.ptm.toString()
            tvDate.text = record.date
            tvPtm.text = record.ptm.toString()
            tvRangkuman.text = record.rangkuman
            tvBeritaAcara.text = record.beritaAcara

            // Set status badge
            if (record.isHadir) {
                tvStatus.text = getString(R.string.status_hadir)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_success)
                )
            } else {
                tvStatus.text = getString(R.string.status_tidak_hadir)
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_error)
                )
            }

            rowMain.setOnClickListener {
                toggleExpand(expandableContent, ivExpand)
            }

            container.addView(itemView)
        }
        
        updateAttendanceUI()
    }
    
    private fun toggleExpand(expandableContent: View, ivExpand: ImageView) {
        if (expandableContent.visibility == View.GONE) {
            expandableContent.visibility = View.VISIBLE
            expandableContent.alpha = 0f
            expandableContent.animate()
                .alpha(1f)
                .setDuration(250)
                .start()
            ivExpand.animate()
                .rotation(180f)
                .setDuration(200)
                .start()
        } else {
            expandableContent.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    expandableContent.visibility = View.GONE
                }
                .start()
            ivExpand.animate()
                .rotation(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun updateAttendanceUI() {
        binding.tvHadirCount.text = hadirCount.toString()
        binding.tvIzinCount.text = tidakHadirCount.toString()
    }

    private fun performPresensi() {
        val s = schedule ?: return
        
        if (prefManager.isGuestMode) {
            // Guest mode - simulate success
            simulatePresensiSuccess()
            return
        }
        
        if (s.encryptedId.isEmpty()) {
            Toast.makeText(this, "Data presensi tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.btnPresensi.isEnabled = false
        binding.btnPresensi.text = getString(R.string.processing)
        
        lifecycleScope.launch {
            val result = repository.submitAttendance(s.encryptedId)
            
            result.onSuccess { attendanceResult ->
                if (attendanceResult.success) {
                    repository.updateAttendance(s.id, true, System.currentTimeMillis())
                    hadirCount++
                    updateAttendanceUI()
                    updateLastResponse(200)
                    
                    showResponseDialog(
                        title = getString(R.string.presensi_success),
                        message = attendanceResult.message,
                        isSuccess = true
                    )
                    binding.btnPresensi.text = getString(R.string.presensi_already)
                } else {
                    updateLastResponse(400)
                    showResponseDialog(
                        title = getString(R.string.response_failed),
                        message = attendanceResult.message,
                        isSuccess = false
                    )
                    binding.btnPresensi.isEnabled = true
                    binding.btnPresensi.text = getString(R.string.presensi_start)
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    handleSessionExpired()
                } else {
                    updateLastResponse(500)
                    showResponseDialog(
                        title = getString(R.string.response_failed),
                        message = error.message ?: "Terjadi kesalahan",
                        isSuccess = false
                    )
                    binding.btnPresensi.isEnabled = true
                    binding.btnPresensi.text = getString(R.string.presensi_start)
                }
            }
        }
    }
    
    private fun simulatePresensiSuccess() {
        binding.btnPresensi.isEnabled = false
        binding.btnPresensi.text = getString(R.string.processing)
        
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500)
            
            schedule?.let { s ->
                repository.updateAttendance(s.id, true, System.currentTimeMillis())
                hadirCount++
                updateAttendanceUI()
                updateLastResponse(200)
                
                showResponseDialog(
                    title = getString(R.string.presensi_success),
                    message = getString(R.string.response_200_ok),
                    isSuccess = true
                )
                binding.btnPresensi.text = getString(R.string.presensi_already)
            }
        }
    }
    
    private fun handleSessionExpired() {
        updateLastResponse(419)
        
        Toast.makeText(
            this,
            getString(R.string.session_expired_reload),
            Toast.LENGTH_LONG
        ).show()
        
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            val intent = Intent(this@PresensiActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateLastResponse(code: Int) {
        val (text, color) = when (code) {
            200 -> Pair(getString(R.string.response_success), R.color.status_success)
            419 -> Pair(getString(R.string.response_expired), R.color.status_warning)
            else -> Pair(getString(R.string.response_failed), R.color.status_error)
        }
        binding.tvLastResponse.text = text
        binding.tvLastResponse.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun showResponseDialog(title: String, message: String, isSuccess: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_info)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private suspend fun AppRepository.getScheduleById(id: Int): ScheduleEntity? {
        return try {
            val database = AppDatabase.getDatabase(this@PresensiActivity)
            database.scheduleDao().getScheduleById(id)
        } catch (e: Exception) {
            null
        }
    }
}
