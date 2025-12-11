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
                } else if (!isScheduleToday(s.day)) {
                    binding.btnPresensi.text = "Bukan Jadwal Hari Ini"
                    binding.btnPresensi.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@PresensiActivity, R.color.status_warning)
                    )
                    // Disable button for non-today schedules
                    binding.btnPresensi.isEnabled = false
                }

                // Load attendance records from API or local
                loadAttendanceRecords(s.encryptedId)
            }
        }
    }

    private fun isScheduleToday(scheduleDay: String): Boolean {
        val today = getCurrentDayInIndonesian()
        return scheduleDay.equals(today, ignoreCase = true)
    }

    private fun getCurrentDayInIndonesian(): String {
        val calendar = java.util.Calendar.getInstance()
        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Senin"
            java.util.Calendar.TUESDAY -> "Selasa"
            java.util.Calendar.WEDNESDAY -> "Rabu"
            java.util.Calendar.THURSDAY -> "Kamis"
            java.util.Calendar.FRIDAY -> "Jumat"
            java.util.Calendar.SATURDAY -> "Sabtu"
            java.util.Calendar.SUNDAY -> "Minggu"
            else -> "Senin"
        }
    }

    private var hadirCount = 0
    private var tidakHadirCount = 0

    private fun loadAttendanceRecords(encryptedId: String) {
        if (encryptedId.isEmpty()) {
            showEmptyAttendanceState("Data tidak tersedia")
            return
        }
        
        lifecycleScope.launch {
            val result = repository.getAttendanceRecords(encryptedId)
            
            result.onSuccess { records ->
                if (records.isNotEmpty()) {
                    populateAttendanceList(records)
                } else {
                    showEmptyAttendanceState("Belum ada data presensi")
                }
            }.onFailure { error ->
                if (error is SessionExpiredException) {
                    handleSessionExpired()
                } else {
                    Toast.makeText(
                        this@PresensiActivity,
                        "Gagal memuat data presensi: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyAttendanceState("Gagal memuat data")
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
    
    private fun showEmptyAttendanceState(message: String) {
        binding.attendanceListContainer.removeAllViews()
        hadirCount = 0
        tidakHadirCount = 0
        updateAttendanceUI()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
